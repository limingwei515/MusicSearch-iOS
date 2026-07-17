import Foundation

/// 更新日志条目（对应 assets/updateLog.json）
struct UpdateLogItem: Identifiable, Hashable, Codable {
    var id: String { (version ?? "") + (time ?? "") }
    let version: String?
    let time: String?
    let content: String?
}
