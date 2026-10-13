import SwiftUI

// MARK: - 手机号追踪

struct PhoneTrackView: View {
    var prefill: String?
    @State private var input = ""
    @State private var error: String?
    @State private var scanning = false
    @State private var result: PhoneInfo?
    @State private var progress = 0.0
    @State private var toast: String?
    private let service = ScanService()
    @Environment(HistoryStore.self) private var history
    @Environment(\.openURL) private var openURL

    var body: some View {
        ScanScaffold(
            title: "手机号追踪", placeholder: "415 555 0132",
            input: $input, error: $error, scanning: $scanning, progress: $progress,
            canSubmit: input.filter(\.isNumber).count >= 8, resultShown: result != nil,
            submitTitle: "开始追踪", scanningTitle: "正在解析",
            toast: $toast,
            onSubmit: { run() }
        ) {
            if let r = result { PhoneResultCard(info: r) }
        }
        .onAppear { if let prefill, input.isEmpty { input = prefill; run() } }
    }

    private func run() {
        let raw = input
        error = nil; result = nil; scanning = true; progress = 0
        UIImpactFeedbackGenerator.tap()
        Task {
            withAnimation { progress = 0.4 }
            do {
                let info = try service.parsePhone(raw)
                try await Task.sleep(for: .seconds(0.7))
                withAnimation { progress = 1 }
                result = info
                if info.valid {
                    history.add(.init(type: .phone, value: info.e164, summary: info.regionName))
                }
            } catch {
                withAnimation { self.error = error.localizedDescription }
                UINotificationFeedbackGenerator().notificationOccurred(.error)
            }
            scanning = false
        }
    }
}

struct PhoneResultCard: View {
    let info: PhoneInfo

    var body: some View {
        VStack(alignment: .leading, spacing: 14) {
            HStack(spacing: 12) {
                Text(String(info.countryCode))
                    .font(.gtHeadline.monospaced()).frame(width: 52, height: 52)
                    .background(.orange.opacity(0.18), in: .rect(cornerRadius: 14))
                VStack(alignment: .leading, spacing: 3) {
                    MonoText(text: info.e164).font(.gtTitle)
                    HStack(spacing: 6) {
                        Label(info.valid ? "号码有效" : "格式异常",
                              systemImage: info.valid ? "checkmark.seal.fill" : "exclamationmark.triangle.fill")
                            .font(.caption2)
                            .foregroundStyle(info.valid ? .gtGreen : .orange)
                        Text("本地解析").font(.caption2).foregroundStyle(.tertiary)
                    }
                }
                Spacer()
            }

            KVGroup(rows: [
                ("归属区域", info.regionName),
                ("国家码", "+\(info.countryCode)"),
                ("线路类型", "当前数据源暂未提供"),
            ])

            KVGroup(mono: true, rows: [
                ("E.164", info.e164),
                ("国际格式", info.intlFormatted),
            ])
        }
    }
}

// MARK: - 用户名扫描

struct UsernameScanView: View {
    var prefill: String?
    @State private var input = ""
    @State private var error: String?
    @State private var scanning = false
    @State private var hits: [UsernameHit] = []
    @State private var done = 0
    @State private var progress = 0.0
    @State private var toast: String?
    private let service = ScanService()
    @Environment(HistoryStore.self) private var history

    private var foundCount: Int { hits.filter(\.found).count }

    var body: some View {
        ScanScaffold(
            title: "用户名扫描", placeholder: "username",
            input: $input, error: $error, scanning: $scanning, progress: $progress,
            canSubmit: input.range(of: #"^[\w.\-]{2,30}$"#, options: .regularExpression) != nil,
            resultShown: !hits.isEmpty,
            submitTitle: "开始扫描", scanningTitle: "已扫描 \(done)/\(Platforms.all.count) 个平台",
            toast: $toast,
            onSubmit: { run() }
        ) {
            VStack(alignment: .leading, spacing: 10) {
                HStack {
                    Text("\(input) · \(foundCount) 个平台命中").font(.gtHeadline)
                    Spacer()
                    Text("实时").font(.caption2).foregroundStyle(.tertiary)
                }
                LazyVGrid(columns: [GridItem(.adaptive(minimum: 100), spacing: 8)], spacing: 8) {
                    ForEach(hits) { hit in
                        PlatformCell(hit: hit, username: input) { toast = "演示环境不跳转外链" }
                    }
                }
            }
        }
        .onAppear { if let prefill, input.isEmpty { input = prefill; run() } }
    }

    private func run() {
        let name = input.trimmingCharacters(in: .whitespaces)
        error = nil; hits = []; done = 0; scanning = true; progress = 0
        UIImpactFeedbackGenerator.tap()
        Task {
            let results = await service.scanUsername(name)
            // 按探测完成顺序逐格点亮
            for (i, _) in results.enumerated() {
                try? await Task.sleep(for: .seconds(0.12))
                hits = Array(results.prefix(i + 1))
                done = i + 1
                withAnimation { progress = Double(i + 1) / Double(results.count) }
            }
            scanning = false
            if foundCount > 0 {
                history.add(.init(type: .username, value: name, summary: "\(foundCount) 个平台命中"))
            }
        }
    }
}

struct PlatformCell: View {
    let hit: UsernameHit
    let username: String
    let onTapLimited: () -> Void

    var body: some View {
        VStack(spacing: 5) {
            Image(systemName: hit.found ? "checkmark.circle.fill" : "minus.circle")
                .foregroundStyle(hit.found ? .gtGreen : .tertiary)
            Text(hit.platform.name).font(.caption2).lineLimit(1)
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 12)
        .background(hit.found ? Color.gtGreen.opacity(0.14) : Color.secondary.opacity(0.06),
                    in: .rect(cornerRadius: 12))
        .overlay(.rect(cornerRadius: 12).strokeBorder(hit.found ? .gtGreen.opacity(0.4) : .clear))
        .opacity(hit.found ? 1 : 0.55)
    }
}
