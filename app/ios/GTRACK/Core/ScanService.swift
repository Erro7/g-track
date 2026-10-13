import Foundation

/// 三类侦查的数据访问（DESIGN.md §9）。全部只访问公开数据源。
struct ScanService {
    // MARK: - IP 定位（ipwho.is 公共接口）

    func trackIP(_ raw: String) async throws -> IPInfo {
        let ip = raw.trimmingCharacters(in: .whitespaces)
        guard ip.range(of: #"^(\d{1,3}\.){3}\d{1,3}$"#, options: .regularExpression) != nil,
              ip.split(separator: ".").allSatisfy({ (0...255).contains(Int($0) ?? 256) })
        else { throw ScanError.invalidIP }
        if isReservedIP(ip) { throw ScanError.reservedIP }

        guard let url = URL(string: "https://ipwho.is/\(ip)") else { throw ScanError.invalidIP }
        let (data, _) = try await URLSession.shared.data(from: url)
        let info = try JSONDecoder().decode(IPInfo.self, from: data)
        guard info.success else { throw ScanError.lookupFailed(info.message ?? "查询失败") }
        return info
    }

    private func isReservedIP(_ ip: String) -> Bool {
        ip.hasPrefix("127.") || ip.hasPrefix("10.") || ip.hasPrefix("192.168.")
            || ip.hasPrefix("172.16.") || ip.hasPrefix("169.254.") || ip.hasPrefix("0.")
    }

    /// 本机 IP（ipwho.is 不带参数即查本机）
    func trackMyIP() async throws -> String {
        let (data, _) = try await URLSession.shared.data(from: URL(string: "https://ipwho.is/")!)
        let info = try JSONDecoder().decode(IPInfo.self, from: data)
        return info.success ? info.ip : "获取失败"
    }

    // MARK: - 手机号（本地解析；运营商字段当前数据源暂未提供）

    static let regionCodes: [(code: Int, name: String, minDigits: Int, maxDigits: Int)] = [
        (1, "美国/加拿大", 10, 10), (86, "中国", 11, 11), (44, "英国", 10, 10),
        (81, "日本", 10, 10), (49, "德国", 10, 11), (33, "法国", 9, 9),
        (91, "印度", 10, 10), (61, "澳大利亚", 9, 9), (55, "巴西", 10, 11),
        (7, "俄罗斯", 10, 10), (82, "韩国", 9, 10), (39, "意大利", 9, 10),
        (34, "西班牙", 9, 9), (66, "泰国", 9, 9), (62, "印度尼西亚", 9, 12),
    ]

    func parsePhone(_ raw: String) throws -> PhoneInfo {
        let digits = raw.filter(\.isNumber)
        guard digits.count >= 8 else { throw ScanError.invalidPhone }

        // 匹配国家码（优先长码）
        let match = Self.regionCodes
            .filter { digits.hasPrefix(String($0.code)) }
            .max { $0.code.description.count < $1.code.description.count }

        if let m = match {
            let national = String(digits.dropFirst(String(m.code).count))
            let valid = national.count >= m.minDigits && national.count <= m.maxDigits
            return PhoneInfo(
                e164: "+\(m.code)\(national)", countryCode: m.code, nationalDigits: national,
                regionName: m.name, valid: valid, carrier: nil, timezone: nil
            )
        }
        // 无匹配国家码：按原样校验长度
        guard (8...15).contains(digits.count) else { throw ScanError.invalidPhone }
        return PhoneInfo(
            e164: "+\(digits)", countryCode: 0, nationalDigits: digits,
            regionName: "未知区码", valid: false, carrier: nil, timezone: nil
        )
    }

    // MARK: - 用户名全平台扫描（并发 HEAD 探测公开主页）

    func scanUsername(_ username: String) async -> [UsernameHit] {
        let pattern = #"^[\w.\-]{2,30}$"#
        guard username.range(of: pattern, options: .regularExpression) != nil else { return [] }

        await withTaskGroup(of: UsernameHit.self) { group in
            for platform in Platforms.all {
                let url = URL(string: String(format: platform.template, username))
                group.addTask {
                    var found = false
                    if let url, var req = URLRequest(url: url) {
                        req.httpMethod = "HEAD"
                        req.timeoutInterval = 6
                        req.setValue("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7)", forHTTPHeaderField: "User-Agent")
                        if let (_, resp) = try? await URLSession.shared.data(for: req) {
                            let code = (resp as? HTTPURLResponse)?.statusCode ?? 0
                            found = (200..<300).contains(code) && code != 999 // LinkedIn 999 = 拦截
                        }
                    }
                    return UsernameHit(platform: platform, found: found)
                }
            }
            var hits: [UsernameHit] = []
            for await hit in group { hits.append(hit) }
            return hits.sorted { $0.platform.name < $1.platform.name }
        }
    }
}

// MARK: - 错误（均有恢复路径，DESIGN.md §6）

enum ScanError: LocalizedError {
    case invalidIP, reservedIP, invalidPhone, lookupFailed(String)

    var errorDescription: String? {
        switch self {
        case .invalidIP: "IP 格式不正确，示例：1.1.1.1"
        case .reservedIP: "保留地址不路由公网，请输入公网 IP 重试"
        case .invalidPhone: "号码不完整，示例：415 555 0132"
        case .lookupFailed(let why): why
        }
    }
}
