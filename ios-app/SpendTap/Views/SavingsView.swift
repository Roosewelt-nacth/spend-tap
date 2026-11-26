import SwiftUI

struct SavingsView: View {
    @EnvironmentObject var store: AppStore
    @State private var editing: SavingsPlan?
    @State private var showNew = false

    private var activeMonthly: Double {
        store.plans.filter(\.isActive).reduce(0) { $0 + $1.monthlyAmount }
    }

    var body: some View {
        ScrollView {
            VStack(spacing: 16) {
                summaryCard

                if store.plans.isEmpty {
                    Text("Add your SIPs, RDs and other monthly savings.\nThey are set aside before spending, so \"left to spend\" stays honest.")
                        .multilineTextAlignment(.center)
                        .foregroundStyle(.secondary)
                        .padding(.top, 24).padding(.horizontal, 24)
                }

                ForEach(store.plans) { plan in
                    Button { editing = plan } label: { planCard(plan) }
                        .buttonStyle(.plain)
                }
            }
            .padding(.vertical, 12)
            .animation(.smooth, value: store.plans)
        }
        .scrollIndicators(.hidden)
        .background { AuroraBackground() }
        .navigationTitle("Savings")
        .toolbar {
            ToolbarItem(placement: .topBarTrailing) {
                Button { showNew = true } label: { Image(systemName: "plus") }
            }
        }
        .sheet(isPresented: $showNew) { PlanEditor(plan: nil) }
        .sheet(item: $editing) { PlanEditor(plan: $0) }
    }

    private var summaryCard: some View {
        let income = store.settings.monthlyIncome
        return VStack(alignment: .leading, spacing: 8) {
            Text("SET ASIDE EVERY MONTH").font(.caption.monospaced()).foregroundStyle(.secondary)
            Text(rupees(activeMonthly))
                .font(.system(size: 34, weight: .semibold, design: .rounded))
                .contentTransition(.numericText(value: activeMonthly))
                .animation(.smooth, value: activeMonthly)
            if income > 0 {
                Text("\(Int((activeMonthly / income * 100).rounded()))% of your monthly income · \(rupees(activeMonthly * 12)) a year")
                    .font(.footnote).foregroundStyle(.secondary)
            } else {
                Text("\(rupees(activeMonthly * 12)) a year").font(.footnote).foregroundStyle(.secondary)
            }
        }
        .glassCard(cornerRadius: 32)
    }

    private func planCard(_ plan: SavingsPlan) -> some View {
        VStack(alignment: .leading, spacing: 10) {
            HStack(spacing: 12) {
                Image(systemName: plan.kind.symbol).font(.title3).foregroundStyle(.mint).frame(width: 32)
                VStack(alignment: .leading, spacing: 2) {
                    Text(plan.name).font(.body.weight(.semibold))
                    Text("\(plan.kind.title) · on the \(plan.dayOfMonth)\(ordinal(plan.dayOfMonth))")
                        .font(.caption).foregroundStyle(.secondary)
                }
                Spacer()
                VStack(alignment: .trailing, spacing: 2) {
                    Text(rupees(plan.monthlyAmount)).font(.body.monospacedDigit().weight(.semibold))
                    Text(plan.isActive ? "per month" : "paused").font(.caption).foregroundStyle(.secondary)
                }
            }

            Text("Put in so far: \(rupees(plan.contributed())) over \(plan.monthsContributed()) months")
                .font(.footnote).foregroundStyle(.secondary)

            if let p = plan.progress, let target = plan.targetAmount {
                ProgressView(value: p) { Text("Goal \(rupees(target))").font(.caption) }
                    .tint(.mint)
            }
            if let v = plan.projectedValue(years: 5) {
                Text("≈ \(rupees(v, compact: true)) in 5 years at \(plan.expectedReturnPct.formatted())% a year (estimate)")
                    .font(.caption).foregroundStyle(.secondary)
            }
        }
        .opacity(plan.isActive ? 1 : 0.55)
        .glassCard()
    }

    private func ordinal(_ n: Int) -> String {
        switch n % 100 {
        case 11, 12, 13: return "th"
        default:
            switch n % 10 { case 1: return "st"; case 2: return "nd"; case 3: return "rd"; default: return "th" }
        }
    }
}

struct PlanEditor: View {
    @EnvironmentObject var store: AppStore
    @Environment(\.dismiss) private var dismiss

    let original: SavingsPlan?

    @State private var name = ""
    @State private var kind: SavingsKind = .sip
    @State private var amount: Double = 0
    @State private var day = 5
    @State private var start = Date()
    @State private var hasEnd = false
    @State private var end = Calendar.current.date(byAdding: .year, value: 1, to: Date()) ?? Date()
    @State private var hasGoal = false
    @State private var goal: Double = 0
    @State private var returnPct: Double = 0
    @State private var active = true

    init(plan: SavingsPlan?) {
        original = plan
    }

    var body: some View {
        NavigationStack {
            Form {
                Section {
                    TextField("Name (e.g. Nifty index fund)", text: $name)
                    Picker("Type", selection: $kind) {
                        ForEach(SavingsKind.allCases, id: \.self) { Text($0.title).tag($0) }
                    }
                    TextField("Monthly amount", value: $amount, format: .number).keyboardType(.decimalPad)
                    Stepper("Debited on day \(day)", value: $day, in: 1...28)
                }
                Section("Schedule") {
                    DatePicker("Started", selection: $start, displayedComponents: .date)
                    Toggle("Has an end date", isOn: $hasEnd.animation())
                    if hasEnd { DatePicker("Ends", selection: $end, displayedComponents: .date) }
                    Toggle("Active", isOn: $active)
                }
                Section {
                    Toggle("Savings goal", isOn: $hasGoal.animation())
                    if hasGoal { TextField("Goal amount", value: $goal, format: .number).keyboardType(.decimalPad) }
                    TextField("Expected return % per year", value: $returnPct, format: .number).keyboardType(.decimalPad)
                } header: { Text("Optional") } footer: {
                    Text("Return is only used for a rough projection. It is an estimate, not advice or a promise.")
                }
                if original != nil {
                    Section {
                        Button("Delete", role: .destructive) {
                            if let o = original { store.delete(o) }
                            dismiss()
                        }
                    }
                }
            }
            .scrollContentBackground(.hidden)
            .background { AuroraBackground() }
            .navigationTitle(original == nil ? "New plan" : "Edit plan")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) { Button("Cancel") { dismiss() } }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Save") { save() }.disabled(name.isEmpty || amount <= 0)
                }
            }
            .onAppear(perform: load)
        }
        .presentationDetents([.large])
    }

    private func load() {
        guard let p = original else { return }
        name = p.name; kind = p.kind; amount = p.monthlyAmount; day = p.dayOfMonth
        start = p.startDate; active = p.isActive; returnPct = p.expectedReturnPct
        if let e = p.endDate { hasEnd = true; end = e }
        if let g = p.targetAmount { hasGoal = true; goal = g }
    }

    private func save() {
        var p = original ?? SavingsPlan(name: name, monthlyAmount: amount)
        p.name = name; p.kind = kind; p.monthlyAmount = amount; p.dayOfMonth = day
        p.startDate = start; p.endDate = hasEnd ? end : nil
        p.targetAmount = hasGoal && goal > 0 ? goal : nil
        p.expectedReturnPct = returnPct; p.isActive = active
        store.upsert(p)
        dismiss()
    }
}
