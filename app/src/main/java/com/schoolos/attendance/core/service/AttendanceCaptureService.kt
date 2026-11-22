package com.schoolos.attendance.core.service

import android.graphics.Bitmap
import com.schoolos.attendance.core.biometrics.FaceLivenessDetector
import com.schoolos.attendance.core.biometrics.MlKitFaceDetector
import com.schoolos.attendance.core.biometrics.MobileFaceNetRecognizer
import com.schoolos.attendance.core.hardware.DetectedFace
import com.schoolos.attendance.core.hardware.FaceEmbedding
import com.schoolos.attendance.data.local.AttendanceDatabase
import com.schoolos.attendance.data.local.entities.AttendanceLogEntity
import com.schoolos.attendance.data.local.entities.EnrolledPersonEntity
import com.schoolos.attendance.data.repository.DeviceActivationManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.UUID

sealed class CaptureResult {
    data class Success(
        val person: EnrolledPersonEntity,
        val direction: String,
        val mode: String,
        val confidence: Float,
        val timestamp: Long = System.currentTimeMillis()
    ) : CaptureResult()

    data class DualFactorPendingFace(
        val person: EnrolledPersonEntity,
        val message: String
    ) : CaptureResult()

    data class LivenessFailed(val reason: String) : CaptureResult()
    data class UnknownPerson(val modality: String, val identifier: String?) : CaptureResult()
    data class Idle(val status: String = "Ready for Attendance") : CaptureResult()
}

