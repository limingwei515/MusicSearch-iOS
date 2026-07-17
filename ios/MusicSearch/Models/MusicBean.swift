import Foundation

/// 音乐数据模型（对应 Android MusicBean）
struct MusicBean: Identifiable, Hashable, Codable {
    var id: String
    var name: String
    var artist: String
    var albumIcon: String
    var playUrl: String
    var like: Bool

    init(id: String = "", name: String = "", artist: String = "",
         albumIcon: String = "", playUrl: String = "", like: Bool = false) {
        self.id = id
        self.name = name
        self.artist = artist
        self.albumIcon = albumIcon
        self.playUrl = playUrl
        self.like = like
    }
}
