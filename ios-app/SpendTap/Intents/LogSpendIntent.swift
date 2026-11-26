import AppIntents

/// Surface A from PLAN-ios-backtap.md: Back Tap -> Shortcut -> this intent.
/// iOS shows native prompts for amount (number pad) and category (list),
/// then logs the entry without opening the app.
struct LogSpendIntent: AppIntent {
    static var title: LocalizedStringResource = "Log Spend"
    static var description = IntentDescription("Record how much you just spent and on what.")
    static var openAppWhenRun: Bool = false

    @Parameter(title: "Amount", requestValueDialog: "How much was spent?")
    var amount: Double

    @Parameter(title: "Category", requestValueDialog: "What was it for?")
    var category: SpendCategory

    static var parameterSummary: some ParameterSummary {
        Summary("Log \(\.$amount) for \(\.$category)")
    }

    func perform() async throws -> some IntentResult & ProvidesDialog {
        guard amount > 0 else {
            throw $amount.needsValueError("Enter an amount above zero.")
        }
        SpendStore.shared.add(SpendEntry(amount: amount, category: category.rawValue))
        let shown = amount.formatted(.number.precision(.fractionLength(0...2)))
        return .result(dialog: "Logged ₹\(shown) for \(category.title).")
    }
}

struct SpendTapShortcuts: AppShortcutsProvider {
    static var appShortcuts: [AppShortcut] {
        AppShortcut(
            intent: LogSpendIntent(),
            phrases: [
                "Log spend in \(.applicationName)",
                "Add expense in \(.applicationName)",
            ],
            shortTitle: "Log Spend",
            systemImageName: "indianrupeesign.circle.fill"
        )
    }
}
