import SwiftUI

/// 全屏播放器（对应 MusicActivity BottomSheet 展开态）
struct PlayerView: View {
    @Environment(\.dismiss) var dismiss
    @EnvironmentObject var player: MusicPlayerManager
    @EnvironmentObject var playerVM: MusicPlayerViewModel
    @State private var showLyric = false
    @State private var seeking = false
    @State private var seekValue: Double = 0
    @State private var showQuality = false
    @State private var dragOffset: CGSize = .zero

    var body: some View {
        ZStack {
            // 背景模糊
            AlbumArtView(url: player.currentMusic?.albumIcon ?? "", size: 400, cornerRadius: 0)
                .frame(maxWidth: .infinity, maxHeight: .infinity)
                .blur(radius: 60)
                .overlay(Color.black.opacity(0.4))
                .ignoresSafeArea()

            VStack(spacing: 0) {
                // 顶部拖拽条 + 关闭
                topBar

                Spacer()

                // 封面/歌词切换
                if showLyric {
                    lyricView
                } else {
                    albumArtView
                }

                Spacer()

                // 歌曲信息
                songInfo

                // 进度条
                progressBar

                // 控制按钮
                controlBar

                // 底部操作
                bottomActions
            }
            .padding(.horizontal, 24)
            .padding(.bottom, 40)
        }
        .offset(y: dragOffset.height > 0 ? dragOffset.height : 0)
        .gesture(
            DragGesture()
                .onChanged { v in dragOffset = v.translation }
                .onEnded { v in
                    if v.translation.height > 100 { dismiss() }
                    dragOffset = .zero
                }
        )
        .preferredColorScheme(.dark)
    }

    // MARK: - 顶部
    private var topBar: some View {
        HStack {
            Button { dismiss() } label: {
                Image(systemName: "chevron.down")
                    .font(.title3)
                    .foregroundStyle(.white)
            }
            Spacer()
            Text("正在播放")
                .font(.caption)
                .foregroundStyle(.white.opacity(0.7))
            Spacer()
            Button {
                withAnimation(.easeInOut(duration: 0.25)) { showLyric.toggle() }
            } label: {
                Image(systemName: showLyric ? "photo" : "text.quote")
                    .font(.title3)
                    .foregroundStyle(.white)
            }
        }
        .padding(.top, 8)
    }

    // MARK: - 封面
    private var albumArtView: some View {
        AlbumArtView(url: player.currentMusic?.albumIcon ?? "", size: 300, cornerRadius: 24)
            .shadow(color: .black.opacity(0.5), radius: 20, x: 0, y: 10)
    }

    // MARK: - 歌词
    private var lyricView: some View {
        ScrollViewReader { proxy in
            ScrollView {
                LazyVStack(spacing: 12) {
                    ForEach(Array(player.lyricList.enumerated()), id: \.element.id) { idx, entry in
                        Text(entry.text)
                            .font(isCurrentLyric(idx) ? .body : .subheadline)
                            .foregroundStyle(isCurrentLyric(idx) ? .white : .white.opacity(0.4))
                            .padding(.vertical, 4)
                            .id(idx)
                    }
                }
                .padding(.vertical, 40)
            }
            .onChange(of: player.progress) { _ in
                if let idx = currentLyricIndex() {
                    withAnimation(.easeInOut) { proxy.scrollTo(idx, anchor: .center) }
                }
            }
        }
        .frame(maxHeight: 320)
    }

    private func currentLyricIndex() -> Int? {
        let entries = player.lyricList
        for (i, e) in entries.enumerated() where e.time > player.progress {
            return i > 0 ? i - 1 : 0
        }
        return entries.last != nil ? entries.count - 1 : nil
    }

    private func isCurrentLyric(_ idx: Int) -> Bool {
        currentLyricIndex() == idx
    }

    // MARK: - 歌曲信息
    private var songInfo: some View {
        VStack(spacing: 4) {
            Text(player.currentMusic?.name ?? "未在播放")
                .font(.title3.weight(.semibold))
                .foregroundStyle(.white)
                .lineLimit(1)
            Text(player.currentMusic?.artist ?? "")
                .font(.subheadline)
                .foregroundStyle(.white.opacity(0.7))
                .lineLimit(1)
        }
        .padding(.vertical, 16)
    }

