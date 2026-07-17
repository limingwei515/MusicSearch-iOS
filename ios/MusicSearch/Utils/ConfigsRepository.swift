import Foundation

/// 远程配置内存仓库（对应 Android ConfigsRepository）
enum ConfigsRepository {
    private static var configs: [String: Any] = [:]

    static func getConfigs() -> [String: Any] { configs }

    static func setConfigs(_ map: [String: Any]) {
        configs = map
    }

    static func getBanners() -> [BannerBean] {
        guard let list = configs["首页轮播图"] as? [[String: String]] else { return [] }
        return list.compactMap { item in
            BannerBean(title: item["标题"] ?? item["title"] ?? "",
                       imageUrl: item["图片地址"] ?? item["imageUrl"] ?? "",
                       addressUrl: item["跳转地址"] ?? item["addressUrl"] ?? "")
        }
    }

    static func getUpdateConfig() -> [String: Any]? {
        configs["软件更新控制"] as? [String: Any]
    }

    static func getFeedbackQQGroup() -> String? {
        (configs["杂项配置"] as? [String: Any])?["反馈Q群"] as? String
    }
}

/// 本地默认配置（对应 Android LocalConfigs）
enum LocalConfigs {

    static func getLocalConfigs() -> [String: Any] {
        return [
            "启动页控制": [
                "启动页倒计时": 3,
                "启动页副标题": "音乐搜索 重构版",
                "启动页背景图": "https://img1.kuwo.cn/star/albumcover/default.jpg",
                "启动页跳转地址": ""
            ],
            "软件更新控制": [
                "版本": "2.4.0",
                "更新弹窗标题": "发现新版本",
                "更新弹窗内容": "1. 修复已知问题<br>2. 优化用户体验",
                "强制更新": "关闭",
                "更新地址": ""
            ],
            "首页轮播图": [
                ["标题": "音乐搜索", "图片地址": "https://img1.kuwo.cn/star/albumcover/default.jpg", "跳转地址": ""]
            ],
            "杂项配置": ["反馈Q群": "324372634"]
        ]
    }
}
