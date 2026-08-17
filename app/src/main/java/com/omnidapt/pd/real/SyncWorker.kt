package com.omnidapt.pd.real

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class SyncWorker(
    context: Context,
    parameters: WorkerParameters,
) : CoroutineWorker(context, parameters) {
    override suspend fun doWork(): Result {
        val repository = (applicationContext as OminidaptApplication).realRepository
        return runCatching {
            repository.syncPending()
            if (repository.pendingCount() == 0) Result.success() else Result.retry()
        }.getOrElse { Result.retry() }
    }
}
