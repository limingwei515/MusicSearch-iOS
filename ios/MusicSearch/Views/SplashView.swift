import SwiftUI

/// 启动页（对应 Android MainActivity）
struct SplashView: View {
    var onFinish: () -> Void
    @State private var countdown = 3
    @State private var progress: Double = 0

    var body: some View {
        ZStack {
            // 背景渐变
            LinearGradient(colors: [AppTheme.primary, AppTheme.primary.opacity(0.6)],
                          startPoint: .topLeading, endPoint: .bottomTrailing)
                .ignoresSafeArea()

            VStack(spacing: 24) {
                Spacer()

                // 应用图标
                Image(systemName: "music.note")
                    .font(.system(size: 72, weight: .light))
                    .foregroundStyle(.white)
                    .glassBackground(cornerRadius: 32)
                    .frame(width: 120, height: 120)
                    .overlay(
                        RoundedRectangle(cornerRadius: 32)
                            .stroke(.white.opacity(0.3), lineWidth: 1)
                    )

                Text("音乐搜索")
                    .font(.largeTitle.weight(.bold))
                    .foregroundStyle(.white)

                Text("Liquid Glass · iOS 26")
                    .font(.subheadline)
                    .foregroundStyle(.white.opacity(0.8))

                Spacer()

                // 倒计时跳过
                Button {
                    onFinish()
                } label: {
                    HStack(spacing: 8) {
                        Circle()
                            .trim(from: 0, to: progress)
                            .stroke(.white.opacity(0.6), lineWidth: 2)
                            .rotationEffect(.degrees(-90))
                            .frame(width: 24, height: 24)
                        Text("跳过 \(countdown)")
                            .font(.caption)
                    }
                    .glassBackground(cornerRadius: 20)
                    .padding(.horizontal, 16)
                    .padding(.vertical, 8)
                }
                .tint(.white)
                .padding(.bottom, 40)
            }
        }
        .onAppear {
            startCountdown()
        }
    }

    private func startCountdown() {
        let total = 3
        Timer.scheduledTimer(withTimeInterval: 1.0, repeats: true) { timer in
            countdown -= 1
            progress = Double(total - countdown) / Double(total)
            if countdown <= 0 {
                timer.invalidate()
                onFinish()
            }
        }
    }
}
