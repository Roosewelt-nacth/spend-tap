package com.austin.spendtap.model

import kotlinx.serialization.Serializable
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId
import java.util.UUID
import kotlin.math.pow

// Pure Kotlin, no Android classes, so it runs as a plain JVM unit test.
// Mirrors the iOS Swift models in ios-app/SpendTap/Models.

/** Mirrors the Supabase `entries` row. `createdAt` is epoch milliseconds. */
data class SpendEntry(
    val id: String = UUID.randomUUID().toString(),
    val amount: Double,
    val category: String,
    val note: String = "",
    val paymentMethod: String = "upi",   // "upi" | "cash"
    val isImpulse: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
)

data class MonthKey(val year: Int, val month: Int) : Comparable<MonthKey> {
    val id: Int get() = year * 100 + month

    override fun compareTo(other: MonthKey) = id.compareTo(other.id)

    fun adding(months: Int): MonthKey {
        val total = year * 12 + (month - 1) + months
        return MonthKey(Math.floorDiv(total, 12), Math.floorMod(total, 12) + 1)
    }

    fun daysInMonth(): Int = YearMonth.of(year, month).lengthOfMonth()

    companion object {
        fun of(millis: Long, zone: ZoneId = ZoneId.systemDefault()): MonthKey {
            val d = Instant.ofEpochMilli(millis).atZone(zone)
            return MonthKey(d.year, d.monthValue)
        }
        fun now(zone: ZoneId = ZoneId.systemDefault()) = of(System.currentTimeMillis(), zone)
    }
}

enum class SavingsKind(val title: String) {
    SIP("SIP / Mutual fund"),
    RD("Recurring deposit"),
    FD("Fixed deposit"),
    PPF("PPF / EPF / NPS"),
    GOLD("Gold"),
    OTHER("Other savings"),
}

/** A recurring monthly commitment. Counted against income before spending. */
data class SavingsPlan(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val kind: SavingsKind = SavingsKind.SIP,
    val monthlyAmount: Double,
    val dayOfMonth: Int = 5,
    val startDate: Long = System.currentTimeMillis(),
    val endDate: Long? = null,
    val targetAmount: Double? = null,
    val expectedReturnPct: Double = 0.0,
    val isActive: Boolean = true,
) {
    fun isDue(key: MonthKey, zone: ZoneId = ZoneId.systemDefault()): Boolean {
        if (!isActive) return false
        if (key < MonthKey.of(startDate, zone)) return false
        if (endDate != null && key > MonthKey.of(endDate, zone)) return false
        return true
    }

    /** How many monthly instalments have already left the account. */
    fun monthsContributed(now: Long = System.currentTimeMillis(), zone: ZoneId = ZoneId.systemDefault()): Int {
        val start = MonthKey.of(startDate, zone)
        var last = MonthKey.of(now, zone)
        var lastIsCurrent = true
        if (endDate != null) {
            val endKey = MonthKey.of(endDate, zone)
            if (endKey < last) { last = endKey; lastIsCurrent = false }
        }
        val n = (last.year - start.year) * 12 + (last.month - start.month) + 1
        if (n <= 0) return 0
        val today = Instant.ofEpochMilli(now).atZone(zone).dayOfMonth
        return if (lastIsCurrent && today < dayOfMonth) maxOf(0, n - 1) else n
    }

    fun contributed(now: Long = System.currentTimeMillis(), zone: ZoneId = ZoneId.systemDefault()): Double =
        monthsContributed(now, zone) * monthlyAmount

    /** Rough future value; an estimate for planning, not a promise or advice. */
    fun projectedValue(years: Int, now: Long = System.currentTimeMillis(), zone: ZoneId = ZoneId.systemDefault()): Double? {
        if (expectedReturnPct <= 0 || years <= 0) return null
        val r = expectedReturnPct / 100 / 12
        val growth = (1 + r).pow(years * 12)
        val future = monthlyAmount * ((growth - 1) / r) * (1 + r)
        return contributed(now, zone) * growth + future
    }

    fun progress(now: Long = System.currentTimeMillis(), zone: ZoneId = ZoneId.systemDefault()): Double? {
        val t = targetAmount ?: return null
        if (t <= 0) return null
        return minOf(1.0, contributed(now, zone) / t)
    }
}

/** Defaults on every field: an older saved file with missing keys still loads. */
@Serializable
data class AppSettings(
    val monthlyIncome: Double = 0.0,
    val monthlyBudget: Double = 0.0,      // 0 = derive from income minus savings
    val sheetsUrl: String = "",
    val sheetsToken: String = "",
    val lastAutoExportedYear: Int = 0,
)

data class SpendFilter(
    val categories: Set<String> = emptySet(),   // empty = all
    val paymentMethod: String? = null,
    val impulseOnly: Boolean = false,
) {
    val isActive: Boolean get() = categories.isNotEmpty() || paymentMethod != null || impulseOnly

    fun matches(e: SpendEntry): Boolean {
        if (categories.isNotEmpty() && e.category !in categories) return false
        if (paymentMethod != null && e.paymentMethod != paymentMethod) return false
        if (impulseOnly && !e.isImpulse) return false
        return true
    }
}

data class CategoryTotal(val category: String, val total: Double, val count: Int, val share: Double)

data class DayTotal(val day: Int, val total: Double)

data class MonthSummary(
    val key: MonthKey,
    val spentAll: Double,                // whole month, ignores filters (drives the hero ring)
    val spent: Double,                   // after filters
    val count: Int,
    val impulse: Double,
    val byCategory: List<CategoryTotal>,
    val byDay: List<DayTotal>,
    val entries: List<SpendEntry>,       // filtered, newest first
    val savingsCommitted: Double,
    val limit: Double?,
) {
    val remaining: Double? get() = limit?.let { it - spentAll }
    val progress: Double get() = if (limit != null && limit > 0) spentAll / limit else 0.0
}

data class YearSummary(
    val year: Int,
    val total: Double,
    val byCategory: List<CategoryTotal>,
    val byMonth: List<Double>,           // 12 values
    val savingsByMonth: List<Double>,    // 12 values
)

/** Uploaded to the user's Google Sheet once a year. Same JSON as the iOS app sends. */
@Serializable
data class YearlyReport(
    val year: Int,
    val categories: List<String>,
    val months: List<List<Double>>,      // 12 rows x categories
    val spendTotals: List<Double>,
    val savings: List<Double>,
    val generatedAt: String,
)
