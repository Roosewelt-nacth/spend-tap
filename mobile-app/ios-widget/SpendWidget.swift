// WidgetKit extension for the Spend Tracker home-screen widget.
//
// This file is NOT part of the React Native app target — it belongs in a
// separate "Widget Extension" target that you add in Xcode (File > New >
// Target > Widget Extension, name it "SpendWidget"). It has no dependency on
// React Native; it only reads the JSON blob the app writes to the shared App
// Group via `react-native-shared-group-preferences` (see widgetSync.ts).
//
// Setup checklist (do this in Xcode, on a Mac):
// 1. Add an App Group capability to both the main app target and this widget
//    target, using the same group id, e.g. group.com.yourorg.spendtracker
// 2. Match APP_GROUP below to that id, and to the one used in widgetSync.ts
// 3. Add this file to the SpendWidget extension target

import WidgetKit
import SwiftUI

private let APP_GROUP = "group.com.yourorg.spendtracker"

struct SpendSummary: Codable {
    var todaySpend: Double
    var dailyBudget: Double
    var weeklySpend: Double
    var weeklyBudget: Double
    var topCategory: String
}

func loadSummary() -> SpendSummary {
    let fallback = SpendSummary(todaySpend: 0, dailyBudget: 0, weeklySpend: 0, weeklyBudget: 0, topCategory: "—")
    guard let defaults = UserDefaults(suiteName: APP_GROUP),
          let raw = defaults.string(forKey: "spendWidgetSummary"),
          let data = raw.data(using: .utf8),
          let summary = try? JSONDecoder().decode(SpendSummary.self, from: data)
    else {
        return fallback
    }
    return summary
}

struct SpendEntry: TimelineEntry {
    let date: Date
    let summary: SpendSummary
}

struct SpendProvider: TimelineProvider {
    func placeholder(in context: Context) -> SpendEntry {
        SpendEntry(date: Date(), summary: SpendSummary(todaySpend: 0, dailyBudget: 500, weeklySpend: 0, weeklyBudget: 3000, topCategory: "Essentials"))
    }

    func getSnapshot(in context: Context, completion: @escaping (SpendEntry) -> Void) {
        completion(SpendEntry(date: Date(), summary: loadSummary()))
    }

    func getTimeline(in context: Context, completion: @escaping (Timeline<SpendEntry>) -> Void) {
        let entry = SpendEntry(date: Date(), summary: loadSummary())
        // Refresh every 30 minutes; the app also force-refreshes the widget
        // immediately after each new entry via WidgetCenter.reloadAllTimelines()
        // triggered from the native module side.
        let nextUpdate = Calendar.current.date(byAdding: .minute, value: 30, to: Date())!
        completion(Timeline(entries: [entry], policy: .after(nextUpdate)))
    }
}

struct SpendWidgetView: View {
    var entry: SpendProvider.Entry

    var pct: Double {
        entry.summary.dailyBudget > 0
            ? min(1.0, entry.summary.todaySpend / entry.summary.dailyBudget)
            : 0
    }

    var barColor: Color {
        if pct >= 1.0 { return Color(red: 0.97, green: 0.44, blue: 0.44) }
        if pct >= 0.7 { return Color(red: 0.98, green: 0.75, blue: 0.14) }
        return Color(red: 0.43, green: 0.9, blue: 0.7)
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            Text("Today's spend")
                .font(.caption2)
                .foregroundColor(.gray)
            Text("₹\(Int(entry.summary.todaySpend)) / ₹\(Int(entry.summary.dailyBudget))")
                .font(.headline)
                .foregroundColor(.white)
            GeometryReader { geo in
                ZStack(alignment: .leading) {
                    RoundedRectangle(cornerRadius: 3)
                        .fill(Color.white.opacity(0.1))
                    RoundedRectangle(cornerRadius: 3)
                        .fill(barColor)
                        .frame(width: max(4, geo.size.width * pct))
                }
            }
            .frame(height: 6)
            Spacer()
            Text("Top this week: \(entry.summary.topCategory)")
                .font(.caption2)
                .foregroundColor(.gray)
        }
        .padding(12)
        .background(Color(red: 0.09, green: 0.10, blue: 0.13))
    }
}

struct SpendWidget: Widget {
    let kind: String = "SpendWidget"

    var body: some WidgetConfiguration {
        StaticConfiguration(kind: kind, provider: SpendProvider()) { entry in
            SpendWidgetView(entry: entry)
        }
        .configurationDisplayName("Spend Tracker")
        .description("Today's and this week's spend against your budget.")
        .supportedFamilies([.systemSmall, .systemMedium])
    }
}
