import Foundation

/// 轮播图数据模型（对应 Android BannerBean）
struct BannerBean: Identifiable, Hashable, Codable {
    var id: String { title + imageUrl }
    var title: String
    var imageUrl: String
    var addressUrl: String

    init(title: String = "", imageUrl: String = "", addressUrl: String = "") {
        self.title = title
        self.imageUrl = imageUrl
        self.addressUrl = addressUrl
    }
}
