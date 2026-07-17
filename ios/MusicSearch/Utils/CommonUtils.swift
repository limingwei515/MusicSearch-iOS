import Foundation
import UIKit

/// 通用工具（对应 Android CommonUtils）
enum CommonUtils {

    /// 数字格式化：≥5 位除以 10000 保留 1 位小数加"万"
    static func formatNumber(_ numberStr: String) -> String {
        guard let num = Int(numberStr) else { return "0" }
        let count = abs(num)
        if count < 10000 { return numberStr }
        let wan = Double(num) / 10000.0
        return String(format: "%.1f万", (wan * 10).rounded(.down) / 10)
    }

    /// 秒数转 mm:ss
    static func formatTime(_ seconds: Int) -> String {
        guard seconds >= 0 else { return "00:00" }
        let m = seconds / 60
        let s = seconds % 60
        return String(format: "%02d:%02d", m, s)
    }

    /// 毫秒转 mm:ss
    static func formatTimeMs(_ ms: Int) -> String {
        formatTime(ms / 1000)
    }

    /// 从 Bundle 读取文件内容
    static func getFileContent(fromBundle name: String, ext: String) -> String? {
        guard let url = Bundle.main.url(forResource: name, withExtension: ext) else { return nil }
        return try? String(contentsOf: url, encoding: .utf8)
    }

    /// 获取 App 版本号
    static var versionName: String {
        return Bundle.main.infoDictionary?["CFBundleShortVersionString"] as? String ?? "1.0.0"
    }

    /// 获取 App Build 号
    static var versionCode: String {
        return Bundle.main.infoDictionary?["CFBundleVersion"] as? String ?? "1"
    }
}

/// 字符串截取工具（对应 Android MySubString）
enum MySubString {
    static func subStartString(_ input: String, start: String) -> String? {
        guard let range = input.range(of: start) else { return nil }
        return String(input[range.upperBound...])
    }

    static func subEndString(_ input: String, end: String) -> String? {
        guard let range = input.range(of: end) else { return nil }
        return String(input[..<range.lowerBound])
    }

    static func subString(_ input: String, start: String, end: String) -> String? {
        guard let sRange = input.range(of: start) else { return nil }
        let afterStart = input[sRange.upperBound...]
        guard let eRange = afterStart.range(of: end) else { return nil }
        return String(afterStart[..<eRange.lowerBound])
    }
}