class AttendanceCaptureService(
    private val db: AttendanceDatabase,
    private val activationManager: DeviceActivationManager,
    private val scope: CoroutineScope
) {
    private val faceDetector = MlKitFaceDetector()
    private val faceRecognizer = MobileFaceNetRecognizer()
    private val livenessDetector = FaceLivenessDetector()

    private val faceHistory = mutableListOf<DetectedFace>()
    private var pendingDualFactorPerson: EnrolledPersonEntity? = null
    private var pendingDualFactorExpiry = 0L

    var onCaptureResult: ((CaptureResult) -> Unit)? = null

    // Cache of enrolled embeddings for lightning-fast on-device matrix comparison
    private val enrolledFaceCache = mutableMapOf<String, Pair<EnrolledPersonEntity, FaceEmbedding>>()

    init {
        reloadEnrolledBiometrics()
    }

    fun reloadEnrolledBiometrics() {
        scope.launch(Dispatchers.IO) {
            val persons = db.getAllEnrolledPersons()
            synchronized(enrolledFaceCache) {
                enrolledFaceCache.clear()
                for (p in persons) {
                    p.faceEmbedding?.let { embeddingArray ->
                        enrolledFaceCache[p.id] = Pair(p, FaceEmbedding(embeddingArray))
                    }
                }
            }
        }
    }

    fun processCameraFrame(bitmap: Bitmap) {
        scope.launch(Dispatchers.Default) {
            val faces = faceDetector.detectFaces(bitmap)
            if (faces.isEmpty()) {
                synchronized(faceHistory) { faceHistory.clear() }
                return@launch
            }

            val primaryFace = faces.maxByOrNull { it.bounds.width() * it.bounds.height() } ?: return@launch

            // 1. Check Liveness
            val liveness = synchronized(faceHistory) {
                val res = livenessDetector.evaluateLiveness(primaryFace, faceHistory)
                faceHistory.add(primaryFace)
                if (faceHistory.size > 10) faceHistory.removeAt(0)
                res
            }

            if (!liveness.isLive) {
                onCaptureResult?.invoke(CaptureResult.LivenessFailed(liveness.reason))
                return@launch
            }

            // 2. Crop Face & Extract Embedding
            val bounds = primaryFace.bounds
            val left = bounds.left.coerceAtLeast(0)
            val top = bounds.top.coerceAtLeast(0)
            val width = bounds.width().coerceAtMost(bitmap.width - left)
            val height = bounds.height().coerceAtMost(bitmap.height - top)

            if (width < 60 || height < 60) return@launch

            val faceCrop = try {
                Bitmap.createBitmap(bitmap, left, top, width, height)
            } catch (e: Exception) {
                return@launch
            }

            val embedding = faceRecognizer.extractEmbedding(faceCrop)
            faceCrop.recycle()

            // 3. Match against Enrolled Database
            var bestMatch: EnrolledPersonEntity? = null
            var highestSimilarity = -1.0f

            synchronized(enrolledFaceCache) {
                for ((_, pair) in enrolledFaceCache) {
                    val person = pair.first
                    val enrolledEmb = pair.second
                    val sim = faceRecognizer.computeSimilarity(embedding, enrolledEmb)
                    if (sim > highestSimilarity) {
                        highestSimilarity = sim
                        bestMatch = person
                    }
                }
            }

            val threshold = MobileFaceNetRecognizer.MATCH_THRESHOLD
            if (bestMatch != null && highestSimilarity >= threshold) {
                val matchedPerson = bestMatch!!

                // Check Dual-Factor Pending State if applicable
                val now = System.currentTimeMillis()
                if (pendingDualFactorPerson != null && now < pendingDualFactorExpiry) {
                    if (pendingDualFactorPerson?.id == matchedPerson.id) {
                        pendingDualFactorPerson = null
                        recordAttendance(matchedPerson, "rfid_plus_face", highestSimilarity)
                    } else {
                        onCaptureResult?.invoke(CaptureResult.LivenessFailed("Face mismatch: Card does not belong to this student"))
                    }
                    return@launch
                }

                val creds = activationManager.getCredentials()
                val policy = creds.attendancePolicy
                if (policy == AttendancePolicy.RFID_ONLY.name) {
                    // RFID only policy ignores direct face check-in
                    return@launch
                }

                recordAttendance(matchedPerson, "face", highestSimilarity)
            }
        }
    }

    fun processRfidCard(cardUid: String) {
        scope.launch(Dispatchers.IO) {
            val person = db.findPersonByRfid(cardUid)
            if (person == null) {
                onCaptureResult?.invoke(CaptureResult.UnknownPerson("RFID Card", cardUid))
                return@launch
            }

            val creds = activationManager.getCredentials()
            val policy = creds.attendancePolicy

            if (policy == AttendancePolicy.RFID_PLUS_FACE.name) {
                // Initiate Dual-Factor Verification State
                pendingDualFactorPerson = person
                pendingDualFactorExpiry = System.currentTimeMillis() + 8000L // 8-second window
                onCaptureResult?.invoke(CaptureResult.DualFactorPendingFace(person, "Card verified! Please look at the camera to complete attendance."))
                return@launch
            }

            // Normal RFID verification
            recordAttendance(person, "rfid", 1.0f)
        }
    }

    fun processFingerprintTemplate(scannedTemplate: ByteArray) {
        scope.launch(Dispatchers.IO) {
            val persons = db.getAllEnrolledPersons()
            var matchedPerson: EnrolledPersonEntity? = null
            var bestScore = 0f

            for (p in persons) {
                p.fingerprintTemplate?.let { enrolledTpl ->
                    var matches = 0
                    val minLen = minOf(scannedTemplate.size, enrolledTpl.size)
                    for (i in 0 until minLen) {
                        if (scannedTemplate[i] == enrolledTpl[i]) matches++
                    }
                    val score = (matches.toFloat() / minLen.toFloat()) * 100f
                    if (score > bestScore && score >= 70f) {
                        bestScore = score
                        matchedPerson = p
                    }
                }
            }

            if (matchedPerson != null) {
                recordAttendance(matchedPerson!!, "fingerprint", bestScore / 100f)
            } else {
                onCaptureResult?.invoke(CaptureResult.UnknownPerson("Fingerprint", null))
            }
        }
    }

    private fun recordAttendance(person: EnrolledPersonEntity, mode: String, confidence: Float) {
        val creds = activationManager.getCredentials()
        val terminalId = creds.terminalId ?: "TERM_LOCAL"
        val tenantId = creds.tenantId ?: person.tenantId

        val direction = when (creds.direction.lowercase()) {
            "out" -> "out"
            "in" -> "in"
            else -> {
                // Auto schedule: 06:00 to 12:00 = in, 12:01 onwards = out
                val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
                if (hour < 12) "in" else "out"
            }
        }

        val log = AttendanceLogEntity(
            id = UUID.randomUUID().toString(),
            tenantId = tenantId,
            terminalId = terminalId,
            personId = person.id,
            personType = person.personType,
            identifier = person.identifier,
            fullName = person.fullName,
            classroomOrDept = person.classroomOrDept,
            verificationMode = mode,
            direction = direction,
            timestamp = System.currentTimeMillis(),
            confidenceScore = confidence,
            syncStatus = "pending"
        )

        db.insertAttendanceLog(log)
        onCaptureResult?.invoke(
            CaptureResult.Success(
                person = person,
                direction = direction,
                mode = mode,
                confidence = confidence
            )
        )
    }

    fun release() {
        faceDetector.release()
        faceRecognizer.release()
    }
}
