import Foundation

// MARK: - 侦查记录

enum ScanType: String, Codable, CaseIterable {
    case ip, phone, username

    var label: String {
        switch self {
        case .ip: "IP 定位"
        case .phone: "手机号"
        case .username: "用户名"
        }
    }
}

struct ScanRecord: Codable, Identifiable, Equatable {
    var id = UUID()
    var type: ScanType
    var value: String
    var summary: String
    var date: Date = .now
}

// MARK: - IP 定位结果（ipwho.is 字段映射）

struct IPInfo: Codable {
    struct Connection: Codable {
        var asn: Int?
        var org: String?
        var isp: String?
        var domain: String?
    }
    struct TZ: Codable {
        var id: String?
        var utc: String?
        var current_time: String?
    }
    var ip: String
    var success: Bool
    var type: String?
    var continent: String?
    var country: String?
    var country_code: String?
    var city: String?
    var postal: String?
    var latitude: Double?
    var longitude: Double?
    var flag: [String: String]?
    var connection: Connection?
    var timezone: TZ?

    var isReserved: Bool {
        guard let ip else { return false }
        return ip.hasPrefix("127.") || ip.hasPrefix("10.") || ip.hasPrefix("192.168.")
            || ip.hasPrefix("169.254.") || ip.hasPrefix("0.")
    }
}

// MARK: - 手机号解析结果

struct PhoneInfo {
    var e164: String
    var countryCode: Int
    var nationalDigits: String
    var regionName: String
    var valid: Bool
    /// 接入数据源后填充；当前暂未提供，本地解析不伪造
    var carrier: String?
    var timezone: String?

    var intlFormatted: String {
        let groups = stride(from: 0, to: nationalDigits.count, by: 3).map {
            String(nationalDigits.digits[$0..<min($0 + 3, nationalDigits.count)])
        }
        return "+\(countryCode) \(groups.joined(separator: " "))"
    }
}

private extension String {
    var digits: [Character] { Array(filter(\.isNumber)) }
}

// MARK: - 用户名平台

struct Platform: Identifiable {
    let name: String
    let template: String
    var id: String { name }
}

/// 平台清单 —— 以上游技术指标为准：
/// 上游 https://github.com/HunxByts/GhostTrack → GhostTR.py::TrackLu() 的 social_media 列表。
/// 上游 24 条中 Snapchat 重复出现两次，去重后为 23 个平台（与 DESIGN.md §7「23 格网格」一致）。
/// 详见仓库根目录 UPSTREAM.md。
enum Platforms {
    static let all: [Platform] = [
        .init(name: "Facebook", template: "https://www.facebook.com/%@"),
        .init(name: "Twitter", template: "https://www.twitter.com/%@"),
        .init(name: "Instagram", template: "https://www.instagram.com/%@"),
        .init(name: "LinkedIn", template: "https://www.linkedin.com/in/%@"),
        .init(name: "GitHub", template: "https://www.github.com/%@"),
        .init(name: "Pinterest", template: "https://www.pinterest.com/%@"),
        .init(name: "Tumblr", template: "https://www.tumblr.com/%@"),
        .init(name: "Youtube", template: "https://www.youtube.com/%@"),
        .init(name: "SoundCloud", template: "https://soundcloud.com/%@"),
        .init(name: "Snapchat", template: "https://www.snapchat.com/add/%@"),
        .init(name: "TikTok", template: "https://www.tiktok.com/@%@"),
        .init(name: "Behance", template: "https://www.behance.net/%@"),
        .init(name: "Medium", template: "https://www.medium.com/@%@"),
        .init(name: "Quora", template: "https://www.quora.com/profile/%@"),
        .init(name: "Flickr", template: "https://www.flickr.com/people/%@"),
        .init(name: "Periscope", template: "https://www.periscope.tv/%@"),
        .init(name: "Twitch", template: "https://www.twitch.tv/%@"),
        .init(name: "Dribbble", template: "https://www.dribbble.com/%@"),
        .init(name: "StumbleUpon", template: "https://www.stumbleupon.com/stumbler/%@"),
        .init(name: "Ello", template: "https://www.ello.co/%@"),
        .init(name: "Product Hunt", template: "https://www.producthunt.com/@%@"),
        .init(name: "Telegram", template: "https://www.telegram.me/%@"),
        .init(name: "We Heart It", template: "https://www.weheartit.com/%@"),
    ]
}

struct UsernameHit: Identifiable {
    let platform: Platform
    let found: Bool
    var id: String { platform.name }
}
