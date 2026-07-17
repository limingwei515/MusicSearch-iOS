import SwiftUI
import UniformTypeIdentifiers

/// 下载页面（对应 Android DownloadActivity）
struct DownloadView: View {
    @EnvironmentObject var downloadManager: DownloadManager

    var body: some View {
        NavigationStack {
            Group {
                if downloadManager.tasks.isEmpty {
                    EmptyStateView(icon: "arrow.down.circle", title: "暂无下载任务", subtitle: "下载的歌曲会在这里显示")
                } else {
                    List {
                        ForEach(downloadManager.tasks) { task in
                            DownloadRow(task: task)
                        }
                        .onDelete { indexSet in
                            for i in indexSet { downloadManager.cancel(downloadManager.tasks[i].id) }
                        }
                    }
                    .listStyle(.plain)
                }
            }
            .navigationTitle("下载")
            .navigationBarTitleDisplayMode(.large)
            .toolbar {
                if !downloadManager.tasks.isEmpty {
                    ToolbarItem(placement: .topBarTrailing) {
                        Menu {
                            Button("全部删除", role: .destructive) {
                                downloadManager.removeAll()
                            }
                        } label: {
                            Image(systemName: "ellipsis.circle")
                        }
                    }
                }
            }
        }
    }
}

/// 下载列表行
struct DownloadRow: View {
    let task: DownloadTask
    @EnvironmentObject var downloadManager: DownloadManager

    var body: some View {
        HStack(spacing: 12) {
            AlbumArtView(url: task.albumIcon, size: 48, cornerRadius: 8)

            VStack(alignment: .leading, spacing: 4) {
                Text(task.name)
                    .font(.subheadline)
                    .lineLimit(1)
                Text(task.tonalName)
                    .font(.caption2)
                    .foregroundStyle(.secondary)

                ProgressView(value: task.progress)
                    .tint(AppTheme.primary)

                Text(stateText)
                    .font(.caption2)
                    .foregroundStyle(stateColor)
            }

            Spacer()

            stateButton
        }
        .padding(.vertical, 4)
    }

    private var stateText: String {
        switch task.state {
        case .waiting: return "等待中..."
        case .running: return "下载中 \(Int(task.progress * 100))%"
        case .paused: return "已暂停"
        case .completed: return "已完成"
        case .failed: return "下载失败"
        case .retrying: return "重试中..."
        }
    }

    private var stateColor: Color {
        switch task.state {
        case .completed: return .green
        case .failed: return .red
        default: return .secondary
        }
    }

    @ViewBuilder
    private var stateButton: some View {
        switch task.state {
        case .completed:
            Button("打开") { openFile() }
                .buttonStyle(.bordered)
                .tint(AppTheme.primary)
        case .running:
            Button("暂停") { downloadManager.pause(task.id) }
                .buttonStyle(.bordered)
        case .paused, .failed:
            Button("继续") { downloadManager.resume(task.id) }
                .buttonStyle(.bordered)
                .tint(AppTheme.primary)
        default:
            EmptyView()
        }
    }

    private func openFile() {
        guard let url = task.localURL else { return }
        UIApplication.shared.open(url)
    }
}
