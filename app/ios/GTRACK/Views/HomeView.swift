import SwiftUI

// MARK: - 侦查台（首页）

struct HomeView: View {
    @Environment(AppState.self) private var appState
    @Environment(HistoryStore.self) private var history
    @State private var myIP = "查询中…"

    var body: some View {
        @Bindable var nav = NavHub.shared
        NavigationStack(path: $nav.path) {
            ScrollView {
                VStack(alignment: .leading, spacing: 26) {
                    // 本机 IP 卡
                    HStack {
                        Image(systemName: "wifi").foregroundStyle(.blue)
                        VStack(alignment: .leading, spacing: 1) {
                            Text("本机 IP").font(.gtSub).foregroundStyle(.secondary)
                            MonoText(text: myIP).font(.gtHeadline)
                        }
                        Spacer()
                        Button {
                            UIImpactFeedbackGenerator.tap()
                            myIP = "查询中…"
                            Task { myIP = (try? await ScanService().trackMyIP()) ?? "获取失败" }
                        } label: { Image(systemName: "arrow.clockwise") }
                    }
                    .padding(16).frame(maxWidth: .infinity, alignment: .leading)
                    .gtGlass(corner: 20)

                    // 侦查入口（垂直列表，不做均分网格）
                    VStack(spacing: 10) {
                        entry("dot.radiowaves.left.and.right", "IP 定位", "查询归属地、ASN 与运营商网络", .ip) { IPTrackView() }
                        entry("iphone.gen3", "手机号追踪", "解析号码归属地与运营商", .phone) { PhoneTrackView() }
                        entry("person.crop.circle.badge.magnifyingglass", "用户名扫描", "一次核查 \(Platforms.all.count) 个社交平台的同名账号", .username) { UsernameScanView() }
                    }

                    if !history.records.isEmpty {
                        VStack(alignment: .leading, spacing: 10) {
                            Text("最近侦查").font(.gtFoot).foregroundStyle(.secondary).padding(.leading, 4)
                            ForEach(history.records.prefix(3)) { rec in
                                NavigationLink(value: rec) {
                                    historyRow(rec)
                                }
                                .buttonStyle(.plain)
                            }
                        }
                    }

                    Text("仅供合法授权调查 · 记录仅存本机")
                        .font(.caption2).foregroundStyle(.tertiary)
                        .frame(maxWidth: .infinity)
                        .padding(.top, 8)
                }
                .padding(.horizontal, 16)
            }
            .navigationTitle("侦查台")
            .navigationDestination(for: ScanRecord.self) { rec in
                RecallView(record: rec) // 记录回查路由
            }
            .task { if myIP == "查询中…" { myIP = (try? await ScanService().trackMyIP()) ?? "获取失败" } }
        }
    }

    private func entry(_ icon: String, _ title: String, _ sub: String, _ type: ScanType, @ViewBuilder dest: @escaping () -> some View) -> some View {
        NavigationLink {
            dest()
        } label: {
            HStack(spacing: 14) {
                Image(systemName: icon)
                    .font(.system(size: 17, weight: .medium))
                    .foregroundStyle(.gtGreen)
                    .frame(width: 40, height: 40)
                    .background(.gtGreen.opacity(0.14), in: .rect(cornerRadius: 12))
                VStack(alignment: .leading, spacing: 2) {
                    Text(title).font(.gtHeadline).foregroundStyle(.primary)
                    Text(sub).font(.caption).foregroundStyle(.secondary)
                }
                Spacer()
                Image(systemName: "chevron.right").font(.caption).foregroundStyle(.tertiary)
            }
            .padding(14)
            .gtGlass(corner: 20)
        }
        .buttonStyle(.plain)
    }

    private func historyRow(_ rec: ScanRecord) -> some View {
        HStack(spacing: 12) {
            Image(systemName: iconFor(rec.type)).foregroundStyle(.gtGreen)
                .frame(width: 30, height: 30)
                .background(.fill, in: .rect(cornerRadius: 8))
            VStack(alignment: .leading, spacing: 1) {
                Text(rec.value).font(.system(.footnote, design: .monospaced))
                Text("\(rec.type.label) · \(rec.date.formatted(.relative(presentation: .named)))").font(.caption2).foregroundStyle(.secondary)
            }
            Spacer()
            Image(systemName: "chevron.right").font(.caption2).foregroundStyle(.tertiary)
        }
        .padding(12).gtGlass(corner: 16)
    }

    private func iconFor(_ t: ScanType) -> String {
        switch t {
        case .ip: "dot.radiowaves.left.and.right"
        case .phone: "iphone.gen3"
        case .username: "person.crop.circle"
        }
    }
}

/// 记录回查：按类型打开对应页并回填
struct RecallView: View {
    let record: ScanRecord
    var body: some View {
        switch record.type {
        case .ip: IPTrackView(prefill: record.value)
        case .phone: PhoneTrackView(prefill: record.value)
        case .username: UsernameScanView(prefill: record.value)
        }
    }
}

/// 全局导航栈路径（记录回查与 push 复用）
@Observable
final class NavHub {
    static let shared = NavHub()
    var path: [ScanRecord] = []
}
