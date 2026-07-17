import SwiftUI

@main
struct MusicSearchApp: App {
    @StateObject private var player = MusicPlayerManager.shared
    @StateObject private var playerVM = MusicPlayerViewModel()

    init() {
        // 配置远程控制
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
