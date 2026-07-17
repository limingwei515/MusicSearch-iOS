import Foundation
import Combine

/// 音乐播放器 ViewModel（对应 Android MusicPlayerViewModel）
/// 统一管理播放队列、歌词、收藏、音质
@MainActor
final class MusicPlayerViewModel: ObservableObject {

    @Published var player = MusicPlayerManager.shared
    @Published var isLoadingLyric: Bool = false
    @Published var isLiked: Bool = false
    @Published var musicTonalList: [(bitrate: String, size: String)] = []
    @Published var musicDownloadUrl: String?
    @Published var errorMessage: String?

    private let favoriteStore = FavoriteStore.shared
    private let lyricStore = LyricCacheStore.shared
    private let prefs = PreferencesManager.shared
    private var isGettingDetail = false

    init() {
        // 设置播放器回调
        player.onSkipToNext = { [weak self] in
            self?.playNext()
        }
        player.onSkipToPrevious = { [weak self] in
            self?.playPrevious()
        }
        player.onPlayerError = { [weak self] msg in
            self?.tryLowerQuality(msg)
        }
        // 恢复收藏状态
        if let m = player.currentMusic {
            checkIfLiked(m.id)
        }
    }

    // MARK: - 音质映射
    func qualityToBitrate(_ quality: String) -> String {
        switch quality {
        case "lossless": return "2000"
        case "high": return "320"
        case "standard": return "128"
        case "fluent": return "48"
        default: return "128"
        }
    }

    private func currentBitrate() -> String {
        if NetworkMonitor.shared.isCellular && prefs.getBool(.cellularAutoLower, default: true) {
            return qualityToBitrate(prefs.getString(.cellularPlayQuality, default: "standard"))
        }
        return qualityToBitrate(prefs.getString(.playQuality, default: "lossless"))
    }

    // MARK: - 播放
    func playMusic(id: String, position: Int, musicList: [MusicBean]) {
        guard position < musicList.count else { return }
        let bean = musicList[position]
        let bitrate = currentBitrate()

        Task {
            do {
                let resp = try await MusicModel.getMusicPlayInfo(musicId: id, bitrate: bitrate)
                guard let url = MusicModel.extractPlayUrl(resp) else {
                    self.errorMessage = "获取播放地址失败"
                    return
                }
                var b = bean
                b.playUrl = url
                self.player.play(url: url, music: b, queue: musicList, index: position)
                self.initMusicLyric(id: id)
                self.checkIfLiked(id)
            } catch {
                self.errorMessage = "播放失败: \(error.localizedDescription)"
                self.tryLowerQuality(nil)
            }
        }
    }

    func toggleMusic() {
        if !player.hasPlayedOnce, let m = player.currentMusic, !m.playUrl.isEmpty {
            player.play(url: m.playUrl, music: m, queue: player.playbackQueue, index: player.currentIndex)
        } else {
            player.togglePlayPause()
        }
    }

    func playNext() {
        let next = nextIndex()
        guard next < player.playbackQueue.count else { return }
        let bean = player.playbackQueue[next]
        playMusic(id: bean.id, position: next, musicList: player.playbackQueue)
    }

    func playPrevious() {
        // 进度超过 3 秒回到开头
        if player.progress > 3 {
            player.seekTo(0)
            return
        }
        let prev = previousIndex()
        guard prev < player.playbackQueue.count else { return }
        let bean = player.playbackQueue[prev]
        playMusic(id: bean.id, position: prev, musicList: player.playbackQueue)
    }

    private func nextIndex() -> Int {
        let count = player.playbackQueue.count
        guard count > 0 else { return 0 }
        switch player.playingMode {
        case .xunhuan: return player.currentIndex
        case .suiji:   return Int.random(in: 0..<count)
        case .shunxu:  return (player.currentIndex + 1) % count
        }
    }

    private func previousIndex() -> Int {
        let count = player.playbackQueue.count
        guard count > 0 else { return 0 }
        switch player.playingMode {
        case .xunhuan: return player.currentIndex
        case .suiji:   return Int.random(in: 0..<count)
        case .shunxu:  return (player.currentIndex - 1 + count) % count
        }
    }

    func seekTo(_ seconds: Double) {
        player.seekTo(seconds)
    }

    func togglePlayStatus(_ mode: PlayingMode) {
        player.playingMode = mode
        player.saveState()
    }

    // MARK: - 音质降级
    private func tryLowerQuality(_ msg: String?) {
        let current = currentBitrate()
        let next: String
        switch current {
        case "2000": next = "320"
        case "320": next = "128"
        case "128": next = "48"
        default: return
        }
        guard let m = player.currentMusic else { return }
        Task {
            do {
                let resp = try await MusicModel.getMusicPlayInfo(musicId: m.id, bitrate: next)
                if let url = MusicModel.extractPlayUrl(resp) {
                    var b = m
                    b.playUrl = url
                    self.player.play(url: url, music: b, queue: self.player.playbackQueue, index: self.player.currentIndex)
                }
            } catch { }
        }
    }

    // MARK: - 歌词
    func initMusicLyric(id: String) {
        isLoadingLyric = true
        // 先查缓存
        if let cached = lyricStore.getLyric(id) {
            parseAndPostLyric(cached)
            isLoadingLyric = false
            return
        }
        // 走网络
        Task {
            do {
                let raw = try await MusicModel.getMusicLyric(musicId: id)
                self.lyricStore.saveLyric(id, raw)
                self.parseAndPostLyric(raw)
            } catch {
                self.player.lyricList = []
            }
            self.isLoadingLyric = false
        }
    }

    private func parseAndPostLyric(_ data: String) {
        // 解析酷我歌词格式: { lrclist: [ { time, lineLyric } ] }
        guard let d = data.data(using: .utf8),
              let root = try? JSONSerialization.jsonObject(with: d) as? [String: Any],
              let arr = root["lrclist"] as? [[String: Any]] else {
            player.lyricList = []
            return
        }
        let entries = arr.compactMap { item -> LyricEntry? in
            let time = (item["time"] as? Double) ?? Double(item["time"] as? String ?? "") ?? 0
            let text = item["lineLyric"] as? String ?? ""
            return text.isEmpty ? nil : LyricEntry(time: time, text: text)
        }
        player.lyricList = entries
    }

    // MARK: - 收藏
    func checkIfLiked(_ musicId: String) {
        isLiked = favoriteStore.isFavorite(musicId)
    }

    func toggleLike() {
        guard let m = player.currentMusic else { return }
        favoriteStore.toggle(m)
        isLiked = favoriteStore.isFavorite(m.id)
    }

    // MARK: - 音质详情与下载
    func getMusicDetail(for music: MusicBean) {
        guard !isGettingDetail else { return }
        isGettingDetail = true
        Task {
            do {
                let list = try await MusicModel.getMusicDetail(musicId: music.id)
                self.musicTonalList = list
            } catch {
                self.musicTonalList = []
            }
            self.isGettingDetail = false
        }
    }

    func getMusicDownloadUrl(music: MusicBean, bitrate: String) {
        Task {
            do {
                if let url = try await MusicModel.getMusicDownloadUrl(musicId: music.id, bitrate: bitrate) {
                    self.musicDownloadUrl = url
                }
            } catch {
                self.musicDownloadUrl = nil
            }
        }
    }
}
