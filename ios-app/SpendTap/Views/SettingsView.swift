import SwiftUI

struct SettingsView: View {
    @EnvironmentObject var store: AppStore
    private let thisYear = Calendar.current.component(.year, from: Date())

    var body: some View {
        Form {
            Section {
                TextField("Monthly income", value: $store.settings.monthlyIncome, format: .number)
                    .keyboardType(.decimalPad)
                TextField("Monthly spending budget", value: $store.settings.monthlyBudget, format: .number)
                    .keyboardType(.decimalPad)
            } header: { Text("Monthly plan") } footer: {
                Text("Leave the budget at 0 and \"left to spend\" becomes income minus your SIPs and savings.")
            }

            Section {
                TextField("Web app URL (…/exec)", text: $store.settings.sheetsURL)
                    .textInputAutocapitalization(.never).autocorrectionDisabled().keyboardType(.URL)
                SecureField("Secret from the script", text: $store.settings.sheetsToken)

                Button {
                    Task { await store.exportYear(thisYear) }
                } label: { Label("Export \(thisYear) now", systemImage: "arrow.up.doc") }
                    .disabled(store.isExporting)

                Button {
                    Task { await store.exportYear(thisYear - 1) }
                } label: { Label("Export \(thisYear - 1)", systemImage: "arrow.up.doc") }
                    .disabled(store.isExporting)

                if store.isExporting { ProgressView() }
                if !store.exportStatus.isEmpty {
                    Text(store.exportStatus).font(.footnote).foregroundStyle(.secondary)
                }
            } header: { Text("Yearly Google Sheet") } footer: {
                Text("Once a year, the first time you open the app in January, last year is uploaded to a tab named after the year. Setup steps are at the top of google-sheets/Code.gs in the project.")
            }

            Section("About") {
                LabeledContent("Back Tap", value: "Settings › Accessibility › Touch")
                LabeledContent("Data", value: "Stored on this iPhone")
            }
        }
        .scrollContentBackground(.hidden)
        .background { AuroraBackground() }
        .navigationTitle("Settings")
        .onChange(of: store.settings) { _, _ in store.saveSettings() }
    }
}

struct AddEntrySheet: View {
    @EnvironmentObject var store: AppStore
    @Environment(\.dismiss) private var dismiss

    @State private var amount: Double = 0
    @State private var category: SpendCategory = .personal
    @State private var date = Date()
    @State private var note = ""
    @State private var payment = "upi"
    @State private var impulse = false

    var body: some View {
        NavigationStack {
            Form {
                TextField("Amount", value: $amount, format: .number).keyboardType(.decimalPad)
                Picker("Category", selection: $category) {
                    ForEach(SpendCategory.allCases, id: \.self) { Label($0.title, systemImage: $0.symbol).tag($0) }
                }
                DatePicker("When", selection: $date, in: ...Date())
                Picker("Paid with", selection: $payment) {
                    Text("UPI").tag("upi")
                    Text("Cash").tag("cash")
                }
                .pickerStyle(.segmented)
                Toggle("Impulse buy", isOn: $impulse)
                TextField("Note (optional)", text: $note)
            }
            .scrollContentBackground(.hidden)
            .background { AuroraBackground() }
            .navigationTitle("Add spend")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) { Button("Cancel") { dismiss() } }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Save") {
                        store.add(SpendEntry(amount: amount, category: category.rawValue, note: note,
                                             paymentMethod: payment, isImpulse: impulse, createdAt: date))
                        dismiss()
                    }
                    .disabled(amount <= 0)
                }
            }
        }
    }
}
