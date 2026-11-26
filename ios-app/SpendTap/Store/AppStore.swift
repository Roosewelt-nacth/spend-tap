import SwiftUI

/// One observable object the whole UI reads. Views never touch files or do maths;
/// they ask the store, and the store asks Analytics.
@MainActor
final class AppStore: ObservableObject {
    @Published var entries: [SpendEntry] = []
    @Published var plans: [SavingsPlan] = []
    @Published var settings = AppSettings()

    // UI state shared by the Month and Analytics tabs
    @Published var month = MonthKey(Date())
    @Published var filter = SpendFilter()

    @Published var exportStatus = ""
    @Published var isExporting = false

    private let plansFile = JSONFile<[SavingsPlan]>(name: "plans.json", defaultValue: [])
    private let settingsFile = JSONFile<AppSettings>(name: "settings.json", defaultValue: AppSettings())

    init() { reload() }

    /// Entries are written by the Back Tap intent in the same process,
    /// so re-read from disk whenever the app becomes active.
    func reload() {
        entries = SpendStore.shared.all()
        plans = plansFile.load()
        settings = settingsFile.load()
    }

    // MARK: derived

    var summary: MonthSummary {
        Analytics.summary(for: month, entries: entries, plans: plans, settings: settings, filter: filter)
    }

    var yearSummary: YearSummary {
        Analytics.yearSummary(year: month.year, entries: entries, plans: plans, filter: filter)
    }

    var canGoForward: Bool { month < MonthKey(Date()) }

    // MARK: entries

    func add(_ entry: SpendEntry) {
        SpendStore.shared.add(entry)
        entries = SpendStore.shared.all()
    }

    func delete(_ entry: SpendEntry) {
        SpendStore.shared.delete(id: entry.id)
        entries = SpendStore.shared.all()
    }

    // MARK: savings

    func upsert(_ plan: SavingsPlan) {
        if let i = plans.firstIndex(where: { $0.id == plan.id }) { plans[i] = plan } else { plans.append(plan) }
        plansFile.save(plans)
    }

    func delete(_ plan: SavingsPlan) {
        plans.removeAll { $0.id == plan.id }
        plansFile.save(plans)
    }

    // MARK: settings

    func saveSettings() { settingsFile.save(settings) }

    // MARK: Google Sheets

    func exportYear(_ year: Int) async {
        guard !settings.sheetsURL.isEmpty else {
            exportStatus = "Add your Google Sheet link in Settings first."
            return
        }
        isExporting = true
        defer { isExporting = false }
        let report = Analytics.report(year: year, entries: entries, plans: plans,
                                      categories: SpendCategory.allCases.map(\.rawValue))
        do {
            try await SheetsExporter.send(report, urlString: settings.sheetsURL, token: settings.sheetsToken)
            exportStatus = "Exported \(year) to your sheet."
        } catch {
            exportStatus = "Export failed: \(error.localizedDescription)"
        }
    }

    /// Runs on launch. iOS cannot wake an app on 1 January, so the first time the
    /// app opens in a new year it uploads the finished previous year.
    func autoExportIfNeeded() async {
        let calendar = Calendar.current
        let lastYear = calendar.component(.year, from: Date()) - 1
        guard !settings.sheetsURL.isEmpty,
              (settings.lastAutoExportedYear ?? 0) < lastYear,
              entries.contains(where: { calendar.component(.year, from: $0.createdAt) == lastYear })
        else { return }
        await exportYear(lastYear)
        if exportStatus.hasPrefix("Exported") {
            settings.lastAutoExportedYear = lastYear
            saveSettings()
        }
    }
}
