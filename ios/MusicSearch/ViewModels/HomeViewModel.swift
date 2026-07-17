import Foundation
import Combine

/// 首页 ViewModel（对应 Android HomeViewModel）
@MainActor
final class HomeViewModel: ObservableObject {

    @Published var bangList: [BangBean] = []
    @Published var songList: [SongBean] = []
    @Published var mySongList: [SongBean] = []
    @Published var isLoading: Bool = false
    @Published var errorMessage: String?
    @Published var songDetail: SongBean?

    private let prefs = PreferencesManager.shared

    init() {
        loadMySongList()
    }

    func initBangConfig() {
        isLoading = true
        Task {
            do {
                self.bangList = try await HomeModel.getBangList()
            } catch {
                self.errorMessage = "获取榜单失败"
            }
            self.isLoading = false
        }
    }

    func initSongConfig() {
        isLoading = true
        Task {
            do {
                self.songList = try await HomeModel.getSongList()
            } catch {
                self.errorMessage = "获取歌单失败"
            }
            self.isLoading = false
        }
    }

    func getSongDetail(songId: String) {
        Task {
            do {
                if let bean = try await HomeModel.getMusicSongDetail(songId) {
                    self.songDetail = bean
                    self.mySongList.insert(bean, at: 0)
                    saveMySongList()
                } else {
                    self.songDetail = SongBean(songName: "获取失败", songId: "error")
                }
            } catch {
                self.songDetail = SongBean(songName: "获取失败", songId: "error")
            }
        }
    }

    // MARK: - 我的歌单（本地存储）
    func loadMySongList() {
        mySongList = prefs.getList(.mySongsList, as: SongBean.self)
    }

    func saveMySongList() {
        prefs.setList(mySongList, for: .mySongsList)
    }

    func removeMySong(at index: Int) {
        guard index < mySongList.count else { return }
        mySongList.remove(at: index)
        saveMySongList()
    }

    func addMySong(_ song: SongBean) {
        guard !mySongList.contains(where: { $0.songId == song.songId }) else { return }
        mySongList.insert(song, at: 0)
        saveMySongList()
    }
}