    // MARK: - 进度条
    private var progressBar: some View {
        VStack(spacing: 4) {
            Slider(value: Binding(
                get: { seeking ? seekValue : player.progress },
                set: { newVal in
                    seekValue = newVal
                    seeking = true
                }
            ), in: 0...max(player.duration, 1), onEditingChanged: { editing in
                if !editing {
                    playerVM.seekTo(seekValue)
                    seeking = false
                }
            })
            .tint(.white)

            HStack {
                Text(CommonUtils.formatTime(Int(seeking ? seekValue : player.progress)))
                    .font(.caption2)
                    .foregroundStyle(.white.opacity(0.7))
                Spacer()
                Text(CommonUtils.formatTime(Int(player.duration)))
                    .font(.caption2)
                    .foregroundStyle(.white.opacity(0.7))
            }
        }
        .padding(.vertical, 8)
    }

    // MARK: - 控制按钮
    private var controlBar: some View {
        HStack(spacing: 0) {
            // 播放模式
            Button {
                let next = player.playingMode.next()
                playerVM.togglePlayStatus(next)
            } label: {
                Image(systemName: player.playingMode.iconName)
                    .font(.title2)
                    .foregroundStyle(.white)
            }
            Spacer()

            // 上一首
            Button { playerVM.playPrevious() } label: {
                Image(systemName: "backward.fill")
                    .font(.title)
                    .foregroundStyle(.white)
            }
            Spacer()

            // 播放/暂停
            Button { playerVM.toggleMusic() } label: {
                Image(systemName: player.isPlaying ? "pause.circle.fill" : "play.circle.fill")
                    .font(.system(size: 64))
                    .foregroundStyle(.white)
            }
            Spacer()

            // 下一首
            Button { playerVM.playNext() } label: {
                Image(systemName: "forward.fill")
                    .font(.title)
                    .foregroundStyle(.white)
            }
            Spacer()

            // 收藏
            Button { playerVM.toggleLike() } label: {
                Image(systemName: playerVM.isLiked ? "heart.fill" : "heart")
                    .font(.title2)
                    .foregroundStyle(playerVM.isLiked ? AppTheme.likeColor : .white)
            }
        }
        .padding(.vertical, 24)
    }

    // MARK: - 底部操作
    private var bottomActions: some View {
        HStack(spacing: 24) {
            Button { showQuality = true } label: {
                Image(systemName: "arrow.down.circle")
                    .font(.title3)
                    .foregroundStyle(.white)
            }
            Button { } label: {
                Image(systemName: "list.bullet")
                    .font(.title3)
                    .foregroundStyle(.white)
            }
            Button { } label: {
                Image(systemName: "ellipsis.circle")
                    .font(.title3)
                    .foregroundStyle(.white)
            }
        }
        .sheet(isPresented: $showQuality) {
            if let m = player.currentMusic {
                QualityDialog(tonalList: playerVM.musicTonalList, isPresented: $showQuality) { bitrate in
                    playerVM.getMusicDownloadUrl(music: m, bitrate: bitrate)
                }
                .onAppear { playerVM.getMusicDetail(for: m) }
                .presentationDetents([.medium])
            }
        }
        .onChange(of: playerVM.musicDownloadUrl) { url in
            guard let url, let m = player.currentMusic else { return }
            let prefs = PreferencesManager.shared
            var dir = prefs.getString(.downloadPath, default: "")
            if dir.isEmpty {
                let docs = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask)[0]
                dir = docs.appendingPathComponent("音频").path
            }
            try? FileManager.default.createDirectory(atPath: dir, withIntermediateDirectories: true)
            let safeName = "\(m.artist) - \(m.name)".replacingOccurrences(of: "/", with: "-")
            let path = "\(dir)/\(safeName).mp3"
            DownloadManager.shared.addDownload(url: url, filePath: path,
                                                albumIcon: m.albumIcon, tonalName: "")
        }
    }
}
