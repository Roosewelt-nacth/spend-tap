// Run on the Mac, no simulator needed:
//   swiftc -o check SpendTap/Models/SpendEntry.swift SpendTap/Models/SavingsPlan.swift \
//     SpendTap/Models/AppSettings.swift SpendTap/Models/Analytics.swift checks/main.swift && ./check
import Foundation

var cal = Calendar(identifier: .gregorian)
cal.timeZone = TimeZone(identifier: "UTC")!

var failures = 0
func check(_ ok: Bool, _ msg: String) {
    if ok { print("PASS  \(msg)") } else { print("FAIL  \(msg)"); failures += 1 }
}
func d(_ y: Int, _ m: Int, _ day: Int) -> Date {
    cal.date(from: DateComponents(year: y, month: m, day: day, hour: 12))!
}

let entries = [
    SpendEntry(amount: 100, category: "petrol", createdAt: d(2026, 11, 3)),
    SpendEntry(amount: 250, category: "family", paymentMethod: "cash", createdAt: d(2026, 11, 3)),
    SpendEntry(amount: 50, category: "friends", isImpulse: true, createdAt: d(2026, 11, 28)),
    SpendEntry(amount: 400, category: "petrol", createdAt: d(2026, 10, 30)),   // other month
    SpendEntry(amount: 70, category: "food", createdAt: d(2025, 12, 31)),      // other year
]
let sip = SavingsPlan(name: "Index fund", kind: .sip, monthlyAmount: 5000, dayOfMonth: 5,
                      startDate: d(2026, 9, 1), expectedReturnPct: 12)
var settings = AppSettings()
settings.monthlyIncome = 50000

let nov = MonthKey(year: 2026, month: 11)
let s = Analytics.summary(for: nov, entries: entries, plans: [sip], settings: settings, calendar: cal)

check(s.spentAll == 400, "November total ignores other months (got \(s.spentAll))")
check(s.count == 3, "November has 3 entries (got \(s.count))")
check(s.savingsCommitted == 5000, "SIP due in November")
check(s.limit == 45000, "limit = income - SIP when no budget (got \(String(describing: s.limit)))")
check(s.remaining == 44600, "remaining = 45000 - 400")
check(s.byCategory.first?.category == "family", "biggest category first")
check(abs(s.byCategory.map(\.share).reduce(0, +) - 1) < 1e-9, "category shares sum to 1")
check(s.byDay.count == 30, "November has 30 day buckets")
check(s.byDay[2].total == 350, "day 3 total = 350")
check(s.impulse == 50, "impulse total")

var f = SpendFilter(); f.categories = ["petrol"]
let sf = Analytics.summary(for: nov, entries: entries, plans: [sip], settings: settings, filter: f, calendar: cal)
check(sf.spent == 100 && sf.spentAll == 400, "filter changes charts, not the hero total")

var f2 = SpendFilter(); f2.paymentMethod = "cash"
check(Analytics.summary(for: nov, entries: entries, plans: [], settings: settings, filter: f2, calendar: cal).spent == 250, "cash filter")

settings.monthlyBudget = 10000
check(Analytics.summary(for: nov, entries: entries, plans: [sip], settings: settings, calendar: cal).limit == 10000, "explicit budget wins")

// SIP schedule: started September, pays on the 5th
check(sip.isDue(in: MonthKey(year: 2026, month: 8), calendar: cal) == false, "SIP not due before start")
check(sip.isDue(in: MonthKey(year: 2026, month: 9), calendar: cal), "SIP due from start month")
check(sip.monthsContributed(asOf: d(2026, 11, 28), calendar: cal) == 3, "Sep, Oct, Nov = 3 instalments")
check(sip.monthsContributed(asOf: d(2026, 11, 2), calendar: cal) == 2, "Nov 2 is before the 5th, so 2")
check(sip.contributed(asOf: d(2026, 11, 28), calendar: cal) == 15000, "contributed 15000")
var ended = sip; ended.endDate = d(2026, 10, 15)
check(ended.monthsContributed(asOf: d(2026, 11, 28), calendar: cal) == 2, "ended plan stops counting")
check(ended.isDue(in: nov, calendar: cal) == false, "ended plan not due after end")
let proj = sip.projectedValue(years: 1, asOf: d(2026, 11, 28), calendar: cal)!
check(proj > 15000 + 5000 * 12, "projection beats plain sum (got \(Int(proj)))")

// Year view and export report
let y = Analytics.yearSummary(year: 2026, entries: entries, plans: [sip], calendar: cal)
check(y.total == 800, "2026 total = 100+250+50+400 (got \(y.total))")
check(y.byMonth[9] == 400 && y.byMonth[10] == 400, "Oct and Nov buckets")
check(y.savingsByMonth[7] == 0 && y.savingsByMonth[8] == 5000, "savings per month")
let r = Analytics.report(year: 2026, entries: entries, plans: [sip], categories: ["family", "petrol"], calendar: cal)
check(r.categories == ["family", "petrol", "friends"], "unknown categories appended (got \(r.categories))")
check(r.months.count == 12 && r.months[10].count == 3, "12 x categories matrix")
check(r.spendTotals[10] == 400, "report month total")

// Old settings file without new keys still loads
let old = #"{"monthlyIncome": 1000}"#.data(using: .utf8)!
let decoded = try! JSONDecoder().decode(AppSettings.self, from: old)
check(decoded.monthlyIncome == 1000 && decoded.sheetsURL == "", "old settings json still decodes")

print(failures == 0 ? "\nALL CHECKS PASSED" : "\n\(failures) FAILED")
exit(failures == 0 ? 0 : 1)
