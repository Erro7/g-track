import SwiftUI

struct RootView: View {
    @Environment(AppState.self) private var appState

    var body: some View {
        if appState.hasAgreedLegal {
            MainTabs()
                .preferredColorScheme(appState.colorScheme)
        } else {
            OnboardingView()
                .preferredColorScheme(appState.colorScheme)
        }
    }
}

// MARK: - 主 Tab（侦查 / 记录 / 设置）

struct MainTabs: View {
    var body: some View {
        TabView {
            Tab("侦查", systemImage: "dot.radiowaves.left.and.right") {
                HomeView()
            }
            Tab("记录", systemImage: "clock.arrow.circlepath") {
                HistoryView()
            }
            Tab("设置", systemImage: "gearshape") {
                SettingsView()
            }
        }
        // iOS 26：滚动时 Tab 栏自动收缩
        .tabBarMinimizeBehavior(.onScrollDown)
    }
}

// MARK: - 首启合规页

struct OnboardingView: View {
    @Environment(AppState.self) private var appState
    @State private var showLegal = false

    var body: some View {
        ZStack {
            // 环境光底（DESIGN.md §3）
            Color.gtDeep.ignoresSafeArea()
            RadialGradient(colors: [Color(hex: 0x0C2118), .clear], center: .top, startRadius: 0, endRadius: 480)
                .ignoresSafeArea().opacity(0.9)

            VStack(spacing: 0) {
                Spacer(minLength: 48)
                Image(systemName: "ghost")
                    .font(.system(size: 64, weight: .light))
                    .foregroundStyle(.gtGreen)
                    .shadow(color: .gtGreen.opacity(0.45), radius: 18)
                Text("GTRACK")
                    .font(.gtLargeTitle)
                    .foregroundStyle(.primary)
                    .padding(.top, 16)
                Text("合法授权下的公开信息核查工具")
                    .font(.gtSub)
                    .foregroundStyle(.secondary)
                    .padding(.top, 6)

                VStack(spacing: 0) {
                    legalPoint("checkmark.shield", "仅限合法授权调查", "安全调查 / 协寻 / 公开信息核实")
                    Divider().padding(.leading, 50)
                    legalPoint("globe", "只查公开数据源", "公开接口与公开网页，不涉非公开数据")
                    Divider().padding(.leading, 50)
                    legalPoint("iphone", "记录仅存本机", "不上传云端，可随时一键清除")
                }
                .gtGlass(corner: 20)
                .padding(.top, 30)

                Spacer()
                Button {
                    UIImpactFeedbackGenerator.tap()
                    appState.hasAgreedLegal = true
                } label: {
                    Text("同意并进入")
                        .font(.gtHeadline)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 15)
                }
                .buttonStyle(.borderedProminent)

                // 原型 .fulllegal：次要文字 + 下划线，明示可点
                Button { showLegal = true } label: {
                    Text("查看完整使用声明")
                        .font(.gtFoot)
                        .underline()
                        .foregroundStyle(.secondary)
                }
                .buttonStyle(.plain)
                .padding(.top, 12)

                Text("GTRACK · V1.0.0")
                    .font(.system(size: 10, weight: .semibold))
                    .foregroundStyle(.tertiary)
                    .tracking(1.2)
                    .padding(.top, 18)
            }
            .padding(.horizontal, 22)
        }
        .sheet(isPresented: $showLegal) { LegalSheet() }
    }

    private func legalPoint(_ icon: String, _ title: String, _ sub: String) -> some View {
        HStack(spacing: 12) {
            Image(systemName: icon)
                .frame(width: 26)
                .foregroundStyle(.gtGreen)
            VStack(alignment: .leading, spacing: 1) {
                Text(title).font(.gtSub).foregroundStyle(.primary)
                Text(sub).font(.caption).foregroundStyle(.secondary)
            }
            Spacer(minLength: 0)
        }
        .padding(.horizontal, 16).padding(.vertical, 12)
    }
}

// MARK: - 使用声明（半屏 Sheet，detent 对应原型 105 层规范）

struct LegalSheet: View {
    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 14) {
                legalBlock("用途边界", "本工具仅限用于合法授权的安全调查、失联亲友协寻、资产核实与公开信息研究。使用者需对自身查询行为承担全部法律责任。")
                legalBlock("数据来源", "所有查询结果均来自公开数据接口与公开网页，本工具不入侵、不监听、不获取任何非公开数据。")
                legalBlock("数据留存", "查询记录默认仅保存在设备本地，可随时在「设置」中一键清除。")
                legalBlock("禁止行为", "严禁用于跟踪骚扰、人肉搜索、电信诈骗或其他违法用途。发现滥用请立即停止使用。")
            }
            .padding(.horizontal, 18).padding(.top, 44).padding(.bottom, 24)
        }
        .background(Color.gtDeep)
        .presentationDragIndicator(.visible)
        .presentationDetents([.medium, .large]) // 半屏优先
        // 无「完成」按钮：拖动指示条下拉 / 下滑即可关闭
        .overlay(alignment: .top) {
            HStack {
                Text("使用声明").font(.gtHeadline)
                Spacer()
            }
            .padding(.horizontal, 18).padding(.top, 8)
        }
    }

    private func legalBlock(_ title: String, _ body: String) -> some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(title).font(.gtHeadline).foregroundStyle(.primary)
            Text(body).font(.gtFoot).foregroundStyle(.secondary).lineSpacing(4)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }
}

// 便捷色值
extension Color {
    init(hex: UInt) {
        self.init(.sRGB, red: Double((hex >> 16) & 0xFF) / 255, green: Double((hex >> 8) & 0xFF) / 255, blue: Double(hex & 0xFF) / 255)
    }
}
