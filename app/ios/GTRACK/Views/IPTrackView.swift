import SwiftUI

// MARK: - IP 定位

struct IPTrackView: View {
    var prefill: String?
    @State private var input = ""
    @State private var error: String?
    @State private var scanning = false
    @State private var result: IPInfo?
    @State private var progress = 0.0
    @State private var toast: String?
    private let service = ScanService()
    @Environment(HistoryStore.self) private var history

    var body: some View {
        ScanScaffold(
            title: "IP 定位", placeholder: "1.1.1.1",
            input: $input, error: $error, scanning: $scanning, progress: $progress,
            canSubmit: Self.isValidIP(input), resultShown: result != nil,
            submitTitle: "开始追踪", scanningTitle: "正在追踪",
            toast: $toast,
            onSubmit: { run() }
        ) {
            if let r = result { IPResultCard(info: r) { toast = "已在记录中保留" } }
        }
        .onAppear { if let prefill, input.isEmpty { input = prefill; run() } }
    }

    private func run() {
        error = nil
        scanning = true
        progress = 0
        let target = input.trimmingCharacters(in: .whitespaces)
        Task {
            // 进度环动画与真实请求并行
            let ticker = Task {
                while !Task.isCancelled {
                    withAnimation(.easeOut(duration: 0.3)) { progress = min(progress + 0.08, 0.92) }
                    try? await Task.sleep(for: .seconds(0.3))
                }
            }
            do {
                let info = try await service.trackIP(target)
                ticker.cancel()
                withAnimation { progress = 1; result = info; scanning = false }
                history.add(ScanRecord(type: .ip, value: info.ip ?? target, summary: [info.country, info.city].compactMap(\.self).joined(separator: " · ")))
            } catch {
                ticker.cancel()
                withAnimation { scanning = false }
                self.error = (error as? ScanError)?.errorDescription ?? "查询失败，请检查网络后重试"
            }
        }
    }

    static func isValidIP(_ s: String) -> Bool {
        guard s.range(of: #"^(\d{1,3}\.){3}\d{1,3}$"#, options: .regularExpression) != nil else { return false }
        return s.split(separator: ".").allSatisfy { (0...255).contains(Int($0) ?? 256) }
    }
}

struct IPResultCard: View {
    let info: IPInfo
    var onSaved: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 14) {
            // 地图卡（上线版接入 MapKit；坐标已可直接反查）
            ZStack {
                RoundedRectangle(cornerRadius: 18).fill(.gtGreen.opacity(0.10))
                VStack(spacing: 6) {
                    Image(systemName: "mappin.and.ellipse").font(.title2).foregroundStyle(.gtGreen)
                    MonoText(text: String(format: "%.4f, %.4f", info.latitude ?? 0, info.longitude ?? 0))
                        .font(.gtFoot).foregroundStyle(.secondary)
                }
            }
            .frame(height: 120).frame(maxWidth: .infinity)

            KVGroup("位置信息", rows: [
                ("国家", [info.country, info.country_code.flatMap { "(\($0))" }].compactMap(\.self).joined(separator: " ")),
                ("城市", info.city ?? "—"),
                ("大洲", info.continent ?? "—"),
                ("邮编", info.postal ?? "—"),
                ("时区", info.timezone?.id ?? "—"),
            ])
            KVGroup("网络信息", mono: true, rows: [
                ("ASN", info.connection?.asn.map { "AS\(String($0))" } ?? "—"),
                ("ISP", info.connection?.isp ?? "—"),
                ("ORG", info.connection?.org ?? "—"),
                ("域名", info.connection?.domain ?? "—"),
            ])
            HStack(spacing: 10) {
                Link(destination: mapURL) {
                    Label("在地图中打开", systemImage: "arrow.up.right.square").frame(maxWidth: .infinity)
                }
                .buttonStyle(.bordered)
                Button { UIPasteboard.general.string = String(format: "%.4f, %.4f", info.latitude ?? 0, info.longitude ?? 0); UIImpactFeedbackGenerator.tap(); onSaved() } label: {
                    Label("复制坐标", systemImage: "doc.on.doc").frame(maxWidth: .infinity)
                }
                .buttonStyle(.bordered)
            }
            .font(.gtSub)
        }
        .padding(.top, 18)
    }

    private var mapURL: URL {
        URL(string: "https://maps.apple.com/?ll=\(info.latitude ?? 0),\(info.longitude ?? 0)")!
    }
}

// MARK: - 键值分组

struct KVGroup: View {
    let title: String
    var mono = false
    let rows: [(String, String)]

    init(_ title: String, mono: Bool = false, rows: [(String, String)]) {
        self.title = title; self.mono = mono; self.rows = rows
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            Text(title).font(.gtFoot).foregroundStyle(.secondary).padding(.leading, 4)
            VStack(spacing: 0) {
                ForEach(rows.indices, id: \.self) { i in
                    HStack {
                        Text(rows[i].0).font(.gtSub).foregroundStyle(.secondary)
                        Spacer()
                        Text(rows[i].1)
                            .font(mono ? .system(.footnote, design: .monospaced) : .gtSub)
                            .foregroundStyle(.primary)
                            .lineLimit(1).truncationMode(.middle)
                    }
                    .padding(.horizontal, 14).padding(.vertical, 11)
                    if i < rows.count - 1 { Divider().padding(.leading, 14) }
                }
            }
            .gtGlass(corner: 18)
        }
    }
}
