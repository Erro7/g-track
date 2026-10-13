import SwiftUI

// MARK: - 记录

struct HistoryView: View {
    @Environment(HistoryStore.self) private var history

    var body: some View {
        NavigationStack {
            Group {
                if history.records.isEmpty {
                    ContentUnavailableView {
                        Label("暂无侦查记录", systemImage: "clock.arrow.circlepath")
                    } description: {
                        Text("在侦查台完成一次侦查后，记录将保存在此处（仅存储于本机）")
                    } actions: {
                        // 空态引导（DESIGN.md §6：空态给说明 + 动作）
                        Text("前往「侦查台」开始")
                    }
                } else {
                    List {
                        ForEach(ScanType.allCases, id: \.self) { type in
                            let items = history.records.filter { $0.type == type }
                            if !items.isEmpty {
                                Section(type.label) {
                                    ForEach(items) { rec in
                                        NavigationLink(value: rec) {
                                            HistoryRow(rec: rec)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            .navigationTitle("记录")
            .navigationDestination(for: ScanRecord.self) { rec in
                RecallView(record: rec)
            }
        }
    }
}

struct HistoryRow: View {
    let rec: ScanRecord
    var body: some View {
        VStack(alignment: .leading, spacing: 2) {
            MonoText(text: rec.value).font(.gtSub)
            Text("\(rec.summary) · \(rec.date.formatted(.relative(presentation: .named)))")
                .font(.caption2).foregroundStyle(.secondary)
        }
        .padding(.vertical, 2)
    }
}

// MARK: - 设置

struct SettingsView: View {
    @Environment(AppState.self) private var appState
    @Environment(HistoryStore.self) private var history
    @State private var showLegal = false
    @State private var showClearConfirm = false
    // 调试入口：连点版本号 5 次（DESIGN.md §8）
    @State private var dbgTaps = 0
    @State private var dbgTask: Task<Void, Never>?
    // 调试模式：仅本次会话有效，不落盘（对齐 Android DebugMode）—— 重启后需重新连点 5 次
    @State private var debugUnlocked = false
    @State private var toast: String?

    var body: some View {
        NavigationStack {
            Form {
                Section("外观") {
                    Picker("模式", selection: .init(get: { appState.colorScheme }, set: { appState.colorScheme = $0 })) {
                        Text("深色").tag(ColorScheme.dark)
                        Text("浅色").tag(ColorScheme.light)
                    }
                    .pickerStyle(.segmented)
                    .listRowBackground(Color.clear)
                }

                Section("数据源") {
                    LabeledRow(title: "IPWHO.IS 接口", value: "公共接口")
                    LabeledRow(title: "号码解析引擎", value: "本地规则库")
                }

                Section("数据") {
                    Button(role: .destructive) { showClearConfirm = true } label: {
                        Text("清除全部记录")
                    }
                }

                Section("合规") {
                    Button("使用声明") { showLegal = true }
                }

                if debugUnlocked {
                    Section("调试") {
                        // 上游署名仅调试区可见（品牌要求）
                        Link(destination: URL(string: "https://github.com/HunxByts/GhostTrack")!) {
                            LabeledRow(title: "开源致谢", value: "GhostTrack · 上游开源项目")
                        }
                    }
                }

                Section {
                    Text(versionText)
                        .font(.caption).foregroundStyle(.tertiary)
                        .frame(maxWidth: .infinity)
                        .onTapGesture { handleVersionTap() }
                        .accessibilityAddTraits(.isButton)
                }
                .listRowBackground(Color.clear)
            }
            .navigationTitle("设置")
            .sheet(isPresented: $showLegal) { LegalSheet() }
            .confirmationDialog("清除全部侦查记录？", isPresented: $showClearConfirm, titleVisibility: .visible) {
                Button("清除", role: .destructive) {
                    history.clear()
                    toast = "已清除全部记录"
                }
            }
            .overlay(alignment: .bottom) {
                if let toast {
                    Text(toast).font(.gtFoot)
                        .padding(.horizontal, 16).padding(.vertical, 10)
                        .background(.ultraThinMaterial, in: .capsule)
                        .task { try? await Task.sleep(for: .seconds(1.4)); toast = nil }
                }
            }
        }
    }

    private var versionText: String {
        debugUnlocked ? "GTRACK · 版本 1.0.0 · 调试模式" : "GTRACK · 版本 1.0.0"
    }

    /// iOS 惯例彩蛋：1.6 秒内连点 5 次版本号解锁调试区
    private func handleVersionTap() {
        UIImpactFeedbackGenerator.tap()
        guard !debugUnlocked else { return }
        dbgTaps += 1
        dbgTask?.cancel()
        dbgTask = Task {
            try? await Task.sleep(for: .seconds(1.6))
            if !Task.isCancelled { dbgTaps = 0 }
        }
        if dbgTaps >= 5 {
            debugUnlocked = true
            UINotificationFeedbackGenerator().notificationOccurred(.success)
            toast = "已进入调试模式"
        } else if dbgTaps >= 2 {
            toast = "再点 \(5 - dbgTaps) 次进入调试模式"
        }
    }
}

struct LabeledRow: View {
    let title: String
    let value: String
    var body: some View {
        HStack { Text(title); Spacer(); Text(value).foregroundStyle(.secondary) }
    }
}
