import SwiftUI

@main
struct GTRACKApp: App {
    @State private var appState = AppState()

    var body: some Scene {
        WindowGroup {
            RootView()
                .environment(appState)
                .environment(appState.history)
                .preferredColorScheme(appState.colorScheme)
                .tint(.gtGreen)
        }
    }
}

/// 全局应用状态
@Observable
final class AppState {
    var hasAgreedLegal: Bool = UserDefaults.standard.bool(forKey: "hasAgreedLegal") {
        didSet { UserDefaults.standard.set(hasAgreedLegal, forKey: "hasAgreedLegal") }
    }
    var colorScheme: ColorScheme = {
        switch UserDefaults.standard.string(forKey: "appearance") {
        case "light": return .light
        default: return .dark // 深色默认
        }
    }() {
        didSet { UserDefaults.standard.set(colorScheme == .light ? "light" : "dark", forKey: "appearance") }
    }
    let history = HistoryStore()
}
