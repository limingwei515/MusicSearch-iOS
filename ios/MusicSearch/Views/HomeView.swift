import SwiftUI

/// 首页（对应 Android HomeFragment）
struct HomeView: View {
    @EnvironmentObject var homeVM: HomeViewModel
    @EnvironmentObject private var player: MusicPlayerManager
    @EnvironmentObject private var playerVM: MusicPlayerViewModel
    @State private var showSearch = false
    @State private var searchText = ""
    @State private var navigationPath = NavigationPath()

    var body: some View {
        NavigationStack(path: $navigationPath) {
            ScrollView {
                VStack(spacing: 20) {
                    // 轮播图
                    bannerSection

                    // 我的歌单
                    mySongSection

                    // 热门榜单
                    bangSection

                    // 推荐歌单
                    songSection
                }
                .padding(.horizontal, 16)
                .padding(.bottom, 80)
            }
            .navigationTitle("音乐搜索")
            .navigationBarTitleDisplayMode(.large)
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button { showSearch = true } label: {
                        Image(systemName: "magnifyingglass")
                    }
                }
            }
            .background(AppTheme.background)
            .navigationDestination(for: String.self) { route in
                // 路由："bang:<id>:<name>" / "playlist:<id>:<name>" / "like"
                MusicListView(route: route)
            }
        }
        .onAppear {
            if homeVM.bangList.isEmpty { homeVM.initBangConfig() }
            if homeVM.songList.isEmpty { homeVM.initSongConfig() }
        }
        .sheet(isPresented: $showSearch) {
            SearchSheet(isPresented: $showSearch) { keyword in
                navigationPath.append("search:\(keyword):\(keyword)")
            }
            .presentationDetents([.medium])
        }
    }

    // MARK: - 轮播图
    private var bannerSection: some View {
        TabView {
            ForEach(ConfigsRepository.getBanners().isEmpty ?
                    [BannerBean(title: "音乐搜索", imageUrl: AppTheme.defaultCover, addressUrl: "")] :
                    ConfigsRepository.getBanners()) { banner in
                ZStack(alignment: .bottomLeading) {
                    AlbumArtView(url: banner.imageUrl, size: 160, cornerRadius: 16)
                        .frame(height: 160)
                        .frame(maxWidth: .infinity)
                    if !banner.title.isEmpty {
                        Text(banner.title)
                            .font(.caption)
                            .padding(6)
                            .background(.ultraThinMaterial, in: Capsule())
                            .padding(8)
                    }
                }
                .frame(height: 160)
            }
        }
        .tabViewStyle(.page(indexDisplayMode: .always))
        .frame(height: 180)
    }

    // MARK: - 我的歌单
    private var mySongSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack {
                Text("我的歌单").font(.headline)
                Spacer()
                Button {
                    // 添加歌单 - 简化版直接跳转搜索
                    showAddSongDialog()
                } label: {
                    Image(systemName: "plus.circle.fill")
                        .foregroundStyle(AppTheme.primary)
                }
            }

            if homeVM.mySongList.isEmpty {
                Text("暂无歌单，点击右上角添加")
                    .font(.caption)
                    .foregroundStyle(.secondary)
            } else {
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 12) {
                        ForEach(homeVM.mySongList) { song in
                            SongCardView(
                                bean: song,
                                isMySong: true,
                                onTap: { navigationPath.append("playlist:\(song.songId):\(song.songName)") },
                                onLongPress: {
                                    if let idx = homeVM.mySongList.firstIndex(of: song) {
                                        homeVM.removeMySong(at: idx)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    @State private var showAddSong = false
    @State private var songIdInput = ""

    private func showAddSongDialog() {
        showAddSong = true
    }

    // MARK: - 热门榜单
    private var bangSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("热门榜单").font(.headline)
            if homeVM.bangList.isEmpty {
                LoadingPlaceholder()
            } else {
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 12) {
                        ForEach(homeVM.bangList) { bang in
                            BangCardView(bean: bang) {
                                navigationPath.append("bang:\(bang.bangId):\(bang.bangName)")
                            }
                        }
                    }
                }
            }
        }
    }

    // MARK: - 推荐歌单
    private var songSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("推荐歌单").font(.headline)
            if homeVM.songList.isEmpty {
                LoadingPlaceholder()
            } else {
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 12) {
                        ForEach(homeVM.songList) { song in
                            SongCardView(
                                bean: song,
                                onTap: { navigationPath.append("playlist:\(song.songId):\(song.songName)") },
                                onLongPress: { }
                            )
                        }
                    }
                }
            }
        }
    }
}

/// 加载占位
struct LoadingPlaceholder: View {
    var body: some View {
        HStack(spacing: 12) {
            ForEach(0..<3) { _ in
                RoundedRectangle(cornerRadius: 12)
                    .fill(AppTheme.primaryLight)
                    .frame(width: 100, height: 130)
            }
        }
        .redacted(reason: .placeholder)
    }
}
