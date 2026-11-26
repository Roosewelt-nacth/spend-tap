import SwiftUI
import Charts

struct AnalyticsView: View {
    @EnvironmentObject var store: AppStore
    @State private var scope = 0                 // 0 = month, 1 = year
    @State private var selectedAngle: Double?

    private var totals: [CategoryTotal] {
        scope == 0 ? store.summary.byCategory : store.yearSummary.byCategory
    }
    private var grand: Double { totals.reduce(0) { $0 + $1.total } }

    private var selected: String? {
        guard let angle = selectedAngle else { return nil }
        var running = 0.0
        for t in totals {
            running += t.total
            if angle <= running { return t.category }
        }
        return nil
    }

    var body: some View {
        ScrollView {
            VStack(spacing: 16) {
                Picker("Scope", selection: $scope.animation(.smooth)) {
                    Text("Month").tag(0)
                    Text("Year").tag(1)
                }
                .pickerStyle(.segmented)
                .padding(.horizontal, 20)

                PeriodSwitcher(step: scope == 0 ? 1 : 12)
                FilterBar()

                pieCard
                barCard
                if scope == 0 { incomeSplitCard }
                legendCard
            }
            .padding(.vertical, 12)
        }
        .scrollIndicators(.hidden)
        .background { AuroraBackground() }
        .navigationTitle("Analytics")
        .onChange(of: scope) { _, _ in selectedAngle = nil }
        .onChange(of: store.month) { _, _ in selectedAngle = nil }
    }

    // MARK: pie

    private var pieCard: some View {
        VStack {
            if totals.isEmpty {
                Text("Nothing to chart yet.").foregroundStyle(.secondary).frame(height: 240)
            } else {
                ZStack {
                    Chart(totals) { t in
                        SectorMark(angle: .value("Spent", t.total),
                                   innerRadius: .ratio(0.64),
                                   angularInset: 2)
                            .cornerRadius(7)
                            .foregroundStyle(CategoryStyle.color(t.category))
                            .opacity(selected == nil || selected == t.category ? 1 : 0.28)
                    }
                    .chartAngleSelection(value: $selectedAngle)
                    .frame(height: 260)

                    centerLabel
                }
                .animation(.smooth, value: selected)
                .animation(.smooth, value: totals)
            }
        }
        .glassCard(cornerRadius: 32)
    }

    private var centerLabel: some View {
        VStack(spacing: 4) {
            if let sel = selected, let t = totals.first(where: { $0.category == sel }) {
                Image(systemName: CategoryStyle.symbol(sel)).foregroundStyle(CategoryStyle.color(sel))
                Text(CategoryStyle.title(sel)).font(.subheadline.weight(.medium))
                Text(rupees(t.total)).font(.title2.weight(.semibold).monospacedDigit())
                Text("\(Int((t.share * 100).rounded()))% of spend").font(.caption).foregroundStyle(.secondary)
            } else {
                Text(scope == 0 ? "THIS MONTH" : "THIS YEAR").font(.caption.monospaced()).foregroundStyle(.secondary)
                Text(rupees(grand)).font(.title.weight(.semibold).monospacedDigit())
                    .contentTransition(.numericText(value: grand))
                Text("Tap a slice").font(.caption).foregroundStyle(.secondary)
            }
        }
        .frame(width: 140)
        .multilineTextAlignment(.center)
    }

    // MARK: bars

