package com.schoolos.attendance.data.remote

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.schoolos.attendance.data.local.entities.AttendanceLogEntity
import com.schoolos.attendance.data.local.entities.EnrolledPersonEntity
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

data class ActivationRequest(
    val profile_code: String,
    val staff_reg_no: String,
    val device_name: String,
    val device_model: String
)

data class ActivationResponse(
    val success: Boolean,
    val message: String?,
    val data: ActivationData?
)

data class ActivationData(
    val terminal_id: String,
    val terminal_name: String,
    val tenant_id: String,
    val school_name: String,
    val campus_name: String,
    val api_key: String,
    val direction: String,
    val attendance_policy: String
)

data class SyncBatchRequest(
    val terminal_id: String,
    val logs: List<AttendanceLogEntity>
)

data class SyncBatchResponse(
    val success: Boolean,
    val synced_ids: List<String>?,
    val message: String?
)

data class EnrolmentDeltaResponse(
    val success: Boolean,
    val timestamp: Long,
    val persons: List<EnrolledPersonDto>?
)

data class EnrolledPersonDto(
    val id: String,
    val tenant_id: String,
    val person_type: String,
    val identifier: String,
    val full_name: String,
    val classroom_or_dept: String?,
    val rfid_card_uid: String?,
    val face_embedding: List<Float>?,
    val photo_url: String?,
    val is_active: Boolean
)

class AttendanceApiService(private val baseUrl: String = "http://schos.test/api/v1") {

    private val gson = Gson()
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .build()

    fun activateTerminal(requestDto: ActivationRequest): ActivationResponse {
        val json = gson.toJson(requestDto)
        val request = Request.Builder()
            .url("$baseUrl/attendance/terminals/activate")
            .post(json.toRequestBody(jsonMediaType))
            .addHeader("Accept", "application/json")
            .build()

        client.newCall(request).execute().use { response ->
            val body = response.body?.string() ?: throw IOException("Empty response")
            return gson.fromJson(body, ActivationResponse::class.java)
        }
    }

    fun syncAttendanceLogs(apiKey: String, terminalId: String, logs: List<AttendanceLogEntity>): SyncBatchResponse {
        val payload = SyncBatchRequest(terminal_id = terminalId, logs = logs)
        val json = gson.toJson(payload)
        val request = Request.Builder()
            .url("$baseUrl/attendance/terminals/sync")
            .post(json.toRequestBody(jsonMediaType))
            .addHeader("Accept", "application/json")
            .addHeader("X-Terminal-Key", apiKey)
            .build()

        client.newCall(request).execute().use { response ->
            val body = response.body?.string() ?: throw IOException("Empty response")
            return gson.fromJson(body, SyncBatchResponse::class.java)
        }
    }

    fun fetchEnrolmentDeltas(apiKey: String, sinceTimestamp: Long): EnrolmentDeltaResponse {
        val request = Request.Builder()
            .url("$baseUrl/attendance/terminals/enrolments?since=$sinceTimestamp")
            .get()
            .addHeader("Accept", "application/json")
            .addHeader("X-Terminal-Key", apiKey)
            .build()

        client.newCall(request).execute().use { response ->
            val body = response.body?.string() ?: throw IOException("Empty response")
            return gson.fromJson(body, EnrolmentDeltaResponse::class.java)
        }
    }

    fun sendHeartbeat(apiKey: String, terminalId: String, diagnosticsJson: String): Boolean {
        val request = Request.Builder()
            .url("$baseUrl/attendance/terminals/heartbeat")
            .post(diagnosticsJson.toRequestBody(jsonMediaType))
            .addHeader("Accept", "application/json")
            .addHeader("X-Terminal-Key", apiKey)
            .build()

        return try {
            client.newCall(request).execute().use { it.isSuccessful }
        } catch (_: Exception) {
            false
        }
    }
}
