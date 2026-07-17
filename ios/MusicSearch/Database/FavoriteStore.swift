import Foundation
import Combine

/// 收藏存储（对应 Android FavoriteRepository + FavoriteDao）
/// 基于 JSON 文件持久化，支持 Combine 发布
final class FavoriteStore: ObservableObject {

    static let shared = FavoriteStore()

    @Published private(set) var favorites: [MusicBean] = []
    @Published private(set) var likedIds: Set<String> = []

    private let fileURL: URL

    private init() {
        let dir = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask)[0]
        self.fileURL = dir.appendingPathComponent("favorites.json")
        load()
    }

    private func load() {
        guard let data = try? Data(contentsOf: fileURL),
              let list = try? JSONDecoder().decode([MusicBean].self, from: data) else { return }
        favorites = list
        likedIds = Set(list.map { $0.id })
    }

    private func save() {
        if let data = try? JSONEncoder().encode(favorites) {
            try? data.write(to: fileURL, options: .atomic)
        }
    }

    func isFavorite(_ musicId: String) -> Bool {
        likedIds.contains(musicId)
    }

    func toggle(_ bean: MusicBean) {
        if likedIds.contains(bean.id) {
            favorites.removeAll { $0.id == bean.id }
            likedIds.remove(bean.id)
        } else {
            var b = bean
            b.like = true
            favorites.insert(b, at: 0)
            likedIds.insert(bean.id)
        }
        save()
    }

    func add(_ bean: MusicBean) {
        guard !likedIds.contains(bean.id) else { return }
        var b = bean
        b.like = true
        favorites.insert(b, at: 0)
        likedIds.insert(bean.id)
        save()
    }

    func remove(_ musicId: String) {
        favorites.removeAll { $0.id == musicId }
        likedIds.remove(musicId)
        save()
    }

    func getAll() -> [MusicBean] { favorites }

    func refresh() { load() }
}
