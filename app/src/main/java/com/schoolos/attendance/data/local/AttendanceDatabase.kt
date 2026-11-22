package com.schoolos.attendance.data.local

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.schoolos.attendance.data.local.entities.AttendanceLogEntity
import com.schoolos.attendance.data.local.entities.EnrolledPersonEntity
import com.schoolos.attendance.data.local.entities.SyncQueueEntity

class AttendanceDatabase(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    private val gson = Gson()

    companion object {
        private const val DATABASE_NAME = "schoolos_attendance.db"
        private const val DATABASE_VERSION = 1

        @Volatile
        private var INSTANCE: AttendanceDatabase? = null

        fun getInstance(context: Context): AttendanceDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AttendanceDatabase(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE enrolled_persons (
                id TEXT PRIMARY KEY,
                tenant_id TEXT NOT NULL,
                person_type TEXT NOT NULL,
                identifier TEXT NOT NULL,
                full_name TEXT NOT NULL,
                classroom_or_dept TEXT,
                rfid_card_uid TEXT,
                face_embedding TEXT,
                fingerprint_template BLOB,
                photo_base64 TEXT,
                is_active INTEGER NOT NULL DEFAULT 1,
                updated_at INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX idx_person_identifier ON enrolled_persons(identifier)")
        db.execSQL("CREATE INDEX idx_person_rfid ON enrolled_persons(rfid_card_uid)")
        db.execSQL("CREATE INDEX idx_person_tenant ON enrolled_persons(tenant_id)")

        db.execSQL(
            """
            CREATE TABLE attendance_logs (
                id TEXT PRIMARY KEY,
                tenant_id TEXT NOT NULL,
                terminal_id TEXT NOT NULL,
                person_id TEXT NOT NULL,
                person_type TEXT NOT NULL,
                identifier TEXT NOT NULL,
                full_name TEXT NOT NULL,
                classroom_or_dept TEXT,
                verification_mode TEXT NOT NULL,
                direction TEXT NOT NULL,
                timestamp INTEGER NOT NULL,
                confidence_score REAL NOT NULL,
                sync_status TEXT NOT NULL DEFAULT 'pending'
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX idx_log_sync ON attendance_logs(sync_status)")
        db.execSQL("CREATE INDEX idx_log_timestamp ON attendance_logs(timestamp)")

        db.execSQL(
            """
            CREATE TABLE sync_queue (
                id TEXT PRIMARY KEY,
                payload_json TEXT NOT NULL,
                retry_count INTEGER NOT NULL DEFAULT 0,
                created_at INTEGER NOT NULL
            )
            """.trimIndent()
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS sync_queue")
        db.execSQL("DROP TABLE IF EXISTS attendance_logs")
        db.execSQL("DROP TABLE IF EXISTS enrolled_persons")
        onCreate(db)
    }

    // --- Enrolled Persons CRUD ---

    fun upsertPerson(person: EnrolledPersonEntity) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("id", person.id)
            put("tenant_id", person.tenantId)
            put("person_type", person.personType)
            put("identifier", person.identifier)
            put("full_name", person.fullName)
            put("classroom_or_dept", person.classroomOrDept)
            put("rfid_card_uid", person.rfidCardUid)
            put("face_embedding", person.faceEmbedding?.let { gson.toJson(it) })
            put("fingerprint_template", person.fingerprintTemplate)
            put("photo_base64", person.photoBase64)
            put("is_active", if (person.isActive) 1 else 0)
            put("updated_at", person.updatedAt)
        }
        db.insertWithOnConflict(
            "enrolled_persons",
            null,
            values,
            SQLiteDatabase.CONFLICT_REPLACE
        )
    }

    fun getAllEnrolledPersons(): List<EnrolledPersonEntity> {
        val list = mutableListOf<EnrolledPersonEntity>()
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM enrolled_persons WHERE is_active = 1", null)
        cursor.use {
            val idIdx = it.getColumnIndexOrThrow("id")
            val tenantIdx = it.getColumnIndexOrThrow("tenant_id")
            val typeIdx = it.getColumnIndexOrThrow("person_type")
            val identIdx = it.getColumnIndexOrThrow("identifier")
            val nameIdx = it.getColumnIndexOrThrow("full_name")
            val classIdx = it.getColumnIndexOrThrow("classroom_or_dept")
            val rfidIdx = it.getColumnIndexOrThrow("rfid_card_uid")
            val faceIdx = it.getColumnIndexOrThrow("face_embedding")
            val fpIdx = it.getColumnIndexOrThrow("fingerprint_template")
            val photoIdx = it.getColumnIndexOrThrow("photo_base64")
            val activeIdx = it.getColumnIndexOrThrow("is_active")
            val updateIdx = it.getColumnIndexOrThrow("updated_at")

            while (it.moveToNext()) {
                val embeddingJson = it.getString(faceIdx)
                val embedding: FloatArray? = embeddingJson?.let { json ->
                    gson.fromJson(json, FloatArray::class.java)
                }

                list.add(
                    EnrolledPersonEntity(
                        id = it.getString(idIdx),
                        tenantId = it.getString(tenantIdx),
                        personType = it.getString(typeIdx),
                        identifier = it.getString(identIdx),
                        fullName = it.getString(nameIdx),
                        classroomOrDept = it.getString(classIdx),
                        rfidCardUid = it.getString(rfidIdx),
                        faceEmbedding = embedding,
                        fingerprintTemplate = it.getBlob(fpIdx),
                        photoBase64 = it.getString(photoIdx),
                        isActive = it.getInt(activeIdx) == 1,
                        updatedAt = it.getLong(updateIdx)
                    )
                )
            }
        }
        return list
    }

    fun findPersonByRfid(cardUid: String): EnrolledPersonEntity? {
        val db = readableDatabase
        val cursor = db.rawQuery(
            "SELECT * FROM enrolled_persons WHERE rfid_card_uid = ? AND is_active = 1 LIMIT 1",
            arrayOf(cardUid)
        )
        return cursor.use {
            if (it.moveToFirst()) {
                val embeddingJson = it.getString(it.getColumnIndexOrThrow("face_embedding"))
                val embedding: FloatArray? = embeddingJson?.let { json ->
                    gson.fromJson(json, FloatArray::class.java)
                }
                EnrolledPersonEntity(
                    id = it.getString(it.getColumnIndexOrThrow("id")),
                    tenantId = it.getString(it.getColumnIndexOrThrow("tenant_id")),
                    personType = it.getString(it.getColumnIndexOrThrow("person_type")),
                    identifier = it.getString(it.getColumnIndexOrThrow("identifier")),
                    fullName = it.getString(it.getColumnIndexOrThrow("full_name")),
                    classroomOrDept = it.getString(it.getColumnIndexOrThrow("classroom_or_dept")),
                    rfidCardUid = it.getString(it.getColumnIndexOrThrow("rfid_card_uid")),
                    faceEmbedding = embedding,
                    fingerprintTemplate = it.getBlob(it.getColumnIndexOrThrow("fingerprint_template")),
                    photoBase64 = it.getString(it.getColumnIndexOrThrow("photo_base64")),
                    isActive = it.getInt(it.getColumnIndexOrThrow("is_active")) == 1,
                    updatedAt = it.getLong(it.getColumnIndexOrThrow("updated_at"))
                )
            } else null
        }
    }

    // --- Attendance Logs CRUD ---

    fun insertAttendanceLog(log: AttendanceLogEntity) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("id", log.id)
            put("tenant_id", log.tenantId)
            put("terminal_id", log.terminalId)
            put("person_id", log.personId)
            put("person_type", log.personType)
            put("identifier", log.identifier)
            put("full_name", log.fullName)
            put("classroom_or_dept", log.classroomOrDept)
            put("verification_mode", log.verificationMode)
            put("direction", log.direction)
            put("timestamp", log.timestamp)
            put("confidence_score", log.confidenceScore)
            put("sync_status", log.syncStatus)
        }
        db.insertWithOnConflict("attendance_logs", null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun getPendingAttendanceLogs(limit: Int = 100): List<AttendanceLogEntity> {
        val list = mutableListOf<AttendanceLogEntity>()
        val db = readableDatabase
        val cursor = db.rawQuery(
            "SELECT * FROM attendance_logs WHERE sync_status = 'pending' ORDER BY timestamp ASC LIMIT ?",
            arrayOf(limit.toString())
        )
        cursor.use {
            val idIdx = it.getColumnIndexOrThrow("id")
            val tenantIdx = it.getColumnIndexOrThrow("tenant_id")
            val termIdx = it.getColumnIndexOrThrow("terminal_id")
            val personIdx = it.getColumnIndexOrThrow("person_id")
            val typeIdx = it.getColumnIndexOrThrow("person_type")
            val identIdx = it.getColumnIndexOrThrow("identifier")
            val nameIdx = it.getColumnIndexOrThrow("full_name")
            val classIdx = it.getColumnIndexOrThrow("classroom_or_dept")
            val modeIdx = it.getColumnIndexOrThrow("verification_mode")
            val dirIdx = it.getColumnIndexOrThrow("direction")
            val timeIdx = it.getColumnIndexOrThrow("timestamp")
            val confIdx = it.getColumnIndexOrThrow("confidence_score")
            val syncIdx = it.getColumnIndexOrThrow("sync_status")

            while (it.moveToNext()) {
                list.add(
                    AttendanceLogEntity(
                        id = it.getString(idIdx),
                        tenantId = it.getString(tenantIdx),
                        terminalId = it.getString(termIdx),
                        personId = it.getString(personIdx),
                        personType = it.getString(typeIdx),
                        identifier = it.getString(identIdx),
                        fullName = it.getString(nameIdx),
                        classroomOrDept = it.getString(classIdx),
                        verificationMode = it.getString(modeIdx),
                        direction = it.getString(dirIdx),
                        timestamp = it.getLong(timeIdx),
                        confidenceScore = it.getFloat(confIdx),
                        syncStatus = it.getString(syncIdx)
                    )
                )
            }
        }
        return list
    }

    fun markLogsAsSynced(logIds: List<String>) {
        if (logIds.isEmpty()) return
        val db = writableDatabase
        db.beginTransaction()
        try {
            for (id in logIds) {
                val cv = ContentValues().apply { put("sync_status", "synced") }
                db.update("attendance_logs", cv, "id = ?", arrayOf(id))
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun getPendingSyncCount(): Int {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT COUNT(*) FROM attendance_logs WHERE sync_status = 'pending'", null)
        return cursor.use {
            if (it.moveToFirst()) it.getInt(0) else 0
        }
    }

    fun getTodayLogsCount(): Int {
        val db = readableDatabase
        val now = System.currentTimeMillis()
        val startOfDay = now - (now % (24 * 60 * 60 * 1000))
        val cursor = db.rawQuery("SELECT COUNT(*) FROM attendance_logs WHERE timestamp >= ?", arrayOf(startOfDay.toString()))
        return cursor.use {
            if (it.moveToFirst()) it.getInt(0) else 0
        }
    }
}