    private var barCard: some View {
        VStack(alignment: .leading, spacing: 10) {
            Text(scope == 0 ? "DAILY SPEND" : "MONTHLY SPEND")
                .font(.caption.monospaced()).foregroundStyle(.secondary)

            if scope == 0 {
                Chart(store.summary.byDay) { d in
                    BarMark(x: .value("Day", d.day), y: .value("Spent", d.total))
                        .foregroundStyle(LinearGradient(colors: [.cyan, .indigo], startPoint: .top, endPoint: .bottom))
                        .cornerRadius(3)
                }
                .chartXScale(domain: 1...store.month.daysInMonth())
                .chartXAxis { AxisMarks(values: [1, 8, 15, 22, 29]) }
                .frame(height: 150)
            } else {
                let months = Array(store.yearSummary.byMonth.enumerated())
                Chart(months, id: \.offset) { m in
                    BarMark(x: .value("Month", Calendar.current.shortMonthSymbols[m.offset]),
                            y: .value("Spent", m.element))
                        .foregroundStyle(LinearGradient(colors: [.mint, .teal], startPoint: .top, endPoint: .bottom))
                        .cornerRadius(5)
                }
                .frame(height: 150)
            }
        }
        .glassCard()
        .animation(.smooth, value: store.month)
    }

    // MARK: income split

    private var incomeSplitCard: some View {
        let s = store.summary
        let income = store.settings.monthlyIncome
        let spend = s.spentAll
        let save = s.savingsCommitted
        let free = max(0, income - spend - save)
        return Group {
            if income > 0 {
                VStack(alignment: .leading, spacing: 12) {
                    Text("WHERE YOUR INCOME WENT").font(.caption.monospaced()).foregroundStyle(.secondary)
                    GeometryReader { geo in
                        HStack(spacing: 3) {
                            segment(.pink, spend / income, geo.size.width)
                            segment(.mint, save / income, geo.size.width)
                            segment(.white.opacity(0.25), free / income, geo.size.width)
                        }
                    }
                    .frame(height: 14)
                    HStack {
                        key(.pink, "Spent", spend)
                        key(.mint, "Saved", save)
                        key(.white.opacity(0.4), "Free", free)
                    }
                }
                .glassCard()
            }
        }
    }

    private func segment(_ color: Color, _ fraction: Double, _ width: CGFloat) -> some View {
        Capsule().fill(color).frame(width: max(0, min(1, fraction)) * (width - 6))
            .animation(.smooth, value: fraction)
    }

    private func key(_ color: Color, _ title: String, _ value: Double) -> some View {
        HStack(spacing: 6) {
            Circle().fill(color).frame(width: 8, height: 8)
            VStack(alignment: .leading, spacing: 0) {
                Text(title).font(.caption).foregroundStyle(.secondary)
                Text(rupees(value, compact: true)).font(.footnote.weight(.semibold).monospacedDigit())
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }

    // MARK: legend

    private var legendCard: some View {
        VStack(spacing: 14) {
            ForEach(totals) { t in
                Button {
                    withAnimation(.smooth) {
                        selectedAngle = selected == t.category ? nil : angleMidpoint(of: t.category)
                    }
                } label: {
                    VStack(spacing: 6) {
                        HStack {
                            Image(systemName: CategoryStyle.symbol(t.category))
                                .foregroundStyle(CategoryStyle.color(t.category)).frame(width: 26)
                            Text(CategoryStyle.title(t.category))
                            Text("\(t.count)×").font(.caption).foregroundStyle(.secondary)
                            Spacer()
                            Text(rupees(t.total)).font(.body.monospacedDigit().weight(.semibold))
                            Text("\(Int((t.share * 100).rounded()))%")
                                .font(.caption.monospacedDigit()).foregroundStyle(.secondary).frame(width: 38, alignment: .trailing)
                        }
                        GeometryReader { geo in
                            Capsule().fill(.white.opacity(0.1))
                                .overlay(alignment: .leading) {
                                    Capsule().fill(CategoryStyle.color(t.category)).frame(width: geo.size.width * t.share)
                                }
                        }
                        .frame(height: 5)
                    }
                }
                .buttonStyle(.plain)
            }
            if totals.isEmpty { Text("No data for this period.").foregroundStyle(.secondary) }
        }
        .glassCard()
        .animation(.smooth, value: totals)
    }

    private func angleMidpoint(of category: String) -> Double {
        var running = 0.0
        for t in totals {
            if t.category == category { return running + t.total / 2 }
            running += t.total
        }
        return 0
    }
}
