import SwiftUI

/// 关于页面（对应 Android AboutActivity）
struct AboutView: View {
    var body: some View {
        ScrollView {
            VStack(spacing: 24) {
                // 应用图标
                Image(systemName: "music.note")
                    .font(.system(size: 56))
                    .foregroundStyle(.white)
                    .frame(width: 100, height: 100)
                    .background(LinearGradient(colors: [AppTheme.primary, .purple],
                                              startPoint: .topLeading, endPoint: .bottomTrailing))
                    .clipShape(RoundedRectangle(cornerRadius: 24))

                Text("音乐搜索")
                    .font(.title.bold())

                Text("版本 \(CommonUtils.versionName) (Build \(CommonUtils.versionCode))")
                    .font(.caption)
                    .foregroundStyle(.secondary)

                VStack(spacing: 16) {
                    infoCard(icon: "sparkles", title: "Liquid Glass",
                             desc: "基于 iOS 26 液态玻璃设计语言打造")
                    infoCard(icon: "music.note", title: "在线音乐",
                             desc: "支持在线搜索、播放、下载酷我音乐")
                    infoCard(icon: "heart.fill", title: "本地收藏",
                             desc: "收藏喜欢的歌曲，支持离线播放")
                }
                .padding(.top, 16)

                Spacer()

                Text("本应用仅供学习交流使用\n所有音乐版权归原作者所有")
                    .font(.caption2)
                    .foregroundStyle(.tertiary)
                    .multilineTextAlignment(.center)
                    .padding()
            }
            .padding()
        }
        .navigationTitle("关于")
        .navigationBarTitleDisplayMode(.inline)
    }

    private func infoCard(icon: String, title: String, desc: String) -> some View {
        HStack(spacing: 16) {
            Image(systemName: icon)
                .font(.title2)
                .foregroundStyle(AppTheme.primary)
                .frame(width: 40)
            VStack(alignment: .leading, spacing: 4) {
                Text(title).font(.headline)
                Text(desc).font(.caption).foregroundStyle(.secondary)
            }
            Spacer()
        }
        .padding()
        .glassBackground(cornerRadius: 16)
    }
}

/// 更新日志页面（对应 Android UpdateLogActivity）
struct UpdateLogView: View {
    @State private var logs: [UpdateLogItem] = []

    var body: some View {
        ScrollView {
            LazyVStack(spacing: 16) {
                ForEach(logs) { item in
                    VStack(alignment: .leading, spacing: 8) {
                        HStack {
                            Text("版本 v\(item.version ?? "")")
                                .font(.headline)
                            Spacer()
                            Text("更新日期：\(item.time ?? "")")
                                .font(.caption2)
                                .foregroundStyle(.secondary)
                        }
                        Text(item.content ?? "")
                            .font(.subheadline)
                            .foregroundStyle(.secondary)
                    }
                    .padding()
                    .glassBackground(cornerRadius: 16)
                }
            }
            .padding()
        }
        .navigationTitle("更新日志")
        .navigationBarTitleDisplayMode(.inline)
        .onAppear { loadLogs() }
    }

    private func loadLogs() {
        // 从 Bundle 读取 updateLog.json
        if let json = CommonUtils.getFileContent(fromBundle: "updateLog", ext: "json"),
           let data = json.data(using: .utf8),
           let list = try? JSONDecoder().decode([UpdateLogItem].self, from: data) {
            logs = list.reversed()
        } else {
            // 兜底默认日志
            logs = [
                UpdateLogItem(version: "1.0.0", time: "2026-07-16",
                              content: "iOS 版本首发，适配 Liquid Glass 设计语言\n支持在线搜索、播放、下载、收藏、歌词"),
            ]
        }
    }
}
