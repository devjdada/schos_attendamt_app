package com.schoolos.attendance.core.hardware

data class LivenessResult(
    val isLive: Boolean,
    val confidence: Float,
    val reason: String
)

interface FaceLivenessEngine {
    fun evaluateLiveness(
        currentFace: DetectedFace,
        faceHistory: List<DetectedFace>
    ): LivenessResult
}
