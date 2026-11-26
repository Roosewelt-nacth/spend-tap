package com.austin.spendtap

import com.austin.spendtap.model.*
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneOffset

// Same checks as ios-app/checks/main.swift. Run with: gradlew test
class AnalyticsTest {
    private val utc = ZoneOffset.UTC

    private fun d(y: Int, m: Int, day: Int): Long =
        LocalDateTime.of(y, m, day, 12, 0).toInstant(ZoneOffset.UTC).toEpochMilli()

    private val entries = listOf(
        SpendEntry(amount = 100.0, category = "petrol", createdAt = d(2026, 11, 3)),
        SpendEntry(amount = 250.0, category = "family", paymentMethod = "cash", createdAt = d(2026, 11, 3)),
        SpendEntry(amount = 50.0, category = "friends", isImpulse = true, createdAt = d(2026, 11, 28)),
        SpendEntry(amount = 400.0, category = "petrol", createdAt = d(2026, 10, 30)),   // other month
        SpendEntry(amount = 70.0, category = "food", createdAt = d(2025, 12, 31)),      // other year
    )
    private val sip = SavingsPlan(name = "Index fund", monthlyAmount = 5000.0, dayOfMonth = 5,
        startDate = d(2026, 9, 1), expectedReturnPct = 12.0)
    private val settings = AppSettings(monthlyIncome = 50000.0)
    private val nov = MonthKey(2026, 11)

    private fun summary(filter: SpendFilter = SpendFilter(), s: AppSettings = settings, plans: List<SavingsPlan> = listOf(sip)) =
        Analytics.summary(nov, entries, plans, s, filter, utc)

    @Test fun novemberTotalsIgnoreOtherMonths() {
        val s = summary()
        assertEquals(400.0, s.spentAll, 1e-9)
        assertEquals(3, s.count)
        assertEquals(5000.0, s.savingsCommitted, 1e-9)
        assertEquals(45000.0, s.limit!!, 1e-9)
        assertEquals(44600.0, s.remaining!!, 1e-9)
        assertEquals("family", s.byCategory.first().category)
        assertEquals(1.0, s.byCategory.sumOf { it.share }, 1e-9)
        assertEquals(30, s.byDay.size)
        assertEquals(350.0, s.byDay[2].total, 1e-9)
        assertEquals(50.0, s.impulse, 1e-9)
    }

    @Test fun filtersChangeChartsNotHeroTotal() {
        val sf = summary(SpendFilter(categories = setOf("petrol")))
        assertEquals(100.0, sf.spent, 1e-9)
        assertEquals(400.0, sf.spentAll, 1e-9)
        assertEquals(250.0, summary(SpendFilter(paymentMethod = "cash")).spent, 1e-9)
    }

    @Test fun explicitBudgetWins() {
        assertEquals(10000.0, summary(s = settings.copy(monthlyBudget = 10000.0)).limit!!, 1e-9)
    }

    @Test fun sipSchedule() {
        assertFalse(sip.isDue(MonthKey(2026, 8), utc))
        assertTrue(sip.isDue(MonthKey(2026, 9), utc))
        assertEquals(3, sip.monthsContributed(d(2026, 11, 28), utc))
        assertEquals(2, sip.monthsContributed(d(2026, 11, 2), utc))   // before the 5th
        assertEquals(15000.0, sip.contributed(d(2026, 11, 28), utc), 1e-9)
        val ended = sip.copy(endDate = d(2026, 10, 15))
        assertEquals(2, ended.monthsContributed(d(2026, 11, 28), utc))
        assertFalse(ended.isDue(nov, utc))
        val proj = sip.projectedValue(1, d(2026, 11, 28), utc)!!
        assertTrue("projection beats plain sum (got $proj)", proj > 15000 + 5000 * 12)
    }

    @Test fun yearViewAndReport() {
        val y = Analytics.yearSummary(2026, entries, listOf(sip), zone = utc)
        assertEquals(800.0, y.total, 1e-9)
        assertEquals(400.0, y.byMonth[9], 1e-9)
        assertEquals(400.0, y.byMonth[10], 1e-9)
        assertEquals(0.0, y.savingsByMonth[7], 1e-9)
        assertEquals(5000.0, y.savingsByMonth[8], 1e-9)

        val r = Analytics.report(2026, entries, listOf(sip), listOf("family", "petrol"), utc)
        assertEquals(listOf("family", "petrol", "friends"), r.categories)
        assertEquals(12, r.months.size)
        assertEquals(3, r.months[10].size)
        assertEquals(400.0, r.spendTotals[10], 1e-9)
    }

    @Test fun oldSettingsJsonStillDecodes() {
        val s = Json { ignoreUnknownKeys = true }.decodeFromString<AppSettings>("""{"monthlyIncome": 1000.0}""")
        assertEquals(1000.0, s.monthlyIncome, 1e-9)
        assertEquals("", s.sheetsUrl)
    }

    @Test fun monthKeyArithmetic() {
        assertEquals(MonthKey(2027, 1), MonthKey(2026, 12).adding(1))
        assertEquals(MonthKey(2025, 12), MonthKey(2026, 1).adding(-1))
        assertEquals(MonthKey(2027, 11), MonthKey(2026, 11).adding(12))
    }
}
