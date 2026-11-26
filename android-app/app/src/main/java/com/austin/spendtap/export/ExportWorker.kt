package com.austin.spendtap.export

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.austin.spendtap.SpendApp

/** Runs about once a day (when online). It only does work in the first run of a new year. */
class ExportWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val repo = (applicationContext as SpendApp).repo
        return if (repo.autoExportIfNeeded()) Result.success() else Result.retry()
    }
}
