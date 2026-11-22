package com.schoolos.attendance.data.local.entities

data class EnrolledPersonEntity(
    val id: String,
    val tenantId: String,
    val personType: String, // "student" or "staff"
    val identifier: String, // Admission Number or Staff Reg No
    val fullName: String,
    val classroomOrDept: String?,
    val rfidCardUid: String?,
    val faceEmbedding: FloatArray?,
    val fingerprintTemplate: ByteArray?,
    val photoBase64: String?,
    val isActive: Boolean = true,
    val updatedAt: Long = System.currentTimeMillis()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as EnrolledPersonEntity
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()
}
