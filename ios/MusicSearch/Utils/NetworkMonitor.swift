import Foundation
import Network
import Combine

/// 网络监控（检测蜂窝网络以决定是否降质）
final class NetworkMonitor: ObservableObject {
    static let shared = NetworkMonitor()

    @Published var isCellular: Bool = false
    @Published var isConnected: Bool = false

    private let monitor: NWPathMonitor
    private let queue = DispatchQueue(label: "com.linfeng.music.netmonitor")

    private init() {
        monitor = NWPathMonitor()
        monitor.pathUpdateHandler = { [weak self] path in
            DispatchQueue.main.async {
                self?.isConnected = path.status == .satisfied
                self?.isCellular = path.usesInterfaceType(.cellular)
            }
        }
        monitor.start(queue: queue)
    }
}
