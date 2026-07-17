import Foundation

/// 核心音乐业务 API（对应 Android MusicModel）
enum MusicModel {

    private static let bangMusicUrl = "http://kbangserver.kuwo.cn/ksong.s"
    private static let playListUrl = "https://mobilist.kuwo.cn/list.s"
    private static let searchUrl = "https://search.kuwo.cn/r.s"
    private static let musicPlayUrl = "http://nmobi.kuwo.cn/mobi.s?f=kuwo&q="
    private static let musicLyricUrl = "http://m.kuwo.cn/newh5/singles/songinfoandlrc"
    private static let musicDetailUrl = "https://musicpay.kuwo.cn/music.pay"

    // MARK: - 榜单音乐
    static func getBangMusic(id: String, page: Int, size: String) async throws -> [MusicBean] {
        let params = ["from": "pc", "type": "bang", "id": id, "pn": String(page), "rn": size]
        let resp = try await HttpRequest.request(url: bangMusicUrl, params: params)
        return parseMusicList(resp, key: "musiclist")
    }

    // MARK: - 歌单音乐
    static func getPlayListMusic(id: String, page: Int, size: String) async throws -> [MusicBean] {
        let params = ["type": "songlist", "id": id, "pn": String(page), "rn": size]
        let resp = try await HttpRequest.request(url: playListUrl, params: params)
        // 结构: { data: { musiclist: [...] } }
        guard let data = resp.data(using: .utf8),
              let root = try? JSONSerialization.jsonObject(with: data) as? [String: Any],
              let dataObj = root["data"] as? [String: Any] else { return [] }
        let arr = dataObj["musiclist"] as? [[String: Any]] ?? []
        return parseMusicArray(arr)
    }

    // MARK: - 搜索音乐
    static func getSearchMusic(name: String, page: Int, size: String) async throws -> [MusicBean] {
        let params: [String: String] = [
            "client": "kt", "all": name, "vipver": "1", "ft": "music",
            "encoding": "utf8", "rformat": "json", "mobi": "1",
            "pn": String(page), "rn": size
        ]
        let resp = try await HttpRequest.request(url: searchUrl, params: params)
        guard let data = resp.data(using: .utf8),
              let root = try? JSONSerialization.jsonObject(with: data) as? [String: Any],
              let arr = root["abslist"] as? [[String: Any]] else { return [] }
        return arr.compactMap { item in
            let rid = item["DC_TARGETID"] as? String ?? ""
            let n = item["NAME"] as? String ?? ""
            let a = item["ARTIST"] as? String ?? ""
            var img = item["web_albumpic_short"] as? String ?? ""
            if !img.isEmpty && !img.hasPrefix("http") {
                img = "https://img4.kuwo.cn/star/albumcover/" + img
            }
            if img.isEmpty || img.count < 5 {
                img = "https://img1.kuwo.cn/star/albumcover/default.jpg"
            }
            return MusicBean(id: rid, name: n, artist: a, albumIcon: img, like: false)
        }
    }

    // MARK: - 播放地址
    static func getMusicPlayInfo(musicId: String, bitrate: String) async throws -> String {
        let api = "user=0&android_id=0&prod=kwplayerhd_ar_4.3.0.8&corp=kuwo&vipver=4.3.0.8&source=kwplayerhd_ar_4.3.0.8_tianbao_T1A_qirui.apk&notrace=0&type=convert_url2&br=\(bitrate)&format=flac|mp3|aac&sig=0&priority=bitrate&loginUid=0&network=WIFI&loginSid=0&mode=down&rid=\(musicId)"
        let encrypted = EncryptMusicToken.encrypt(api)
        let url = musicPlayUrl + encrypted
        return try await HttpRequest.request(url: url)
    }

    // MARK: - 歌词
    static func getMusicLyric(musicId: String) async throws -> String {
        let params = ["musicId": musicId]
        // 该接口需要 Cookie
        return try await HttpRequest.requestWithCookie(url: musicLyricUrl, params: params,
            cookie: "kw_token= music")
    }

    // MARK: - 音质详情
    static func getMusicDetail(musicId: String) async throws -> [(bitrate: String, size: String)] {
        let params = ["op": "query", "action": "play", "ids": musicId]
        let resp = try await HttpRequest.request(url: musicDetailUrl, params: params)
        guard let data = resp.data(using: .utf8),
              let root = try? JSONSerialization.jsonObject(with: data) as? [String: Any],
              let songs = root["songs"] as? [[String: Any]],
              let first = songs.first,
              let minfo = first["MINFO"] as? String else { return [] }
        // MINFO 格式: "bitrate=2000|format=flac|size=12345;bitrate=320|format=mp3|size=6789;..."
        return minfo.split(separator: ";").compactMap { segment in
            let parts = segment.split(separator: "|")
            var br = "", sz = ""
            for p in parts {
                let kv = p.split(separator: "=")
                if kv.count == 2 {
                    if kv[0] == "bitrate" { br = String(kv[1]) }
                    if kv[0] == "size" { sz = String(kv[1]) }
                }
            }
            return br.isEmpty ? nil : (bitrate: br, size: sz)
        }.sorted { Int($0.bitrate) ?? 0 > Int($1.bitrate) ?? 0 }
    }

    // MARK: - 下载地址
    static func getMusicDownloadUrl(musicId: String, bitrate: String) async throws -> String? {
        let api = "user=0&android_id=0&prod=kwplayerhd_ar_4.3.0.8&corp=kuwo&vipver=4.3.0.8&source=kwplayerhd_ar_4.3.0.8_tianbao_T1A_qirui.apk&p2p=1&notrace=0&type=convert_url2&br=\(bitrate)&format=flac|mp3|aac&rid=\(musicId)&priority=bitrate&loginUid=0&network=WIFI&loginSid=0&mode=down"
        let encrypted = EncryptMusicToken.encrypt(api)
        let url = musicPlayUrl + encrypted
        let resp = try await HttpRequest.request(url: url)
        return MySubString.subString(resp, start: "url=", end: "sig")
    }

    // MARK: - 从播放地址响应中提取 url
    static func extractPlayUrl(_ resp: String) -> String? {
        return MySubString.subString(resp, start: "url=", end: "sig")
    }

    // MARK: - 通用解析
    private static func parseMusicList(_ json: String, key: String) -> [MusicBean] {
        guard let data = json.data(using: .utf8),
              let root = try? JSONSerialization.jsonObject(with: data) as? [String: Any],
              let arr = root[key] as? [[String: Any]] else { return [] }
        return parseMusicArray(arr)
    }

    private static func parseMusicArray(_ arr: [[String: Any]]) -> [MusicBean] {
        return arr.compactMap { item in
            let id = String(item["rid"] as? Int ?? 0)
            guard id != "0" || item["rid"] != nil else { return nil }
            let name = item["name"] as? String ?? ""
            let artist = item["artist"] as? String ?? ""
            var img = item["albumpic"] as? String ?? item["pic"] as? String ?? item["img"] as? String ?? item["album_img"] as? String ?? item["album_pic"] as? String ?? ""
            if !img.isEmpty && img.count >= 5 && !img.hasPrefix("http") {
                img = "https://img4.kuwo.cn/star/albumcover/" + img
            }
            if img.isEmpty || img.count < 5 {
                img = "https://img1.kuwo.cn/star/albumcover/default.jpg"
            }
            return MusicBean(id: id, name: name, artist: artist, albumIcon: img, like: false)
        }
    }
}
