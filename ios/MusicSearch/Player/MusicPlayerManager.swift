import Foundation
import AVFoundation
import MediaPlayer
import Combine
import UIKit

/// 音乐播放管理器（对应 Android MusicLibraryService）
/// 基于 AVPlayer，支持后台播放、锁屏控制、NowPlaying 信息
final class MusicPlayerManager: NSObject, ObservableObject {

    static let shared = MusicPlayerManager()

    // MARK: - Published 状态
    @Published var currentMusic: MusicBean?
    @Published var isPlaying: Bool = false
    @Published var progress: Double = 0        // 当前秒
    @Published var duration: Double = 0         // 总秒
    @Published var playbackQueue: [MusicBean] = []
    @Published var currentIndex: Int = 0
    @Published var playingMode: PlayingMode = .shunxu
    @Published var lyricList: [LyricEntry] = []
    @Published var isLyricLoading: Bool = false
    @Published var isLiked: Bool = false
    @Published var hasPlayedOnce: Bool = false

    // MARK: - 私有
    private var player: AVPlayer?
    private var timeObserver: Any?
    private var statusObserver: NSKeyValueObservation?
    private var rateObserver: NSKeyValueObservation?
    private var itemEndObserver: NSObjectProtocol?
    private var audioSessionConfigured = false

    // MARK: - 回调
    var onPlaybackCompleted: (() -> Void)?
    var onSkipToNext: (() -> Void)?
    var onSkipToPrevious: (() -> Void)?
    var onPlayerError: ((String) -> Void)?

    private override init() {
        super.init()
        restoreState()
    }

    // MARK: - 音频会话
    func configureAudioSession() {
        guard !audioSessionConfigured else { return }
        do {
            let session = AVAudioSession.sharedInstance()
            try session.setCategory(.playback, mode: .default,
                                    options: [.allowAirPlay, .allowBluetooth])
            try session.setActive(true)
            audioSessionConfigured = true
        } catch {
            print("音频会话配置失败: \(error)")
        }
    }

    // MARK: - 播放
    func play(url: String, music: MusicBean, queue: [MusicBean], index: Int) {
        configureAudioSession()

        // 更新队列
        if !queue.isEmpty {
            playbackQueue = queue
            currentIndex = min(index, queue.count - 1)
        }
        currentMusic = music

        // 停止旧 player
        player?.pause()
        cleanObservers()

        guard let playURL = URL(string: url) else {
            onPlayerError?("播放地址无效")
            return
        }

        let item = AVPlayerItem(url: playURL)
        let p = AVPlayer(playerItem: item)
        p.actionAtItemEnd = .none
        player = p

        // 监听状态
        statusObserver = item.observe(\.status, options: [.new]) { [weak self] item, _ in
            DispatchQueue.main.async {
                if item.status == .readyToPlay {
                    self?.duration = CMTimeGetSeconds(item.duration)
                    p.play()
                    self?.isPlaying = true
                    self?.hasPlayedOnce = true
                    self?.setupNowPlaying(music: music)
                    self?.saveState()
                } else if item.status == .failed {
                    self?.onPlayerError?("播放失败: \(item.error?.localizedDescription ?? "")")
                }
            }
        }

        rateObserver = p.observe(\.rate, options: [.new]) { [weak self] player, _ in
            DispatchQueue.main.async {
                self?.isPlaying = player.rate > 0
                self?.updateNowPlayingPlaybackState()
            }
        }

        itemEndObserver = NotificationCenter.default.addObserver(
            forName: .AVPlayerItemDidPlayToEndTime,
            object: item, queue: .main) { [weak self] _ in
            self?.handlePlaybackEnded()
        }

        // 进度观察
        let interval = CMTime(seconds: 0.5, preferredTimescale: 600)
        timeObserver = p.addPeriodicTimeObserver(forInterval: interval, queue: .main) { [weak self] time in
            guard let self else { return }
            self.progress = CMTimeGetSeconds(time)
            self.updateNowPlayingProgress()
        }

        p.play()
        isPlaying = true
        hasPlayedOnce = true
        setupRemoteCommands()
    }

    func pause() {
        player?.pause()
        isPlaying = false
        updateNowPlayingPlaybackState()
    }

    func resume() {
        configureAudioSession()
        player?.play()
        isPlaying = true
        updateNowPlayingPlaybackState()
    }

    func togglePlayPause() {
        if !hasPlayedOnce, let m = currentMusic {
            // 重新播放
            if let url = m.playUrl.isEmpty ? nil : m.playUrl {
                play(url: url, music: m, queue: playbackQueue, index: currentIndex)
            }
        } else if isPlaying {
            pause()
        } else {
            resume()
        }
    }

    func stop() {
        player?.pause()
        player = nil
        isPlaying = false
        progress = 0
    }

    func seekTo(_ seconds: Double) {
        let target = CMTime(seconds: seconds, preferredTimescale: 600)
        player?.seek(to: target)
        progress = seconds
        updateNowPlayingProgress()
    }

    func getCurrentPosition() -> Double { progress }
    func getDuration() -> Double { duration }

