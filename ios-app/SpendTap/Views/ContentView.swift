import SwiftUI

struct ContentView: View {
    @State private var entries: [SpendEntry] = []
    @State private var today: Double = 0
    @Environment(\.scenePhase) private var scenePhase

    var body: some View {
        NavigationStack {
            List {
                Section {
                    VStack(alignment: .leading, spacing: 4) {
                        Text("TODAY")
                            .font(.caption.monospaced())
                            .foregroundStyle(.secondary)
                        Text("₹\(today.formatted(.number.precision(.fractionLength(0...2))))")
                            .font(.system(size: 40, weight: .semibold, design: .monospaced))
                    }
                    .padding(.vertical, 6)
                }

                Section("Recent") {
                    if entries.isEmpty {
                        Text("Nothing logged yet. Set up Back Tap to log a spend.")
                            .foregroundStyle(.secondary)
                    }
                    ForEach(entries) { entry in
                        row(entry)
                    }
                    .onDelete { offsets in
                        for i in offsets { SpendStore.shared.delete(id: entries[i].id) }
                        reload()
                    }
                }

                Section("Back Tap setup") {
                    Text("1. Open Shortcuts and create a shortcut with the action \"Log Spend\".\n2. Settings, Accessibility, Touch, Back Tap, Triple Tap, choose that shortcut.")
                        .font(.footnote)
                        .foregroundStyle(.secondary)
                }
            }
            .navigationTitle("Spend Tap")
            .onAppear(perform: reload)
            .onChange(of: scenePhase) { _, phase in
                if phase == .active { reload() }
            }
        }
    }

    private func row(_ entry: SpendEntry) -> some View {
        let cat = SpendCategory(rawValue: entry.category) ?? .other
        return HStack {
            Image(systemName: cat.symbol)
                .frame(width: 28)
            VStack(alignment: .leading) {
                Text(cat.title)
                Text(entry.createdAt.formatted(date: .abbreviated, time: .shortened))
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }
            Spacer()
            Text("₹\(entry.amount.formatted(.number.precision(.fractionLength(0...2))))")
                .font(.body.monospaced())
        }
    }

    private func reload() {
        entries = SpendStore.shared.all()
        today = SpendStore.shared.todayTotal()
    }
}

#Preview { ContentView() }
