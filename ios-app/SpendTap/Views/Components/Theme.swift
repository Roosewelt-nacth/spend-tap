import SwiftUI

// Shared look: an animated aurora behind everything, so the iOS 26 "Liquid Glass"
// surfaces have something to bend and blur. Glass only looks like glass when
// there is colour moving behind it.

func rupees(_ v: Double, compact: Bool = false) -> String {
    if compact, abs(v) >= 1000 {
        return "₹" + (v / 1000).formatted(.number.precision(.fractionLength(0...1))) + "k"
    }
    return "₹" + v.formatted(.number.precision(.fractionLength(0...2)))
}

enum CategoryStyle {
    static let palette: [Color] = [.pink, .orange, .yellow, .mint, .green, .teal,
                                   .cyan, .blue, .indigo, .purple, .red, .gray]

    static func color(_ raw: String) -> Color {
        let i = SpendCategory.allCases.firstIndex { $0.rawValue == raw } ?? (palette.count - 1)
        return palette[i % palette.count]
    }
    static func title(_ raw: String) -> String { SpendCategory(rawValue: raw)?.title ?? raw.capitalized }
    static func symbol(_ raw: String) -> String { SpendCategory(rawValue: raw)?.symbol ?? "questionmark.circle.fill" }
}

struct AuroraBackground: View {
    @State private var drift = false

    var body: some View {
        let mid: SIMD2<Float> = drift ? [0.72, 0.38] : [0.28, 0.62]
        let side: SIMD2<Float> = drift ? [0.12, 0.55] : [0.2, 0.42]
        MeshGradient(
            width: 3, height: 3,
            points: [
                [0, 0], [0.5, 0], [1, 0],
                side, mid, [1, 0.5],
                [0, 1], [0.5, 1], [1, 1],
            ],
            colors: [
                .black, .indigo.opacity(0.55), .black,
                .purple.opacity(0.45), .teal.opacity(0.45), .indigo.opacity(0.5),
                .black, .blue.opacity(0.4), .black,
            ]
        )
        .ignoresSafeArea()
        .background(Color.black)
        .onAppear {
            withAnimation(.easeInOut(duration: 8).repeatForever(autoreverses: true)) { drift = true }
        }
    }
}

extension View {
    func glassCard(cornerRadius: CGFloat = 28) -> some View {
        self.padding(18)
            .frame(maxWidth: .infinity, alignment: .leading)
            .glassEffect(.regular, in: .rect(cornerRadius: cornerRadius))
            .padding(.horizontal, 16)
    }
}

/// Prev / next month (or year) control. `step` is 1 for months, 12 for years.
struct PeriodSwitcher: View {
    @EnvironmentObject var store: AppStore
    var step: Int = 1

    private var title: String {
        let date = store.month.date()
        return step == 1 ? date.formatted(.dateTime.month(.wide).year()) : date.formatted(.dateTime.year())
    }

    private var canGoForward: Bool { store.month.adding(months: step) <= MonthKey(Date()) }

    var body: some View {
        HStack {
            Button {
                withAnimation(.smooth) { store.month = store.month.adding(months: -step) }
            } label: { Image(systemName: "chevron.left").frame(width: 22, height: 22) }
                .buttonStyle(.glass)
                .buttonBorderShape(.circle)

            Spacer()
            Text(title)
                .font(.title2.weight(.semibold))
                .contentTransition(.numericText())
                .animation(.smooth, value: title)
            Spacer()

            Button {
                withAnimation(.smooth) { store.month = store.month.adding(months: step) }
            } label: { Image(systemName: "chevron.right").frame(width: 22, height: 22) }
                .buttonStyle(.glass)
                .buttonBorderShape(.circle)
                .disabled(!canGoForward)
        }
        .padding(.horizontal, 20)
    }
}

/// Category chips + payment / impulse filters. Shared by Month and Analytics.
struct FilterBar: View {
    @EnvironmentObject var store: AppStore

