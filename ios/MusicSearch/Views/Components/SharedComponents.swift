import SwiftUI

/// 通用空状态视图
struct EmptyStateView: View {
    let icon: String
    let title: String
    let subtitle: String

    var body: some View {
        VStack(spacing: 12) {
            Image(systemName: icon)
                .font(.system(size: 56))
                .foregroundStyle(.secondary)
            Text(title)
                .font(.headline)
                .foregroundStyle(.secondary)
            Text(subtitle)
                .font(.subheadline)
                .foregroundStyle(.tertiary)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }
}

/// 音乐列表行视图（对应 item_music_list.xml）
struct MusicRowView: View {
    let bean: MusicBean
    var showNumber: Bool = false
    var index: Int = 0
    var isLiked: Bool = false
    var onTap: () -> Void
    var onLike: () -> Void
    var onDownload: () -> Void

    var body: some View {
        HStack(spacing: 12) {
            // 封面或序号
            if showNumber {
                Text("\(index + 1)")
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
                    .frame(width: 28, alignment: .center)
            } else {
                AlbumArtView(url: bean.albumIcon, size: 52)
            }

            VStack(alignment: .leading, spacing: 4) {
                Text(bean.name)
                    .font(.subheadline)
                    .lineLimit(1)
                Text(bean.artist)
                    .font(.caption)
                    .foregroundStyle(.secondary)
                    .lineLimit(1)
            }

            Spacer()

            // 收藏按钮
            Button(action: onLike) {
                Image(systemName: isLiked ? "heart.fill" : "heart")
                    .foregroundStyle(isLiked ? AppTheme.likeColor : AppTheme.primary)
                    .font(.system(size: 18))
            }
            .buttonStyle(.plain)

            // 下载按钮
            Button(action: onDownload) {
                Image(systemName: "arrow.down.circle")
                    .foregroundStyle(AppTheme.primary)
                    .font(.system(size: 20))
            }
            .buttonStyle(.plain)
        }
        .contentShape(Rectangle())
        .onTapGesture(perform: onTap)
        .padding(.vertical, 4)
    }
}

/// 专辑封面视图
struct AlbumArtView: View {
    let url: String
    var size: CGFloat = 50
    var cornerRadius: CGFloat = 8

    var body: some View {
        AsyncImage(url: URL(string: url)) { phase in
            switch phase {
            case .empty:
                RoundedRectangle(cornerRadius: cornerRadius)
                    .fill(AppTheme.primaryLight)
                    .overlay(ProgressView())
            case .success(let image):
                image.resizable()
                    .scaledToFill()
            case .failure:
                RoundedRectangle(cornerRadius: cornerRadius)
                    .fill(AppTheme.primaryLight)
                    .overlay(Image(systemName: "music.note").foregroundStyle(.secondary))
            @unknown default:
                RoundedRectangle(cornerRadius: cornerRadius)
                    .fill(AppTheme.primaryLight)
            }
        }
        .frame(width: size, height: size)
        .clipShape(RoundedRectangle(cornerRadius: cornerRadius, style: .continuous))
    }
}

/// 榜单卡片（对应 item_bang_list.xml）
struct BangCardView: View {
    let bean: BangBean
    var onTap: () -> Void

    var body: some View {
        VStack(spacing: 8) {
            AlbumArtView(url: bean.bangImgUrl, size: 100, cornerRadius: 12)
            Text(bean.bangName)
                .font(.caption)
                .lineLimit(1)
                .foregroundStyle(.primary)
        }
        .frame(width: 108)
        .onTapGesture(perform: onTap)
    }
}

/// 歌单卡片（对应 item_song_list.xml）
struct SongCardView: View {
    let bean: SongBean
    var isMySong: Bool = false
    var onTap: () -> Void
    var onLongPress: () -> Void

    var body: some View {
        VStack(spacing: 6) {
            ZStack(alignment: .bottomTrailing) {
                AlbumArtView(url: bean.songImgUrl, size: 100, cornerRadius: 12)
                if !bean.listencnt.isEmpty {
                    Text(CommonUtils.formatNumber(bean.listencnt))
                        .font(.system(size: 9))
                        .padding(.horizontal, 5)
                        .padding(.vertical, 2)
                        .background(.ultraThinMaterial, in: Capsule())
                        .padding(5)
                }
            }
            Text(bean.songName)
                .font(.caption)
                .lineLimit(1)
                .foregroundStyle(.primary)
        }
        .frame(width: 108)
        .onTapGesture(perform: onTap)
        .onLongPressGesture(perform: onLongPress)
    }
}

/// 搜索弹窗（对应 dialog_music_search.xml）
struct SearchSheet: View {
    @Binding var isPresented: Bool
    @State private var searchText = ""
    var onSearch: (String) -> Void

    var body: some View {
        VStack(spacing: 16) {
            Text("搜索音乐")
                .font(.headline)
            Text("输入歌曲名或歌手名")
                .font(.subheadline)
                .foregroundStyle(.secondary)

            TextField("搜索音乐", text: $searchText)
                .textFieldStyle(.roundedBorder)
                .autocorrectionDisabled()
                .submitLabel(.search)

            HStack(spacing: 12) {
                Button("取消") { isPresented = false }
                    .buttonStyle(.bordered)
                    .tint(.secondary)
                Button("搜索") {
                    let trimmed = searchText.trimmingCharacters(in: .whitespacesAndNewlines)
                    if !trimmed.isEmpty {
                        onSearch(trimmed)
                        isPresented = false
                    }
                }
                .buttonStyle(.borderedProminent)
            }
        }
        .padding(24)
        .presentationDetents([.medium])
    }
}

/// 音质选择对话框
struct QualityDialog: View {
    let tonalList: [(bitrate: String, size: String)]
    @Binding var isPresented: Bool
    var onSelect: (String) -> Void

    var body: some View {
        VStack(spacing: 12) {
            Text("选择音质")
                .font(.headline)

            ForEach(tonalList, id: \.bitrate) { item in
                Button {
                    onSelect(item.bitrate)
                    isPresented = false
                } label: {
                    HStack {
                        Text(MusicViewModel.tonalName(item.bitrate))
                        Spacer()
                        if !item.size.isEmpty {
                            Text(formatSize(item.size))
                                .font(.caption)
                                .foregroundStyle(.secondary)
                        }
                    }
                    .padding(.vertical, 8)
                    .padding(.horizontal, 16)
                }
                .buttonStyle(.plain)
                Divider()
            }
        }
        .padding()
    }

    private func formatSize(_ sizeStr: String) -> String {
        guard let bytes = Int64(sizeStr) else { return "" }
        if bytes > 1_000_000 { return String(format: "%.1f MB", Double(bytes) / 1_000_000) }
        if bytes > 1000 { return String(format: "%.0f KB", Double(bytes) / 1000) }
        return "\(bytes) B"
    }
}
