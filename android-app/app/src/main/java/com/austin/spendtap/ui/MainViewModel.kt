package com.austin.spendtap.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.austin.spendtap.SpendApp
import com.austin.spendtap.model.Analytics
import com.austin.spendtap.model.AppSettings
import com.austin.spendtap.model.MonthKey
import com.austin.spendtap.model.MonthSummary
import com.austin.spendtap.model.SavingsPlan
import com.austin.spendtap.model.SpendEntry
import com.austin.spendtap.model.SpendFilter
import com.austin.spendtap.model.YearSummary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Screens read StateFlows from here and call functions on it. They never do maths or touch the database. */
class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = (app as SpendApp).repo

    val entries: StateFlow<List<SpendEntry>> =
        repo.entries.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val plans: StateFlow<List<SavingsPlan>> =
        repo.plans.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    /** null until DataStore has loaded, so text fields don't start from wrong zeros. */
    val settings: StateFlow<AppSettings?> =
        repo.settings.map<AppSettings, AppSettings?> { it }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val month = MutableStateFlow(MonthKey.now())
    val filter = MutableStateFlow(SpendFilter())

    val summary: StateFlow<MonthSummary> =
        combine(entries, plans, settings, month, filter) { e, p, s, m, f ->
            Analytics.summary(m, e, p, s ?: AppSettings(), f)
        }.stateIn(viewModelScope, SharingStarted.Eagerly,
            Analytics.summary(MonthKey.now(), emptyList(), emptyList(), AppSettings()))

    val yearSummary: StateFlow<YearSummary> =
        combine(entries, plans, month, filter) { e, p, m, f -> Analytics.yearSummary(m.year, e, p, f) }
            .stateIn(viewModelScope, SharingStarted.Eagerly,
                Analytics.yearSummary(MonthKey.now().year, emptyList(), emptyList()))

    val exportStatus = MutableStateFlow("")
    val isExporting = MutableStateFlow(false)

    fun shiftMonth(by: Int) = month.update { it.adding(by) }
    fun setFilter(f: SpendFilter) { filter.value = f }

    fun add(e: SpendEntry) = viewModelScope.launch { repo.add(e) }
    fun delete(e: SpendEntry) = viewModelScope.launch { repo.delete(e) }
    fun upsert(p: SavingsPlan) = viewModelScope.launch { repo.upsert(p) }
    fun delete(p: SavingsPlan) = viewModelScope.launch { repo.delete(p) }

    fun updateSettings(change: (AppSettings) -> AppSettings) {
        val current = settings.value ?: return
        viewModelScope.launch { repo.saveSettings(change(current)) }
    }

    fun exportYear(year: Int) = viewModelScope.launch {
        isExporting.value = true
        exportStatus.value = repo.exportYear(year).fold(
            onSuccess = { "Exported $year to your sheet." },
            onFailure = { "Export failed: ${it.message}" },
        )
        isExporting.value = false
    }
}
