import Foundation

/// 歌词条目
struct LyricEntry: Identifiable, Hashable {
    let id = UUID()
    let time: TimeInterval
    let text: String

    init(time: TimeInterval, text: String) {
        self.time = time
        self.text = text
    }
}
