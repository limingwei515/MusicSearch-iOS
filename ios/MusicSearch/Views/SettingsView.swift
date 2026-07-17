import SwiftUI
import PhotosUI

/// 设置页面（对应 Android SettingsActivity）
struct SettingsView: View {
    @StateObject private var prefs = PreferencesManager.shared
    @State private var playQuality = "lossless"
    @State private var cellularQuality = "standard"
    @State private var cellularAutoLower = true
    @State private var lyricRetry = 3

    var body: some View {
        NavigationStack {
            Form {
                Section("播放音质（WiFi）") {
                    qualityPicker(selection: $playQuality, key: .playQuality)
                }

                Section("蜂窝网络") {
                    Toggle("自动降低音质", isOn: $cellularAutoLower)
                        .onChange(of: cellularAutoLower) { v in prefs.setBool(v, for: .cellularAutoLower) }

                    if cellularAutoLower {
                        qualityPicker(selection: $cellularQuality, key: .cellularPlayQuality)
                    }
                }

                Section("下载路径") {
                    Text(downloadPathDisplay)
                        .font(.caption)
                        .foregroundStyle(.secondary)
                }

                Section("歌词重试次数") {
                    Picker("重试次数", selection: $lyricRetry) {
                        Text("1 次").tag(1)
                        Text("3 次").tag(3)
                        Text("5 次").tag(5)
                        Text("10 次").tag(10)
                    }
                    .onChange(of: lyricRetry) { v in prefs.setInt(v, for: .lyricRetryCount) }
                }

                Section("关于") {
                    NavigationLink("关于应用") { AboutView() }
                    NavigationLink("更新日志") { UpdateLogView() }
                }

                Section {
                    Text("音乐搜索 iOS 版 · Liquid Glass")
                        .font(.caption2)
                        .foregroundStyle(.tertiary)
                        .frame(maxWidth: .infinity, alignment: .center)
                }
            }
            .navigationTitle("设置")
            .navigationBarTitleDisplayMode(.large)
        }
        .onAppear { loadSettings() }
    }

    private var downloadPathDisplay: String {
        let path = prefs.getString(.downloadPath, default: "")
        return path.isEmpty ? "Documents/音频（默认）" : path
    }

    private func loadSettings() {
        playQuality = prefs.getString(.playQuality, default: "lossless")
        cellularQuality = prefs.getString(.cellularPlayQuality, default: "standard")
        cellularAutoLower = prefs.getBool(.cellularAutoLower, default: true)
        lyricRetry = prefs.getInt(.lyricRetryCount, default: 3)
    }

    @ViewBuilder
    private func qualityPicker(selection: Binding<String>, key: PreferencesManager.Key) -> some View {
        Picker("音质", selection: selection) {
            Text("无损 Flac").tag("lossless")
            Text("高品 MP3").tag("high")
            Text("标准 MP3").tag("standard")
            Text("流畅 AAC").tag("fluent")
        }
        .onChange(of: selection.wrappedValue) { v in prefs.setString(v, for: key) }
        .pickerStyle(.segmented)
    }
}