    private var paymentLabel: String {
        switch store.filter.paymentMethod {
        case "upi": "UPI"
        case "cash": "Cash"
        default: "UPI + Cash"
        }
    }

    var body: some View {
        VStack(spacing: 10) {
            ScrollView(.horizontal, showsIndicators: false) {
                GlassEffectContainer(spacing: 8) {
                    HStack(spacing: 8) {
                        ForEach(SpendCategory.allCases, id: \.self) { cat in chip(cat) }
                    }
                    .padding(.horizontal, 20)
                }
            }

            HStack(spacing: 10) {
                Menu {
                    Picker("Payment", selection: $store.filter.paymentMethod) {
                        Text("UPI + Cash").tag(String?.none)
                        Text("UPI").tag(String?.some("upi"))
                        Text("Cash").tag(String?.some("cash"))
                    }
                } label: {
                    Label(paymentLabel, systemImage: "creditcard")
                        .font(.footnote.weight(.medium))
                        .padding(.horizontal, 12).padding(.vertical, 8)
                }
                .glassEffect(store.filter.paymentMethod == nil ? .regular.interactive()
                                                              : .regular.tint(.blue.opacity(0.5)).interactive(),
                             in: .capsule)

                Button {
                    withAnimation(.smooth) { store.filter.impulseOnly.toggle() }
                } label: {
                    Label("Impulse only", systemImage: "bolt.fill")
                        .font(.footnote.weight(.medium))
                        .padding(.horizontal, 12).padding(.vertical, 8)
                }
                .buttonStyle(.plain)
                .glassEffect(store.filter.impulseOnly ? .regular.tint(.orange.opacity(0.55)).interactive()
                                                      : .regular.interactive(),
                             in: .capsule)

                Spacer()

                if store.filter.isActive {
                    Button("Clear") { withAnimation(.smooth) { store.filter = SpendFilter() } }
                        .font(.footnote.weight(.semibold))
                        .transition(.opacity)
                }
            }
            .padding(.horizontal, 20)
        }
    }

    private func chip(_ cat: SpendCategory) -> some View {
        let on = store.filter.categories.contains(cat.rawValue)
        return Button {
            withAnimation(.smooth) {
                if on { store.filter.categories.remove(cat.rawValue) } else { store.filter.categories.insert(cat.rawValue) }
            }
        } label: {
            Label(cat.title, systemImage: cat.symbol)
                .font(.subheadline.weight(.medium))
                .padding(.horizontal, 14).padding(.vertical, 9)
        }
        .buttonStyle(.plain)
        .glassEffect(on ? .regular.tint(CategoryStyle.color(cat.rawValue).opacity(0.6)).interactive()
                        : .regular.interactive(),
                     in: .capsule)
    }
}

struct EntryRow: View {
    let entry: SpendEntry

    var body: some View {
        HStack(spacing: 14) {
            Image(systemName: CategoryStyle.symbol(entry.category))
                .font(.title3)
                .foregroundStyle(CategoryStyle.color(entry.category))
                .frame(width: 34)
            VStack(alignment: .leading, spacing: 2) {
                HStack(spacing: 6) {
                    Text(CategoryStyle.title(entry.category)).font(.body.weight(.medium))
                    if entry.isImpulse { Image(systemName: "bolt.fill").font(.caption).foregroundStyle(.orange) }
                }
                Text(entry.note.isEmpty
                     ? entry.createdAt.formatted(date: .abbreviated, time: .shortened)
                     : "\(entry.note) · \(entry.createdAt.formatted(date: .abbreviated, time: .omitted))")
                    .font(.caption).foregroundStyle(.secondary).lineLimit(1)
            }
            Spacer()
            VStack(alignment: .trailing, spacing: 2) {
                Text(rupees(entry.amount)).font(.body.monospaced().weight(.semibold))
                Text(entry.paymentMethod.uppercased()).font(.caption2).foregroundStyle(.secondary)
            }
        }
        .padding(14)
        .glassEffect(.regular, in: .rect(cornerRadius: 22))
        .padding(.horizontal, 16)
    }
}
