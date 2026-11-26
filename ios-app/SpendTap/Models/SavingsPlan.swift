import Foundation

enum SavingsKind: String, Codable, CaseIterable {
    case sip, rd, fd, ppf, gold, other

    var title: String {
        switch self {
        case .sip: "SIP / Mutual fund"
        case .rd: "Recurring deposit"
        case .fd: "Fixed deposit"
        case .ppf: "PPF / EPF / NPS"
        case .gold: "Gold"
        case .other: "Other savings"
        }
    }

    var symbol: String {
        switch self {
        case .sip: "chart.line.uptrend.xyaxis"
        case .rd: "calendar.badge.clock"
        case .fd: "lock.fill"
        case .ppf: "building.columns.fill"
        case .gold: "circle.hexagongrid.fill"
        case .other: "banknote.fill"
        }
    }
}

/// A recurring monthly commitment (SIP, RD, ...). It is counted against the
/// month's income *before* spending, so "safe to spend" stays honest.
struct SavingsPlan: Codable, Identifiable, Equatable {
    var id: UUID = UUID()
    var name: String
    var kind: SavingsKind = .sip
    var monthlyAmount: Double
    var dayOfMonth: Int = 5            // day the money leaves the account
    var startDate: Date = Date()
    var endDate: Date? = nil
    var targetAmount: Double? = nil    // optional goal
    var expectedReturnPct: Double = 0  // yearly %, 0 = don't project
    var isActive: Bool = true

    /// Is this plan charged in the given month?
    func isDue(in key: MonthKey, calendar: Calendar = .current) -> Bool {
        guard isActive else { return false }
        if key < MonthKey(startDate, calendar: calendar) { return false }
        if let end = endDate, key > MonthKey(end, calendar: calendar) { return false }
        return true
    }

    /// How many monthly instalments have already left the account.
    func monthsContributed(asOf now: Date = Date(), calendar: Calendar = .current) -> Int {
        let start = MonthKey(startDate, calendar: calendar)
        var last = MonthKey(now, calendar: calendar)
        var lastIsCurrent = true
        if let end = endDate {
            let endKey = MonthKey(end, calendar: calendar)
            if endKey < last { last = endKey; lastIsCurrent = false }
        }
        let n = (last.year - start.year) * 12 + (last.month - start.month) + 1
        guard n > 0 else { return 0 }
        if lastIsCurrent && calendar.component(.day, from: now) < dayOfMonth {
            return max(0, n - 1)
        }
        return n
    }

    func contributed(asOf now: Date = Date(), calendar: Calendar = .current) -> Double {
        Double(monthsContributed(asOf: now, calendar: calendar)) * monthlyAmount
    }

    /// Rough future value if you keep paying for `years` more years.
    /// Treats what is already contributed as a lump sum (ignores its past growth).
    /// An estimate for planning only, not a promise or advice.
    func projectedValue(years: Int, asOf now: Date = Date(), calendar: Calendar = .current) -> Double? {
        guard expectedReturnPct > 0, years > 0 else { return nil }
        let r = expectedReturnPct / 100 / 12
        let n = Double(years * 12)
        let growth = pow(1 + r, n)
        let futureInstalments = monthlyAmount * ((growth - 1) / r) * (1 + r)
        return contributed(asOf: now, calendar: calendar) * growth + futureInstalments
    }

    var progress: Double? {
        guard let t = targetAmount, t > 0 else { return nil }
        return min(1, contributed() / t)
    }
}
