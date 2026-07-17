import Foundation
import Combine

/// 偏好设置管理（对应 Android PreferencesManager）
/// 基于 UserDefaults，支持基本类型与 Codable 列表
final class PreferencesManager: ObservableObject {

    static let shared = PreferencesManager()

    private let defaults: UserDefaults

    private init() {
        self.defaults = UserDefaults.standard
    }

    // MARK: - Keys
    enum Key: String {
        case playQuality = "play_quality"
        case downloadPath = "download_path"
        case lyricRetryCount = "lyric_retry_count"
        case cellularAutoLower = "cellular_auto_lower"
        case cellularPlayQuality = "cellular_play_quality"
        case lastPlayedMusicId = "last_played_music_id"
        case lastPlayedName = "last_played_name"
        case lastPlayedArtist = "last_played_artist"
        case lastPlayedAlbum = "last_played_album"
        case lastPlayedPosition = "last_played_position"
        case lastPlayedDuration = "last_played_duration"
        case playlistCache = "playlist_cache"
        case playingMode = "playing_mode"
        case mySongsList = "MySongsList"
    }

    // MARK: - String
    func getString(_ key: Key, default value: String = "") -> String {
        defaults.string(forKey: key.rawValue) ?? value
    }
    func setString(_ value: String, for key: Key) {
        defaults.set(value, forKey: key.rawValue)
    }

    // MARK: - Int
    func getInt(_ key: Key, default value: Int = 0) -> Int {
        if defaults.object(forKey: key.rawValue) == nil { return value }
        return defaults.integer(forKey: key.rawValue)
    }
    func setInt(_ value: Int, for key: Key) {
        defaults.set(value, forKey: key.rawValue)
    }

    // MARK: - Bool
    func getBool(_ key: Key, default value: Bool = false) -> Bool {
        if defaults.object(forKey: key.rawValue) == nil { return value }
        return defaults.bool(forKey: key.rawValue)
    }
    func setBool(_ value: Bool, for key: Key) {
        defaults.set(value, forKey: key.rawValue)
    }

    // MARK: - Codable List
    func getList<T: Decodable>(_ key: Key, as type: T.Type) -> [T] {
        guard let data = defaults.data(forKey: key.rawValue) else { return [] }
        return (try? JSONDecoder().decode([T].self, from: data)) ?? []
    }

    func setList<T: Encodable>(_ list: [T], for key: Key) {
        if let data = try? JSONEncoder().encode(list) {
            defaults.set(data, forKey: key.rawValue)
        }
    }

    // MARK: - Codable Object
    func getObject<T: Decodable>(_ key: Key, as type: T.Type) -> T? {
        guard let data = defaults.data(forKey: key.rawValue) else { return nil }
        return try? JSONDecoder().decode(T.self, from: data)
    }

    func setObject<T: Encodable>(_ value: T, for key: Key) {
        if let data = try? JSONEncoder().encode(value) {
            defaults.set(data, forKey: key.rawValue)
        }
    }

    func contains(_ key: Key) -> Bool {
        defaults.object(forKey: key.rawValue) != nil
    }

    func removeAll() {
        defaults.dictionaryRepresentation().keys.forEach { defaults.removeObject(forKey: $0) }
    }
}
