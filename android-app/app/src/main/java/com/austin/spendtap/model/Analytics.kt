package com.austin.spendtap.model

import java.time.Instant
import java.time.ZoneId

object Analytics {
    private fun day(millis: Long, zone: ZoneId) = Instant.ofEpochMilli(millis).atZone(zone).dayOfMonth
    private fun yearOf(millis: Long, zone: ZoneId) = Instant.ofEpochMilli(millis).atZone(zone).year
    private fun monthOf(millis: Long, zone: ZoneId) = Instant.ofEpochMilli(millis).atZone(zone).monthValue

    fun categoryTotals(entries: List<SpendEntry>): List<CategoryTotal> {
        val grand = entries.sumOf { it.amount }
        return entries.groupBy { it.category }
            .map { (cat, list) ->
                val total = list.sumOf { it.amount }
                CategoryTotal(cat, total, list.size, if (grand > 0) total / grand else 0.0)
            }
            .sortedByDescending { it.total }
    }

    fun summary(
        key: MonthKey,
        entries: List<SpendEntry>,
        plans: List<SavingsPlan>,
        settings: AppSettings,
        filter: SpendFilter = SpendFilter(),
        zone: ZoneId = ZoneId.systemDefault(),
    ): MonthSummary {
        val inMonth = entries.filter { MonthKey.of(it.createdAt, zone) == key }
        val shown = inMonth.filter(filter::matches).sortedByDescending { it.createdAt }

        val perDay = HashMap<Int, Double>()
        for (e in shown) perDay.merge(day(e.createdAt, zone), e.amount, Double::plus)
        val byDay = (1..key.daysInMonth()).map { DayTotal(it, perDay[it] ?: 0.0) }

        val savings = plans.filter { it.isDue(key, zone) }.sumOf { it.monthlyAmount }

        val limit = when {
            settings.monthlyBudget > 0 -> settings.monthlyBudget
            settings.monthlyIncome > 0 -> maxOf(0.0, settings.monthlyIncome - savings)
            else -> null
        }

        return MonthSummary(
            key = key,
            spentAll = inMonth.sumOf { it.amount },
            spent = shown.sumOf { it.amount },
            count = shown.size,
            impulse = shown.filter { it.isImpulse }.sumOf { it.amount },
            byCategory = categoryTotals(shown),
            byDay = byDay,
            entries = shown,
            savingsCommitted = savings,
            limit = limit,
        )
    }

    fun yearSummary(
        year: Int,
        entries: List<SpendEntry>,
        plans: List<SavingsPlan>,
        filter: SpendFilter = SpendFilter(),
        zone: ZoneId = ZoneId.systemDefault(),
    ): YearSummary {
        val inYear = entries.filter { yearOf(it.createdAt, zone) == year }.filter(filter::matches)
        val byMonth = DoubleArray(12)
        for (e in inYear) byMonth[monthOf(e.createdAt, zone) - 1] += e.amount
        val savings = (1..12).map { m ->
            plans.filter { it.isDue(MonthKey(year, m), zone) }.sumOf { it.monthlyAmount }
        }
        return YearSummary(year, byMonth.sum(), categoryTotals(inYear), byMonth.toList(), savings)
    }

    fun report(
        year: Int,
        entries: List<SpendEntry>,
        plans: List<SavingsPlan>,
        categories: List<String>,
        zone: ZoneId = ZoneId.systemDefault(),
    ): YearlyReport {
        val inYear = entries.filter { yearOf(it.createdAt, zone) == year }
        val cats = categories.toMutableList()
        for (e in inYear) if (e.category !in cats) cats.add(e.category)

        val matrix = Array(12) { DoubleArray(cats.size) }
        for (e in inYear) {
            val c = cats.indexOf(e.category)
            if (c >= 0) matrix[monthOf(e.createdAt, zone) - 1][c] += e.amount
        }
        val ys = yearSummary(year, entries, plans, zone = zone)
        return YearlyReport(
            year = year,
            categories = cats,
            months = matrix.map { it.toList() },
            spendTotals = matrix.map { it.sum() },
            savings = ys.savingsByMonth,
            generatedAt = Instant.now().toString(),
        )
    }
}
