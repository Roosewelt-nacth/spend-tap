import SwiftUI

struct MonthView: View {
    @EnvironmentObject var store: AppStore
    @State private var showAdd = false

    var body: some View {
        let s = store.summary

        ScrollView {
            VStack(spacing: 16) {
                PeriodSwitcher()
                HeroCard(summary: s)
                FilterBar()
                stats(s)

                if s.entries.isEmpty {
                    Text(store.filter.isActive ? "Nothing matches these filters."
                                               : "No spends this month yet.\nTriple-tap the back of your phone to log one.")
                        .multilineTextAlignment(.center)
                        .foregroundStyle(.secondary)
                        .padding(.top, 30)
                } else {
                    LazyVStack(spacing: 10) {
                        ForEach(s.entries) { e in
                            EntryRow(entry: e)
                                .contextMenu {
                                    Button(role: .destructive) {
                                        withAnimation(.smooth) { store.delete(e) }
                                    } label: { Label("Delete", systemImage: "trash") }
                                }
                                .transition(.scale.combined(with: .opacity))
                        }
                    }
                }
            }
            .padding(.vertical, 12)
            .animation(.smooth, value: s.entries)
        }
        .scrollIndicators(.hidden)
        .background { AuroraBackground() }
        .navigationTitle("Spend Tap")
        .toolbar {
            ToolbarItem(placement: .topBarTrailing) {
                Button { showAdd = true } label: { Image(systemName: "plus") }
            }
        }
        .sheet(isPresented: $showAdd) { AddEntrySheet() }
    }

    private func stats(_ s: MonthSummary) -> some View {
        let days = max(1, Calendar.current.component(.day, from: Date()))
        let isCurrent = s.key == MonthKey(Date())
        let divisor = isCurrent ? days : s.key.daysInMonth()
        return GlassEffectContainer(spacing: 10) {
            HStack(spacing: 10) {
                tile("Entries", "\(s.count)", "list.bullet")
                tile("Per day", rupees(s.spent / Double(divisor), compact: true), "sun.max.fill")
                tile("Impulse", rupees(s.impulse, compact: true), "bolt.fill")
            }
        }
        .padding(.horizontal, 16)
    }

    private func tile(_ title: String, _ value: String, _ icon: String) -> some View {
        VStack(alignment: .leading, spacing: 6) {
            Image(systemName: icon).font(.footnote).foregroundStyle(.secondary)
            Text(value)
                .font(.title3.weight(.semibold).monospacedDigit())
                .contentTransition(.numericText())
                .animation(.smooth, value: value)
            Text(title).font(.caption).foregroundStyle(.secondary)
        }
        .padding(14)
        .frame(maxWidth: .infinity, alignment: .leading)
        .glassEffect(.regular, in: .rect(cornerRadius: 22))
    }
}

struct HeroCard: View {
    let summary: MonthSummary

    private var tint: Color {
        summary.progress >= 1 ? .red : (summary.progress >= 0.75 ? .orange : .mint)
    }

    var body: some View {
        HStack(spacing: 20) {
            ZStack {
                Circle().stroke(.white.opacity(0.12), lineWidth: 14)
                Circle()
                    .trim(from: 0, to: min(1, summary.progress))
                    .stroke(tint.gradient, style: StrokeStyle(lineWidth: 14, lineCap: .round))
                    .rotationEffect(.degrees(-90))
                Text(summary.limit == nil ? "—" : "\(Int(min(summary.progress, 9.99) * 100))%")
                    .font(.title3.weight(.semibold).monospacedDigit())
                    .contentTransition(.numericText())
            }
            .frame(width: 110, height: 110)
            .animation(.smooth(duration: 0.9), value: summary.progress)

            VStack(alignment: .leading, spacing: 6) {
                Text("SPENT THIS MONTH")
                    .font(.caption.monospaced())
                    .foregroundStyle(.secondary)
                Text(rupees(summary.spentAll))
                    .font(.system(size: 32, weight: .semibold, design: .rounded))
                    .minimumScaleFactor(0.6)
                    .lineLimit(1)
                    .contentTransition(.numericText(value: summary.spentAll))
                    .animation(.smooth, value: summary.spentAll)

                if let rem = summary.remaining {
                    Text(rem >= 0 ? "\(rupees(rem)) left" : "\(rupees(-rem)) over budget")
                        .font(.subheadline.weight(.medium))
                        .foregroundStyle(rem >= 0 ? Color.mint : Color.red)
                } else {
                    Text("Set income or a budget in Settings")
                        .font(.footnote).foregroundStyle(.secondary)
                }
                if summary.savingsCommitted > 0 {
                    Label("\(rupees(summary.savingsCommitted)) set aside for SIPs", systemImage: "leaf.fill")
                        .font(.footnote).foregroundStyle(.secondary)
                }
            }
        }
        .glassCard(cornerRadius: 32)
    }
}
