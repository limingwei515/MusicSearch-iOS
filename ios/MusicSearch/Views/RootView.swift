import SwiftUI

/// 根视图：启动页 -> 主界面（含底部播放器栏）
struct RootView: View {
    @State private var showSplash = true
    @StateObject private var prefs = PreferencesManager.shared
    @State private var hasShownSplash = false

    var body: some View {
        ZStack {
            if showSplash && !hasShownSplash {
                SplashView {
                    hasShownSplash = true
                    withAnimation(.easeInOut(duration: 0.4)) {
                        showSplash = false
                    }
                }
                .transition(.opacity)
            } else {
                MainTabView()
                    .transition(.opacity)
            }
        }
        .animation(.easeInOut(duration: 0.4), value: showSplash)
    }
}

/// 主 Tab 视图（首页 / 收藏 / 下载 / 设置），带液态玻璃 TabBar
struct MainTabView: View {
    @State private var selectedTab = 0
    @EnvironmentObject private var player: MusicPlayerManager
    @State private var showFullPlayer = false

    var body: some View {
        ZStack(alignment: .bottom) {
            TabView(selection: $selectedTab) {
                HomeView()
                    .tabItem { Label("首页", systemImage: "music.note.house") }
                    .tag(0)

                FavoriteListView()
                    .tabItem { Label("收藏", systemImage: "heart") }
                    .tag(1)

                DownloadView()
                    .tabItem { Label("下载", systemImage: "arrow.down.circle") }
                    .tag(2)

                SettingsView()
                    .tabItem { Label("设置", systemImage: "gearshape") }
                    .tag(3)
            }
            .tint(AppTheme.primary)

            // 底部迷你播放器（有歌曲时显示）
            if player.currentMusic != nil {
                VStack(spacing: 0) {
                    MiniPlayerBar {
                        showFullPlayer = true
                    }
                }
                .padding(.bottom, 49) // TabBar 高度
            }
        }
        .fullScreenCover(isPresented: $showFullPlayer) {
            PlayerView()
        }
    }
}

/// 收藏列表视图
struct FavoriteListView: View {
    @EnvironmentObject var favoriteStore: FavoriteStore
    @EnvironmentObject private var playerVM: MusicPlayerViewModel
    @EnvironmentObject private var player: MusicPlayerManager

    var body: some View {
        NavigationStack {
            Group {
                if favoriteStore.favorites.isEmpty {
                    EmptyStateView(icon: "heart.slash", title: "还没有收藏", subtitle: "去首页发现喜欢的音乐吧")
                } else {
                    List {
                        ForEach(favoriteStore.favorites) { bean in
                            MusicRowView(bean: bean, showNumber: false, isLiked: true) {
                                playerVM.playMusic(id: bean.id, position: 0,
                                                  musicList: favoriteStore.favorites)
                            } onLike: {
                                favoriteStore.toggle(bean)
                            } onDownload: { }
                        }
                    }
                    .listStyle(.plain)
                }
            }
            .navigationTitle("我的收藏")
            .navigationBarTitleDisplayMode(.large)
        }
    }
}
