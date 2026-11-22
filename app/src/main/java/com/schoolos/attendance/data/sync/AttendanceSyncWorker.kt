package com.schoolos.attendance.data.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.schoolos.attendance.data.local.AttendanceDatabase
import com.schoolos.attendance.data.local.entities.EnrolledPersonEntity
import com.schoolos.attendance.data.remote.AttendanceApiService
import com.schoolos.attendance.data.repository.DeviceActivationManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AttendanceSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val context = applicationContext
        val db = AttendanceDatabase.getInstance(context)
        val activationManager = DeviceActivationManager(context)
        val creds = activationManager.getCredentials()

        if (!creds.isActivated || creds.apiKey == null || creds.terminalId == null) {
            return@withContext Result.success()
        }

        val apiService = AttendanceApiService()

        try {
            // 1. Flush Pending Attendance Logs to Server
            val pendingLogs = db.getPendingAttendanceLogs(limit = 100)
            if (pendingLogs.isNotEmpty()) {
                val syncResponse = apiService.syncAttendanceLogs(
                    apiKey = creds.apiKey,
                    terminalId = creds.terminalId,
                    logs = pendingLogs
                )
                if (syncResponse.success && !syncResponse.synced_ids.isNullOrEmpty()) {
                    db.markLogsAsSynced(syncResponse.synced_ids)
                }
            }

            // 2. Pull Delta Enrolments from Server
            val lastUpdate = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000L) // Default 7 days
            val enrolmentDelta = apiService.fetchEnrolmentDeltas(
                apiKey = creds.apiKey,
                sinceTimestamp = lastUpdate
            )

            if (enrolmentDelta.success && !enrolmentDelta.persons.isNullOrEmpty()) {
                for (p in enrolmentDelta.persons) {
                    val embeddingArray = p.face_embedding?.toFloatArray()
                    val entity = EnrolledPersonEntity(
                        id = p.id,
                        tenantId = p.tenant_id,
                        personType = p.person_type,
                        identifier = p.identifier,
                        fullName = p.full_name,
                        classroomOrDept = p.classroom_or_dept,
                        rfidCardUid = p.rfid_card_uid,
                        faceEmbedding = embeddingArray,
                        fingerprintTemplate = null,
                        photoBase64 = null,
                        isActive = p.is_active,
                        updatedAt = System.currentTimeMillis()
                    )
                    db.upsertPerson(entity)
                }
            }

            Result.success()
        } catch (e: Exception) {
            // Retry on network failure with exponential backoff
            Result.retry()
        }
    }
}
