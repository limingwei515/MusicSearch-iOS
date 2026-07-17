import Foundation

/// HTTP 请求结果（对应 Android HttpResponse）
struct HttpResponse {
    let success: Bool
    let error: String?
    let data: String?

    static func success(_ data: String) -> HttpResponse { HttpResponse(success: true, error: nil, data: data) }
    static func error(_ error: String) -> HttpResponse { HttpResponse(success: false, error: error, data: nil) }
}

/// HTTP 请求工具（对应 Android HttpRequest）
/// 基于 URLSession，使用 async/await
enum HttpRequest {

    static private let session: URLSession = {
        let config = URLSessionConfiguration.default
        config.timeoutIntervalForRequest = 15
        config.timeoutIntervalForResource = 15
        config.requestCachePolicy = .reloadIgnoringLocalCacheData
        return URLSession(configuration: config)
    }()

    enum Method: String {
        case GET, POST, PUT, PATCH
    }

    /// 发起请求，返回原始响应字符串
    static func request(url: String, method: Method = .GET,
                        params: [String: String] = [:],
                        headers: [String: String] = [:]) async throws -> String {

        var finalURL = url
        var bodyData: Data? = nil

        if method == .GET && !params.isEmpty {
            var comp = URLComponents(string: url)!
            comp.queryItems = params.map { URLQueryItem(name: $0.key, value: $0.value) }
            finalURL = comp.url?.absoluteString ?? url
        } else if method != .GET {
            // 表单 POST
            let bodyString = params.map { "\($0.key)=\($0.value.addingPercentEncoding(withAllowedCharacters: .urlQueryAllowed) ?? "")" }
                .joined(separator: "&")
            bodyData = bodyString.data(using: .utf8)
        }

        guard let u = URL(string: finalURL) else {
            throw URLError(.badURL)
        }

        var req = URLRequest(url: u)
        req.httpMethod = method.rawValue
        if let bodyData {
            req.setValue("application/x-www-form-urlencoded", forHTTPHeaderField: "Content-Type")
            req.httpBody = bodyData
        }
        // 酷我接口需要的 User-Agent
        if req.value(forHTTPHeaderField: "User-Agent") == nil {
            req.setValue("Mozilla/5.0 (Linux; Android 12) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/107.0.5304.141 Mobile Safari/537.36",
                         forHTTPHeaderField: "User-Agent")
        }
        for (k, v) in headers {
            req.setValue(v, forHTTPHeaderField: k)
        }

        let (data, response) = try await session.data(for: req)
        guard let http = response as? HTTPURLResponse else {
            throw URLError(.badServerResponse)
        }
        guard (200...299).contains(http.statusCode) else {
            throw NSError(domain: "HttpRequest", code: http.statusCode,
                          userInfo: [NSLocalizedDescriptionKey: "HTTP \(http.statusCode)"])
        }
        return String(data: data, encoding: .utf8) ?? ""
    }

    /// 带 Cookie 的请求（部分酷我接口需要）
    static func requestWithCookie(url: String, method: Method = .GET,
                                 params: [String: String] = [:],
                                 cookie: String? = nil) async throws -> String {
        var headers: [String: String] = [:]
        if let cookie { headers["Cookie"] = cookie }
        return try await request(url: url, method: method, params: params, headers: headers)
    }
}
