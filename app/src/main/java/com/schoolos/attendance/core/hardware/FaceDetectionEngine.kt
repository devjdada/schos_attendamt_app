package com.schoolos.attendance.core.hardware

import android.graphics.Bitmap
import android.graphics.Rect

data class DetectedFace(
    val bounds: Rect,
    val yawAngle: Float,
    val pitchAngle: Float,
    val rollAngle: Float = 0f,
    val leftEyeOpenProb: Float? = null,
    val rightEyeOpenProb: Float? = null,
    val smilingProb: Float? = null,
    val trackingId: Int? = null
)

interface FaceDetectionEngine {
    suspend fun detectFaces(bitmap: Bitmap): List<DetectedFace>
    fun release()
}
