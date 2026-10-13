import SwiftUI

// MARK: - 色板（DESIGN.md §2）

extension Color {
    /// 磷光绿 #30D158（深）/ #1FA84B（浅）
    static let gtGreen = Color(light: .init(red: 0x1F/255, green: 0xA8/255, blue: 0x4B/255), dark: .init(red: 0x30/255, green: 0xD1/255, blue: 0x58/255))
    static let gtBlue = Color(uiColor: .systemBlue)
    static let gtRed = Color(uiColor: .systemRed)
    static let gtDeep = Color(light: .init(white: 0.95), dark: .init(red: 0x05/255, green: 0x0B/255, blue: 0x08/255))

    init(light: UIColor, dark: UIColor) {
        self.init(uiColor: UIColor { trait in
            trait.userInterfaceStyle == .light ? light : dark
        })
    }
}

// MARK: - Liquid Glass 材质（iOS 26 原生，旧系统降级）

extension View {
    /// 卡片玻璃：DESIGN.md §4 参数的 SwiftUI 映射
    @ViewBuilder
    func gtGlass(corner: CGFloat = 26) -> some View {
        if #available(iOS 26.0, *) {
            self.glassEffect(.regular.interactive, in: .rect(cornerRadius: corner))
        } else {
            self.background(.ultraThinMaterial, in: .rect(cornerRadius: corner))
                .overlay(.rect(cornerRadius: corner).strokeBorder(.white.opacity(0.12)))
        }
    }
}

// MARK: - 类型

extension Font {
    static let gtLargeTitle = Font.system(size: 34, weight: .bold)
    static let gtTitle = Font.system(size: 22, weight: .semibold)
    static let gtHeadline = Font.headline
    static let gtBody = Font.body
    static let gtSub = Font.subheadline
    static let gtFoot = Font.footnote
    static let gtCaption = Font.caption
}

/// 等宽数据（IP / 号码 / 坐标）
struct MonoText: View {
    let text: String
    var body: some View {
        Text(text).font(.system(.body, design: .monospaced))
    }
}

// MARK: - 触感

extension UIImpactFeedbackGenerator {
    static func tap(_ style: UIImpactFeedbackGenerator.FeedbackStyle = .light) {
        UIImpactFeedbackGenerator(style: style).impactOccurred()
    }
}
