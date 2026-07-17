import SwiftUI
import UIKit

/// iOS 26 液态玻璃主题色与样式
enum AppTheme {
    // 主色（对应 Android main_primary #6750A4）
    static let primary = Color(red: 0x67/255, green: 0x50/255, blue: 0xA4/255)
    static let primaryLight = Color(red: 0x67/255, green: 0x50/255, blue: 0xA4/255, opacity: 0.15)
    static let background = Color(.systemGroupedBackground)
    static let surface = Color(.secondarySystemGroupedBackground)
    static let titleColor = Color.primary
    static let subtitleColor = Color.secondary
    static let likeColor = Color(red: 0xFF/255, green: 0x4E/255, blue: 0x00/255)

    // 酷我图片前缀
    static let albumCoverPrefix = "https://img4.kuwo.cn/star/albumcover/"
    static let defaultCover = "https://img1.kuwo.cn/star/albumcover/default.jpg"
}

/// 玻璃容器修饰符（iOS 26 Liquid Glass）
/// 在 iOS 26 上使用 .glassEffect，低版本回退到 .ultraThinMaterial
struct GlassBackground: ViewModifier {
    var cornerRadius: CGFloat = 16

    func body(content: Content) -> some View {
        // 注：.glassEffect 是 iOS 26 新 API
        // 当前 Xcode 16.4 的 iOS 18.5 SDK 不含此符号，直接用 .ultraThinMaterial
        // 未来用 Xcode 26+ 编译时，可改为：
        // if #available(iOS 26.0, *) {
        //     content.background(.glassEffect)...
        // }
        content
            .background(.ultraThinMaterial)
            .clipShape(RoundedRectangle(cornerRadius: cornerRadius, style: .continuous))
    }
}

extension View {
    func glassBackground(cornerRadius: CGFloat = 16) -> some View {
        modifier(GlassBackground(cornerRadius: cornerRadius))
    }
}
