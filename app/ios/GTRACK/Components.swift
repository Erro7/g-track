import SwiftUI

// MARK: - 扫描页通用骨架（输入 / 校验 / 进度环 / 结果容器）

struct ScanScaffold<Result: View>: View {
    let title: String
    let placeholder: String
    @Binding var input: String
    @Binding var error: String?
    @Binding var scanning: Bool
    @Binding var progress: Double
    let canSubmit: Bool
    let resultShown: Bool
    let submitTitle: String
    let scanningTitle: String
    @Binding var toast: String?
    let onSubmit: () -> Void
    @ViewBuilder let result: () -> Result

    @FocusState private var focused: Bool

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                VStack(alignment: .leading, spacing: 8) {
                    Text("目标").font(.gtFoot).foregroundStyle(.secondary).padding(.leading, 4)
                    TextField(placeholder, text: $input)
                        .textFieldStyle(.plain)
                        .font(.system(.body, design: .monospaced))
                        .focused($focused)
                        .submitLabel(.go)
                        .autocorrectionDisabled()
                        .textInputAutocapitalization(.never)
                        .padding(14)
                        .background(.fill, in: .rect(cornerRadius: 14))
                        .overlay(.rect(cornerRadius: 14).strokeBorder(error == nil ? .clear : .gtRed, lineWidth: 1.5))
                        .onChange(of: input) {
                            error = nil // 输入即清错（保留输入，DESIGN.md §6）
                        }
                        .onSubmit { if canSubmit && !scanning { onSubmit() } }
                    if let error {
                        Label(error, systemImage: "exclamationmark.triangle")
                            .font(.gtFoot).foregroundStyle(.gtRed)
                            .transition(.opacity)
                    }
                }

                Button {
                    UIImpactFeedbackGenerator.tap(.medium)
                    focused = false
                    onSubmit()
                } label: {
                    HStack {
                        if scanning {
                            Image(systemName: "antenna.radiowaves.left.and.right").symbolEffect(.variableColor)
                            Text(scanningTitle)
                        } else {
                            Text(submitTitle)
                        }
                    }
                    .font(.gtHeadline)
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 14)
                }
                .buttonStyle(.borderedProminent)
                .disabled(!canSubmit || scanning) // CTA 合法才可点

                // 扫描进度环
                if scanning {
                    VStack(spacing: 12) {
                        ZStack {
                            Circle().stroke(.fill, lineWidth: 5)
                            Circle()
                                .trim(from: 0, to: progress)
                                .stroke(.gtGreen, style: .init(lineWidth: 5, lineCap: .round))
                                .rotationEffect(.degrees(-90))
                            Text("\(Int(progress * 100))%").font(.gtFoot).monospacedDigit()
                        }
                        .frame(width: 84, height: 84)
                        Text("查询公开数据源中…").font(.gtCaption).foregroundStyle(.secondary)
                    }
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 18)
                    .gtGlass(corner: 20)
                    .transition(.opacity.combined(with: .scale(scale: 0.96)))
                }

                if resultShown { result() }
            }
            .padding(.horizontal, 16)
            .animation(.smooth(duration: 0.25), value: scanning)
            .animation(.smooth(duration: 0.2), value: error)
        }
        .navigationTitle(title)
        .navigationBarTitleDisplayMode(.inline)
        .overlay(alignment: .bottom) {
            if let toast {
                Text(toast)
                    .font(.gtFoot).padding(.horizontal, 16).padding(.vertical, 10)
                    .background(.ultraThinMaterial, in: .capsule)
                    .padding(.bottom, 20)
                    .task {
                        try? await Task.sleep(for: .seconds(1.6))
                        self.toast = nil
                    }
            }
        }
    }
}