    // MARK: - 播放结束处理
    private func handlePlaybackEnded() {
        switch playingMode {
        case .xunhuan:
            // 单曲循环：重播当前
            seekTo(0)
            resume()
        case .suiji:
            onSkipToNext?()
        case .shunxu:
            onSkipToNext?()
        }
    }

    // MARK: - 清理观察者
    private func cleanObservers() {
        if let to = timeObserver { player?.removeTimeObserver(to) }
        timeObserver = nil
        statusObserver?.invalidate()
        statusObserver = nil
        rateObserver?.invalidate()
        rateObserver = nil
        if let eo = itemEndObserver {
            NotificationCenter.default.removeObserver(eo)
            itemEndObserver = nil
        }
    }

    // MARK: - NowPlaying 信息
    private func setupNowPlaying(music: MusicBean) {
        var info: [String: Any] = [:]
        info[MPMediaItemPropertyTitle] = music.name
        info[MPMediaItemPropertyArtist] = music.artist
        info[MPMediaItemPropertyPlaybackDuration] = duration
        info[MPNowPlayingInfoPropertyElapsedPlaybackTime] = progress
        info[MPNowPlayingInfoPropertyPlaybackRate] = isPlaying ? 1.0 : 0.0

        // 异步加载封面
        if !music.albumIcon.isEmpty, let url = URL(string: music.albumIcon) {
            DispatchQueue.global().async {
                if let data = try? Data(contentsOf: url), let img = UIImage(data: data) {
                    let artwork = MPMediaItemArtwork(boundsSize: img.size) { _ in img }
                    info[MPMediaItemPropertyArtwork] = artwork
                    DispatchQueue.main.async {
                        MPNowPlayingInfoCenter.default().nowPlayingInfo = info
                    }
                }
            }
        }
        MPNowPlayingInfoCenter.default().nowPlayingInfo = info
    }

    private func updateNowPlayingProgress() {
        var info = MPNowPlayingInfoCenter.default().nowPlayingInfo ?? [:]
        info[MPNowPlayingInfoPropertyElapsedPlaybackTime] = progress
        info[MPNowPlayingInfoPropertyPlaybackRate] = isPlaying ? 1.0 : 0.0
        MPNowPlayingInfoCenter.default().nowPlayingInfo = info
    }

    private func updateNowPlayingPlaybackState() {
        var info = MPNowPlayingInfoCenter.default().nowPlayingInfo ?? [:]
        info[MPNowPlayingInfoPropertyPlaybackRate] = isPlaying ? 1.0 : 0.0
        info[MPNowPlayingInfoPropertyElapsedPlaybackTime] = progress
        MPNowPlayingInfoCenter.default().nowPlayingInfo = info
    }

    // MARK: - 远程控制
    private func setupRemoteCommands() {
        let cc = MPRemoteCommandCenter.shared()

        cc.playCommand.addTarget { [weak self] _ in
            self?.resume()
            return .success
        }
        cc.pauseCommand.addTarget { [weak self] _ in
            self?.pause()
            return .success
        }
        cc.togglePlayPauseCommand.addTarget { [weak self] _ in
            self?.togglePlayPause()
            return .success
        }
        cc.nextTrackCommand.addTarget { [weak self] _ in
            self?.onSkipToNext?()
            return .success
        }
        cc.previousTrackCommand.addTarget { [weak self] _ in
            self?.onSkipToPrevious?()
            return .success
        }
        cc.changePlaybackPositionCommand.addTarget { [weak self] event in
            if let e = event as? MPChangePlaybackPositionCommandEvent {
                self?.seekTo(e.positionTime)
                return .success
            }
            return .commandFailed
        }
    }

    // MARK: - 持久化
    func saveState() {
        let prefs = PreferencesManager.shared
        if let m = currentMusic {
            prefs.setString(m.id, for: .lastPlayedMusicId)
            prefs.setString(m.name, for: .lastPlayedName)
            prefs.setString(m.artist, for: .lastPlayedArtist)
            prefs.setString(m.albumIcon, for: .lastPlayedAlbum)
        }
        prefs.setInt(Int(progress), for: .lastPlayedPosition)
        prefs.setInt(Int(duration), for: .lastPlayedDuration)
        prefs.setList(playbackQueue, for: .playlistCache)
        prefs.setString(playingMode.rawValue, for: .playingMode)
    }

    private func restoreState() {
        let prefs = PreferencesManager.shared
        let id = prefs.getString(.lastPlayedMusicId)
        if !id.isEmpty {
            let name = prefs.getString(.lastPlayedName)
            let artist = prefs.getString(.lastPlayedArtist)
            let album = prefs.getString(.lastPlayedAlbum)
            currentMusic = MusicBean(id: id, name: name, artist: artist, albumIcon: album)
        }
        let queue = prefs.getList(.playlistCache, as: MusicBean.self)
        if !queue.isEmpty { playbackQueue = queue }
        if let mode = PlayingMode(rawValue: prefs.getString(.playingMode)) {
            playingMode = mode
        }
    }
}
