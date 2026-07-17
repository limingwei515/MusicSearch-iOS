import Foundation
import Combine

/// 歌曲列表页 ViewModel（对应 Android MusicViewModel）
/// 支持 4 种类型：search / like / bang / playlist
@MainActor
final class MusicViewModel: ObservableObject {

    @Published var musicList: [MusicBean] = []
    @Published var isLoading: Bool = false
    @Published var hasMoreData: Bool = true
    @Published var loadSuccess: Bool = true
    @Published var musicTonalList: [(bitrate: String, size: String)] = []
    @Published var musicDownloadUrl: String?

    enum ListType: String { case search, like, bang, playlist }

    private var page: Int = 0
    private let pageSize = "100"
    private var currentType: ListType = .search
    private var currentId: String = ""
    private var currentName: String = ""
    private let favoriteStore = FavoriteStore.shared
    private var isGettingDetail = false

    func configure(type: ListType, id: String, name: String) {
        self.currentType = type
        self.currentId = id
        self.currentName = name
        self.page = 0
    }

    func refresh() {
        page = 0
        loadData(isRefresh: true)
    }

    func loadMore() {
        guard hasMoreData else { return }
        page += 1
        loadData(isRefresh: false)
    }

    private func loadData(isRefresh: Bool) {
        isLoading = true
        Task {
            do {
                var result: [MusicBean] = []
                switch currentType {
                case .bang:
                    result = try await MusicModel.getBangMusic(id: currentId, page: page, size: pageSize)
                case .playlist:
                    result = try await MusicModel.getPlayListMusic(id: currentId, page: page, size: pageSize)
                case .search:
                    result = try await MusicModel.getSearchMusic(name: currentName, page: page, size: pageSize)
                case .like:
                    result = favoriteStore.getAll()
                    hasMoreData = false
                }

                if isRefresh {
                    musicList = result
                } else {
                    musicList.append(contentsOf: result)
                }
                if result.isEmpty {
                    hasMoreData = false
                }
                loadSuccess = true
            } catch {
                if isRefresh { loadSuccess = false }
            }
            isLoading = false
        }
    }

    // MARK: - 收藏状态
    func isFavorite(_ id: String) -> Bool {
        favoriteStore.isFavorite(id)
    }

    func toggleLike(_ bean: MusicBean) {
        favoriteStore.toggle(bean)
        // 刷新当前列表中的 like 状态
        if let idx = musicList.firstIndex(where: { $0.id == bean.id }) {
            musicList[idx].like = favoriteStore.isFavorite(bean.id)
        }
    }

    func refreshLikedState() {
        for i in musicList.indices {
            musicList[i].like = favoriteStore.isFavorite(musicList[i].id)
        }
    }

    // MARK: - 音质与下载
    func getMusicDetail(for music: MusicBean) {
        guard !isGettingDetail else { return }
        isGettingDetail = true
        Task {
            do {
                musicTonalList = try await MusicModel.getMusicDetail(musicId: music.id)
            } catch {
                musicTonalList = []
            }
            isGettingDetail = false
        }
    }

    func getMusicDownloadUrl(music: MusicBean, bitrate: String) {
        Task {
            do {
                musicDownloadUrl = try await MusicModel.getMusicDownloadUrl(musicId: music.id, bitrate: bitrate)
            } catch {
                musicDownloadUrl = nil
            }
        }
    }

    // MARK: - 音质名称
    static func tonalName(_ bitrate: String) -> String {
        switch bitrate {
        case "2000": return "无损 Flac"
        case "320": return "高品 MP3"
        case "128": return "标准 MP3"
        case "48": return "流畅 AAC"
        default: return bitrate
        }
    }

    static func tonalExt(_ bitrate: String) -> String {
        switch bitrate {
        case "2000": return "flac"
        case "320", "128": return "mp3"
        case "48": return "aac"
        default: return "mp3"
        }
    }
}
