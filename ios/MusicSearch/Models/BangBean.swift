import Foundation

/// 榜单数据模型（对应 Android BangBean）
struct BangBean: Identifiable, Hashable, Codable {
    var id: String { bangId }
    var bangName: String
    var bangImgUrl: String
    var bangId: String

    init(bangName: String = "", bangImgUrl: String = "", bangId: String = "") {
        self.bangName = bangName
        self.bangImgUrl = bangImgUrl
        self.bangId = bangId
    }
}
