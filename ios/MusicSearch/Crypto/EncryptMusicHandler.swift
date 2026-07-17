import Foundation

/// 酷我音乐接口 Base64 编码（对应 Android EncryptMusicHandler）
/// 码表为标准 Base64（A-Z a-z 0-9 + /），可直接使用系统 Base64。
enum EncryptMusicHandler {
    static func encode(_ bytes: [UInt8], salt: String? = nil) -> String {
        var data = bytes
        if let salt, !salt.isEmpty {
            let saltBytes = Array(salt.utf8)
            if !saltBytes.isEmpty {
                var i = 0
                while i < data.count {
                    for j in 0..<saltBytes.count {
                        if i >= data.count { break }
                        data[i] ^= saltBytes[j]
                        i += 1
                    }
                }
            }
        }
        return Data(data).base64EncodedString()
    }
}
