import SwiftUI

/// 迷你播放器栏（对应 MusicActivity BottomSheet 折叠态）
struct MiniPlayerBar: View {
    @EnvironmentObject var player: MusicPlayerManager
    @EnvironmentObject var playerVM: MusicPlayerViewModel
    var onTap: () -> Void

    var body: some View {
        HStack(spacing: 12) {
            // 封面
            AlbumArtView(url: player.currentMusic?.albumIcon ?? "", size: 44, cornerRadius: 8)

            // 歌名/歌手
            VStack(alignment: .leading, spacing: 2) {
                Text(player.currentMusic?.name ?? "未在播放")
                    .font(.subheadline)
                    .lineLimit(1)
                Text(player.currentMusic?.artist ?? "")
                    .font(.caption)
                    .foregroundStyle(.secondary)
                    .lineLimit(1)
            }
            .frame(maxWidth: .infinity, alignment: .leading)

            // 进度环
            ZStack {
                Circle()
                    .stroke(Color.secondary.opacity(0.2), lineWidth: 3)
                Circle()
                    .trim(from: 0, to: player.duration > 0 ? CGFloat(player.progress / player.duration) : 0)
                    .stroke(AppTheme.primary, style: StrokeStyle(lineWidth: 3, lineCap: .round))
                    .rotationEffect(.degrees(-90))
            }
            .frame(width: 32, height: 32)

            // 播放/暂停
            Button { playerVM.toggleMusic() } label: {
                Image(systemName: player.isPlaying ? "pause.fill" : "play.fill")
                    .font(.system(size: 22))
                    .foregroundStyle(AppTheme.primary)
            }
        }
        .padding(.horizontal, 12)
        .padding(.vertical, 8)
        .glassBackground(cornerRadius: 20)
        .padding(.horizontal, 8)
        .onTapGesture(perform: onTap)
    }
}
