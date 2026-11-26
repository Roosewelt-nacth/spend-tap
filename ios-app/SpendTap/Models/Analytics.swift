import Foundation

// Pure calculation code. Foundation only, no SwiftUI, so it can be tested from a
// plain command-line script (ios-app/checks/main.swift) without a simulator.

struct MonthKey: Hashable, Comparable, Codable, Identifiable {
    var year: Int
    var month: Int          // 1...12
    var id: Int { year * 100 + month }

    init(year: Int, month: Int) { self.year = year; self.month = month }

    init(_ date: Date, calendar: Calendar = .current) {
        let c = calendar.dateComponents([.year, .month], from: date)
        year = c.year ?? 1970
        month = c.month ?? 1
    }

    static func < (l: MonthKey, r: MonthKey) -> Bool { l.id < r.id }

    func date(calendar: Calendar = .current) -> Date {
        calendar.date(from: DateComponents(year: year, month: month, day: 1)) ?? Date()
    }

    func adding(months n: Int, calendar: Calendar = .current) -> MonthKey {
        let d = calendar.date(byAdding: .month, value: n, to: date(calendar: calendar)) ?? date(calendar: calendar)
        return MonthKey(d, calendar: calendar)
    }

    func daysInMonth(calendar: Calendar = .current) -> Int {
        calendar.range(of: .day, in: .month, for: date(calendar: calendar))?.count ?? 30
    }
}

struct SpendFilter: Equatable {
    var categories: Set<String> = []     // empty = every category
    var paymentMethod: String? = nil     // "upi" | "cash" | nil = both
    var impulseOnly = false

    var isActive: Bool { !categories.isEmpty || paymentMethod != nil || impulseOnly }

    func matches(_ e: SpendEntry) -> Bool {
        if !categories.isEmpty && !categories.contains(e.category) { return false }
        if let p = paymentMethod, e.paymentMethod != p { return false }
        if impulseOnly && !e.isImpulse { return false }
        return true
    }
}

struct CategoryTotal: Identifiable, Equatable {
    var category: String
    var total: Double
    var count: Int
    var share: Double                    // 0...1 of the filtered total
    var id: String { category }
}

struct DayTotal: Identifiable, Equatable {
    var day: Int
    var total: Double
    var id: Int { day }
}

struct MonthSummary {
    var key: MonthKey
    var spentAll: Double                 // whole month, ignores filters (drives the hero ring)
    var spent: Double                    // after filters (drives charts and the list)
    var count: Int
    var impulse: Double
    var byCategory: [CategoryTotal]
    var byDay: [DayTotal]
    var entries: [SpendEntry]            // filtered, newest first
    var savingsCommitted: Double         // SIPs etc. due this month
    var limit: Double?                   // budget, or income minus savings, or nil
    var remaining: Double? { limit.map { $0 - spentAll } }
    var progress: Double {
        guard let l = limit, l > 0 else { return 0 }
        return spentAll / l
    }
}

struct YearSummary {
    var year: Int
    var total: Double
    var byCategory: [CategoryTotal]
    var byMonth: [Double]                // 12 values, Jan...Dec
    var savingsByMonth: [Double]         // 12 values
}

/// What gets uploaded to the user's Google Sheet once a year.
struct YearlyReport: Codable {
    var year: Int
    var categories: [String]
    var months: [[Double]]               // 12 rows x categories
    var spendTotals: [Double]            // 12
    var savings: [Double]                // 12
    var generatedAt: Date
}

enum Analytics {
    static func categoryTotals(_ entries: [SpendEntry]) -> [CategoryTotal] {
        let grand = entries.reduce(0) { $0 + $1.amount }
        var sums: [String: (total: Double, count: Int)] = [:]
        for e in entries {
            let cur = sums[e.category] ?? (0, 0)
            sums[e.category] = (cur.total + e.amount, cur.count + 1)
        }
        return sums
            .map { CategoryTotal(category: $0.key, total: $0.value.total, count: $0.value.count,
                                 share: grand > 0 ? $0.value.total / grand : 0) }
            .sorted { $0.total > $1.total }
    }

    static func summary(for key: MonthKey,
                        entries: [SpendEntry],
                        plans: [SavingsPlan],
                        settings: AppSettings,
                        filter: SpendFilter = SpendFilter(),
                        calendar: Calendar = .current) -> MonthSummary {
        let inMonth = entries.filter { MonthKey($0.createdAt, calendar: calendar) == key }
        let shown = inMonth.filter(filter.matches).sorted { $0.createdAt > $1.createdAt }

        var perDay: [Int: Double] = [:]
        for e in shown { perDay[calendar.component(.day, from: e.createdAt), default: 0] += e.amount }
        let byDay = (1...key.daysInMonth(calendar: calendar)).map { DayTotal(day: $0, total: perDay[$0] ?? 0) }

        let savings = plans.filter { $0.isDue(in: key, calendar: calendar) }.reduce(0) { $0 + $1.monthlyAmount }

        let limit: Double?
        if settings.monthlyBudget > 0 {
            limit = settings.monthlyBudget
        } else if settings.monthlyIncome > 0 {
            limit = max(0, settings.monthlyIncome - savings)
        } else {
            limit = nil
        }

        return MonthSummary(
            key: key,
            spentAll: inMonth.reduce(0) { $0 + $1.amount },
            spent: shown.reduce(0) { $0 + $1.amount },
            count: shown.count,
            impulse: shown.filter(\.isImpulse).reduce(0) { $0 + $1.amount },
            byCategory: categoryTotals(shown),
            byDay: byDay,
            entries: shown,
            savingsCommitted: savings,
            limit: limit
        )
    }

    static func yearSummary(year: Int,
                            entries: [SpendEntry],
                            plans: [SavingsPlan],
                            filter: SpendFilter = SpendFilter(),
                            calendar: Calendar = .current) -> YearSummary {
        let inYear = entries.filter { calendar.component(.year, from: $0.createdAt) == year }.filter(filter.matches)
        var byMonth = [Double](repeating: 0, count: 12)
        for e in inYear { byMonth[calendar.component(.month, from: e.createdAt) - 1] += e.amount }
        let savings = (1...12).map { m in
            plans.filter { $0.isDue(in: MonthKey(year: year, month: m), calendar: calendar) }
                 .reduce(0) { $0 + $1.monthlyAmount }
        }
        return YearSummary(year: year, total: byMonth.reduce(0, +),
                           byCategory: categoryTotals(inYear), byMonth: byMonth, savingsByMonth: savings)
    }

    static func report(year: Int,
                       entries: [SpendEntry],
                       plans: [SavingsPlan],
                       categories: [String],
                       calendar: Calendar = .current) -> YearlyReport {
        let inYear = entries.filter { calendar.component(.year, from: $0.createdAt) == year }
        var cats = categories
        for e in inYear where !cats.contains(e.category) { cats.append(e.category) }

        var matrix = [[Double]](repeating: [Double](repeating: 0, count: cats.count), count: 12)
        for e in inYear {
            let m = calendar.component(.month, from: e.createdAt) - 1
            if let c = cats.firstIndex(of: e.category) { matrix[m][c] += e.amount }
        }
        let ys = yearSummary(year: year, entries: entries, plans: plans, calendar: calendar)
        return YearlyReport(year: year, categories: cats, months: matrix,
                            spendTotals: matrix.map { $0.reduce(0, +) },
                            savings: ys.savingsByMonth, generatedAt: Date())
    }
}
