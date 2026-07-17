import SwiftUI

/// 歌曲列表页（对应 Android MusicFragment）
/// 路由格式：search:<keyword>:<title> / bang:<id>:<name> / playlist:<id>:<name> / like
struct MusicListView: View {
    let route: String
    @StateObject private var viewModel = MusicViewModel()
    @StateObject private var player = MusicPlayerManager.shared
    @StateObject private var playerVM = MusicPlayerViewModel()
    @EnvironmentObject var favoriteStore: FavoriteStore
    @State private var showSearch = false
    @State private var showQuality = false
    @State private var selectedMusic: MusicBean?

    private var parts: [String] {
        route.components(separatedBy: ":")
    }

    private var type: MusicViewModel.ListType {
        switch parts.first {
        case "search": return .search
        case "bang": return .bang
        case "playlist": return .playlist
        case "like": return .like
        default: return .search
        }
    }

    private var id: String { parts.count > 1 ? parts[1] : "" }
    private var title: String { parts.count > 2 ? parts.dropFirst(2).joined(separator: ":") : "" }

    var body: some View {
        ZStack {
            if viewModel.musicList.isEmpty && !viewModel.isLoading {
                if type == .like {
                    EmptyStateView(icon: "heart.slash", title: "还没有收藏的歌曲", subtitle: "")
                } else if viewModel.loadSuccess {
                    EmptyStateView(icon: "music.note.list", title: "暂无歌曲", subtitle: "下拉刷新试试")
                } else {
                    EmptyStateView(icon: "wifi.exclamationmark", title: "加载失败", subtitle: "下拉刷新重试")
                }
            } else {
                List {
                    ForEach(Array(viewModel.musicList.enumerated()), id: \.element.id) { idx, bean in
                        MusicRowView(
                            bean: bean,
                            showNumber: type == .bang || type == .search,
                            index: idx,
                            isLiked: viewModel.isFavorite(bean.id),
                            onTap: {
                                playerVM.playMusic(id: bean.id, position: idx, musicList: viewModel.musicList)
                            },
                            onLike: { viewModel.toggleLike(bean) },
                            onDownload: {
                                selectedMusic = bean
                                viewModel.getMusicDetail(for: bean)
                                showQuality = true
                            }
                        )
                    }
                    if viewModel.hasMoreData && !viewModel.musicList.isEmpty {
                        Color.clear
                            .onAppear { viewModel.loadMore() }
                            .frame(height: 1)
                    }
                }
                .listStyle(.plain)
            }
        }
        .navigationTitle(title.isEmpty ? "歌曲列表" : title)
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .topBarTrailing) {
                Button { showSearch = true } label: {
                    Image(systemName: "magnifyingglass")
                }
            }
        }
        .refreshable { viewModel.refresh() }
        .onAppear {
            viewModel.configure(type: type, id: id, name: title.isEmpty ? id : title)
            if viewModel.musicList.isEmpty { viewModel.refresh() }
        }
        .sheet(isPresented: $showSearch) {
            SearchSheet(isPresented: $showSearch) { keyword in
                viewModel.configure(type: .search, id: "", name: keyword)
                viewModel.refresh()
            }
            .presentationDetents([.medium])
        }
        .sheet(isPresented: $showQuality) {
            if let m = selectedMusic {
                QualityDialog(tonalList: viewModel.musicTonalList, isPresented: $showQuality) { bitrate in
                    viewModel.getMusicDownloadUrl(music: m, bitrate: bitrate)
                }
                .presentationDetents([.medium])
            }
        }
        .onChange(of: viewModel.musicDownloadUrl) { _, newUrl in
            guard let url = newUrl, let m = selectedMusic else { return }
            startDownload(url: url, music: m, bitrate: "")
        }
    }

    private func startDownload(url: String, music: MusicBean, bitrate: String) {
        let prefs = PreferencesManager.shared
        var dir = prefs.getString(.downloadPath, default: "")
        if dir.isEmpty {
            let docs = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask)[0]
            dir = docs.appendingPathComponent("音频").path
        }
        try? FileManager.default.createDirectory(atPath: dir, withIntermediateDirectories: true)
        let safeName = "\(music.artist) - \(music.name)".replacingOccurrences(of: "/", with: "-")
        let ext = MusicViewModel.tonalExt(bitrate)
        let path = "\(dir)/\(safeName).\(ext)"
        DownloadManager.shared.addDownload(url: url, filePath: path,
                                            albumIcon: music.albumIcon, tonalName: bitrate)
    }
}
