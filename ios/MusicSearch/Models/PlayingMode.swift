import Foundation
import SwiftUI

/// 播放模式枚举（对应 Android PlayingMode）
/// 原命名 SHUNXU/SUIJI/XUNHUAN，语义：列表循环 / 随机 / 单曲循环
enum PlayingMode: String, CaseIterable, Codable {
    case shunxu
    case suiji
    case xunhuan

    var iconName: String {
        switch self {
        case .shunxu:  return "repeat"
        case .suiji:   return "shuffle"
        case .xunhuan: return "repeat.1"
        }
    }

    var label: String {
        switch self {
        case .shunxu:  return "列表循环"
        case .suiji:   return "随机播放"
        case .xunhuan: return "单曲循环"
        }
    }

    func next() -> PlayingMode {
        let all = PlayingMode.allCases
        let idx = all.firstIndex(of: self) ?? 0
        return all[(idx + 1) % all.count]
    }
}
