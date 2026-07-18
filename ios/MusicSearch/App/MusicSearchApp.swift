import SwiftUI

@main
struct MusicSearchApp: App {
    @StateObject private var player = MusicPlayerManager.shared
    @StateObject private var playerVM = MusicPlayerViewModel()

    init() {
        // 预初始化所有单例，确保在主线程完成，避免后台线程首次访问导致崩溃
        _ = PreferencesManager.shared
        _ = FavoriteStore.shared
        _ = LyricCacheStore.shared
        _ = NetworkMonitor.shared
        _ = DownloadManager.shared
        // 配置音频会话
        MusicPlayerManager.shared.configureAudioSession()
    }

    var body: some Scene {
        WindowGroup {
            RootView()
                .environmentObject(player)
                .environmentObject(playerVM)
                .environmentObject(HomeViewModel())
                .environmentObject(NetworkMonitor.shared)
                .environmentObject(FavoriteStore.shared)
                .environmentObject(DownloadManager.shared)
                .preferredColorScheme(nil) // 跟随系统
                .tint(AppTheme.primary)
        }
    }
}
