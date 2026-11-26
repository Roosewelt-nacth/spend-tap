package com.austin.spendtap

import android.app.Application
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.austin.spendtap.data.AppDatabase
import com.austin.spendtap.data.SettingsStore
import com.austin.spendtap.data.SpendRepository
import com.austin.spendtap.export.ExportWorker
import java.util.concurrent.TimeUnit

class SpendApp : Application() {
    // Plain manual wiring: one database, one repository, shared by screens, tile and worker.
    val repo: SpendRepository by lazy { SpendRepository(AppDatabase.create(this), SettingsStore(this)) }

    override fun onCreate() {
        super.onCreate()
        val request = PeriodicWorkRequestBuilder<ExportWorker>(1, TimeUnit.DAYS)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()
        WorkManager.getInstance(this)
            .enqueueUniquePeriodicWork("yearly-sheet-export", ExistingPeriodicWorkPolicy.KEEP, request)
    }
}
