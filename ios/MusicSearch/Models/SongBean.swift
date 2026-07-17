import Foundation

/// 歌单数据模型（对应 Android SongBean）
/// 封面 URL 规则：空或过短用默认图；不以 http 开头则补全酷我前缀
struct SongBean: Identifiable, Hashable, Codable {
    var id: String { songId }
    var songName: String
    var songImgUrl: String
    var songId: String
    var listencnt: String

    init(songName: String = "", songImgUrl: String = "", songId: String = "", listencnt: String = "") {
        self.songName = songName
        self.songId = songId
        self.listencnt = listencnt
        self.songImgUrl = SongBean.normalizeImgUrl(songImgUrl)
    }

    init(from decoder: Decoder) throws {
        let c = try decoder.container(keyedBy: CodingKeys.self)
        songName = try c.decodeIfPresent(String.self, forKey: .songName) ?? ""
        songId = try c.decodeIfPresent(String.self, forKey: .songId) ?? ""
        listencnt = try c.decodeIfPresent(String.self, forKey: .listencnt) ?? ""
        let raw = try c.decodeIfPresent(String.self, forKey: .songImgUrl) ?? ""
        songImgUrl = SongBean.normalizeImgUrl(raw)
    }

    static func normalizeImgUrl(_ url: String) -> String {
        if url.isEmpty || url.count < 5 {
            return "https://img1.kuwo.cn/star/albumcover/default.jpg"
        }
        if !url.hasPrefix("http") {
            return "https://img4.kuwo.cn/star/albumcover/" + url
        }
        return url
    }
}
