package com.austin.spendtap.data

import com.austin.spendtap.export.SheetsExporter
import com.austin.spendtap.model.Analytics
import com.austin.spendtap.model.AppSettings
import com.austin.spendtap.model.SavingsPlan
import com.austin.spendtap.model.SpendEntry
import com.austin.spendtap.ui.SpendCategory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.ZoneId

class SpendRepository(private val db: AppDatabase, private val settingsStore: SettingsStore) {
    val entries: Flow<List<SpendEntry>> = db.entryDao().all().map { list -> list.map { it.toModel() } }
    val plans: Flow<List<SavingsPlan>> = db.planDao().all().map { list -> list.map { it.toModel() } }
    val settings: Flow<AppSettings> = settingsStore.flow

    suspend fun add(entry: SpendEntry) = db.entryDao().upsert(entry.toEntity())
    suspend fun delete(entry: SpendEntry) = db.entryDao().delete(entry.id)
    suspend fun upsert(plan: SavingsPlan) = db.planDao().upsert(plan.toEntity())
    suspend fun delete(plan: SavingsPlan) = db.planDao().delete(plan.id)
    suspend fun saveSettings(settings: AppSettings) = settingsStore.save(settings)

    suspend fun exportYear(year: Int): Result<Unit> {
        val s = settings.first()
        if (s.sheetsUrl.isBlank()) return Result.failure(IllegalStateException("Add your Google Sheet link in Settings first."))
        val report = Analytics.report(year, entries.first(), plans.first(), SpendCategory.entries.map { it.key })
        return SheetsExporter.send(report, s.sheetsUrl, s.sheetsToken)
    }

    /**
     * Called daily by ExportWorker. Uploads last year once, the first time it runs in a new year.
     * Returns true when there is nothing left to do (so the worker should not retry).
     */
    suspend fun autoExportIfNeeded(): Boolean {
        val s = settings.first()
        val lastYear = java.time.LocalDate.now().year - 1
        if (s.sheetsUrl.isBlank() || s.lastAutoExportedYear >= lastYear) return true
        val zone = ZoneId.systemDefault()
        val hasData = entries.first().any { Instant.ofEpochMilli(it.createdAt).atZone(zone).year == lastYear }
        if (!hasData) return true
        val result = exportYear(lastYear)
        if (result.isSuccess) saveSettings(s.copy(lastAutoExportedYear = lastYear))
        return result.isSuccess
    }
}
