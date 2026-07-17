import Foundation
import Combine

/// 下载任务模型
struct DownloadTask: Identifiable, Hashable {
    var id: Int
    var name: String
    var filePath: String
    var albumIcon: String
    var tonalName: String
    var state: DownloadState
    var progress: Double
    var totalBytes: Int64
    var downloadedBytes: Int64
    var url: String
    var localURL: URL?
}

enum DownloadState: Int {
    case waiting = 0
    case running = 1
    case paused = 2
    case completed = 3
    case failed = 4
    case retrying = 5
}

/// 下载管理器（对应 Android Aria 下载功能）
final class DownloadManager: ObservableObject {

    static let shared = DownloadManager()

    @Published private(set) var tasks: [DownloadTask] = []

    private var session: URLSession!
    private var taskMap: [Int: URLSessionDownloadTask] = [:]
    private var delegates: [Int: DownloadDelegate] = [:]
    private var nextId = 1

    private init() {
        let config = URLSessionConfiguration.background(withIdentifier: "com.linfeng.music.download")
        config.allowsCellularAccess = true
        config.sessionSendsLaunchEvents = true
        config.isDiscretionary = false
        let delegate = BackgroundSessionDelegate.shared
        session = URLSession(configuration: config, delegate: delegate, delegateQueue: nil)
        BackgroundSessionDelegate.shared.owner = self
    }

    func addDownload(url: String, filePath: String, albumIcon: String, tonalName: String) {
        guard let u = URL(string: url) else { return }
        let id = nextId
        nextId += 1
        let name = (filePath as NSString).lastPathComponent
        var task = DownloadTask(id: id, name: name, filePath: filePath,
                                albumIcon: albumIcon, tonalName: tonalName,
                                state: .waiting, progress: 0,
                                totalBytes: 0, downloadedBytes: 0,
                                url: url, localURL: nil)
        let dtask = session.downloadTask(with: u)
        taskMap[id] = dtask
        let d = DownloadDelegate(taskId: id)
        delegates[id] = d
        tasks.insert(task, at: 0)
        dtask.resume()
        updateState(id, state: .running)
    }

    func pause(_ id: Int) {
        taskMap[id]?.suspend()
        updateState(id, state: .paused)
    }

    func resume(_ id: Int) {
        taskMap[id]?.resume()
        updateState(id, state: .running)
    }

    func cancel(_ id: Int) {
        taskMap[id]?.cancel()
        taskMap.removeValue(forKey: id)
        delegates.removeValue(forKey: id)
        tasks.removeAll { $0.id == id }
    }

    func removeAll() {
        for t in tasks { taskMap[t.id]?.cancel() }
        taskMap.removeAll()
        delegates.removeAll()
        tasks.removeAll()
    }

    func updateProgress(_ id: Int, downloaded: Int64, total: Int64) {
        guard let idx = tasks.firstIndex(where: { $0.id == id }) else { return }
        let p = total > 0 ? Double(downloaded) / Double(total) : 0
        tasks[idx].downloadedBytes = downloaded
        tasks[idx].totalBytes = total
        tasks[idx].progress = p
    }

    func complete(_ id: Int, tempURL: URL) {
        guard let idx = tasks.firstIndex(where: { $0.id == id }) else { return }
        let path = tasks[idx].filePath
        let destURL = URL(fileURLWithPath: path)
        try? FileManager.default.removeItem(at: destURL)
        try? FileManager.default.moveItem(at: tempURL, to: destURL)
        tasks[idx].state = .completed
        tasks[idx].progress = 1.0
        tasks[idx].localURL = destURL
        // 写入 MP3 标签封面（简化版：仅记录，实际写入需额外库）
    }

    func updateState(_ id: Int, state: DownloadState) {
        guard let idx = tasks.firstIndex(where: { $0.id == id }) else { return }
        tasks[idx].state = state
    }
}

/// 单个下载任务的代理
final class DownloadDelegate: NSObject, URLSessionDownloadDelegate {
    let taskId: Int
    init(taskId: Int) { self.taskId = taskId }

    func urlSession(_ session: URLSession, downloadTask: URLSessionDownloadTask,
                    didFinishDownloadingTo location: URL) {
        DownloadManager.shared.complete(taskId, tempURL: location)
    }

    func urlSession(_ session: URLSession, downloadTask: URLSessionDownloadTask,
                    didWriteData bytesWritten: Int64, totalBytesWritten: Int64,
                    totalBytesExpectedToWrite: Int64) {
        DownloadManager.shared.updateProgress(taskId,
            downloaded: totalBytesWritten, total: totalBytesExpectedToWrite)
    }
}

/// 后台会话共享代理
final class BackgroundSessionDelegate: NSObject, URLSessionDownloadDelegate {
    static let shared = BackgroundSessionDelegate()
    weak var owner: DownloadManager?

    func urlSession(_ session: URLSession, downloadTask: URLSessionDownloadTask,
                    didFinishDownloadingTo location: URL) {
        if let id = owner?.taskMap.first(where: { $0.value == downloadTask })?.key {
            owner?.complete(id, tempURL: location)
        }
    }

    func urlSession(_ session: URLSession, downloadTask: URLSessionDownloadTask,
                    didWriteData bytesWritten: Int64, totalBytesWritten: Int64,
                    totalBytesExpectedToWrite: Int64) {
        if let id = owner?.taskMap.first(where: { $0.value == downloadTask })?.key {
            owner?.updateProgress(id, downloaded: totalBytesWritten, total: totalBytesExpectedToWrite)
        }
    }
}
