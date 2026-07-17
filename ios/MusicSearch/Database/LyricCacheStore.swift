import Foundation
import Combine

/// 歌词缓存存储（对应 Android LyricCacheRepository + LyricCacheDao）
/// 基于 JSON 字典文件持久化 [musicId: lyricData]
final class LyricCacheStore {

    static let shared = LyricCacheStore()

    private var cache: [String: String] = [:]
    private let fileURL: URL
    private let queue = DispatchQueue(label: "com.linfeng.music.lyriccache")

    private init() {
        let dir = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask)[0]
        self.fileURL = dir.appendingPathComponent("lyric_cache.json")
        load()
    }

    private func load() {
        queue.sync {
            guard let data = try? Data(contentsOf: fileURL),
                  let dict = try? JSONDecoder().decode([String: String].self, from: data) else { return }
            cache = dict
        }
    }

    private func save() {
        if let data = try? JSONEncoder().encode(cache) {
            try? data.write(to: fileURL, options: .atomic)
        }
    }

    func getLyric(_ musicId: String) -> String? {
        queue.sync { cache[musicId] }
    }

    func hasLyric(_ musicId: String) -> Bool {
        queue.sync { cache[musicId] != nil }
    }

    func saveLyric(_ musicId: String, _ lyricData: String) {
        queue.sync(flags: .barrier) {
            cache[musicId] = lyricData
            self.save()
        }
    }

    func deleteLyric(_ musicId: String) {
        queue.sync(flags: .barrier) {
            cache.removeValue(forKey: musicId)
            self.save()
        }
    }

    func clearAll() {
        queue.sync(flags: .barrier) {
            cache.removeAll()
            self.save()
        }
    }

    var count: Int { queue.sync { cache.count } }
}
