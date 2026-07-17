import Foundation

/// 首页相关 API（对应 Android HomeModel）
enum HomeModel {

    private static let bangApi = "http://wapi.kuwo.cn/api/pc/bang/list"
    private static let songApi = "http://wapi.kuwo.cn/api/pc/classify/playlist/getRcmPlayList"
    private static let musicSongUrl = "https://mobilebasedata.kuwo.cn/basedata.s"

    /// 获取榜单列表
    static func getBangList() async throws -> [BangBean] {
        let resp = try await HttpRequest.request(url: bangApi, method: .GET)
        return parseBangList(resp)
    }

    /// 解析榜单列表 JSON
    static func parseBangList(_ json: String) -> [BangBean] {
        guard let data = json.data(using: .utf8),
              let root = try? JSONSerialization.jsonObject(with: data) as? [String: Any] else { return [] }
        // 酷我榜单结构: { child: [ { child: [ { name, sourceid, pic2 } ] } ] }
        guard let firstChild = root["child"] as? [[String: Any]],
              let inner = firstChild.first?["child"] as? [[String: Any]] else { return [] }
        return inner.compactMap { item in
            let name = item["name"] as? String ?? ""
            let id = String(item["sourceid"] as? Int ?? 0)
            let pic = item["pic2"] as? String ?? ""
            return BangBean(bangName: name, bangImgUrl: pic, bangId: id)
        }
    }

    /// 获取热门歌单列表
    static func getSongList() async throws -> [SongBean] {
        let params = ["pn": "1", "rn": "99", "order": "new"]
        let resp = try await HttpRequest.request(url: songApi, method: .GET, params: params)
        return parseSongList(resp)
    }

    /// 解析热门歌单列表 JSON
    static func parseSongList(_ json: String) -> [SongBean] {
        guard let data = json.data(using: .utf8),
              let root = try? JSONSerialization.jsonObject(with: data) as? [String: Any],
              let dataObj = root["data"] as? [String: Any],
              let arr = dataObj["data"] as? [[String: Any]] else { return [] }
        return arr.compactMap { item in
            let name = item["name"] as? String ?? ""
            let id = String(item["id"] as? Int ?? 0)
            let img = item["img"] as? String ?? ""
            let cnt = String(item["listencnt"] as? Int ?? 0)
            return SongBean(songName: name, songImgUrl: img, songId: id, listencnt: cnt)
        }
    }

    /// 获取歌单信息详情
    static func getMusicSongDetail(_ songId: String) async throws -> SongBean? {
        let params = ["type": "get_songlist_info2", "prod": "0", "id": songId]
        let resp = try await HttpRequest.request(url: musicSongUrl, method: .GET, params: params)
        return parseSongDetail(resp, songId: songId)
    }

    static func parseSongDetail(_ json: String, songId: String) -> SongBean? {
        guard let data = json.data(using: .utf8),
              let root = try? JSONSerialization.jsonObject(with: data) as? [String: Any],
              let sl = root["sl_data"] as? [String: Any] else { return nil }
        let title = sl["title"] as? String ?? ""
        let pic = sl["big_pic"] as? String ?? ""
        let cnt = String(sl["play_num"] as? Int ?? 0)
        return SongBean(songName: title, songImgUrl: pic, songId: songId, listencnt: cnt)
    }
}
