package com.schoolos.attendance

import android.app.Application
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.schoolos.attendance.data.local.AttendanceDatabase
import com.schoolos.attendance.data.sync.AttendanceSyncWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class AttendanceApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // 1. Initialize Local Database Instance
        CoroutineScope(Dispatchers.IO).launch {
            AttendanceDatabase.getInstance(this@AttendanceApplication)
        }

        // 2. Schedule Periodic Offline Sync WorkManager (Every 15 Minutes)
        schedulePeriodicSync()
    }

    private fun schedulePeriodicSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val syncRequest = PeriodicWorkRequestBuilder<AttendanceSyncWorker>(15, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "SchoolOsAttendanceSync",
            ExistingPeriodicWorkPolicy.KEEP,
            syncRequest
        )
    }
}
