package com.schoolos.attendance.data.local.entities

data class SyncQueueEntity(
    val id: String,
    val payloadJson: String,
    val retryCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)
