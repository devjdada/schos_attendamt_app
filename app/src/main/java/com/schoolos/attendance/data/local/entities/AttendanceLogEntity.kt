package com.schoolos.attendance.data.local.entities

data class AttendanceLogEntity(
    val id: String, // UUID
    val tenantId: String,
    val terminalId: String,
    val personId: String,
    val personType: String, // "student" or "staff"
    val identifier: String,
    val fullName: String,
    val classroomOrDept: String?,
    val verificationMode: String, // "face", "fingerprint", "rfid", "rfid_plus_face"
    val direction: String, // "in" or "out"
    val timestamp: Long,
    val confidenceScore: Float,
    val syncStatus: String = "pending" // "pending", "synced", "failed"
)
