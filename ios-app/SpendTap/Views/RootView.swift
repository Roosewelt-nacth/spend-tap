import SwiftUI

struct RootView: View {
    @StateObject private var store = AppStore()
    @Environment(\.scenePhase) private var scenePhase

    var body: some View {
        TabView {
            Tab("Month", systemImage: "calendar") {
                NavigationStack { MonthView() }
            }
            Tab("Analytics", systemImage: "chart.pie.fill") {
                NavigationStack { AnalyticsView() }
            }
            Tab("Savings", systemImage: "leaf.fill") {
                NavigationStack { SavingsView() }
            }
            Tab("Settings", systemImage: "gearshape.fill") {
                NavigationStack { SettingsView() }
            }
        }
        .tabBarMinimizeBehavior(.onScrollDown)
        .environmentObject(store)
        .task { await store.autoExportIfNeeded() }
        .onChange(of: scenePhase) { _, phase in
            if phase == .active { store.reload() }
        }
    }
}
