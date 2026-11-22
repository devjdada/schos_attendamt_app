package com.schoolos.attendance.core.biometrics

import com.schoolos.attendance.core.hardware.DetectedFace
import com.schoolos.attendance.core.hardware.FaceLivenessEngine
import com.schoolos.attendance.core.hardware.LivenessResult
import kotlin.math.abs

class FaceLivenessDetector : FaceLivenessEngine {

    companion object {
        private const val MIN_FACE_WIDTH_RATIO = 0.15f
        private const val MAX_YAW_ANGLE = 35.0f
        private const val MAX_PITCH_ANGLE = 25.0f
        private const val BLINK_THRESHOLD = 0.35f
    }

    override fun evaluateLiveness(
        currentFace: DetectedFace,
        faceHistory: List<DetectedFace>
    ): LivenessResult {
        // 1. Pose Angle Check (Reject extreme side profile or floor angles)
        if (abs(currentFace.yawAngle) > MAX_YAW_ANGLE) {
            return LivenessResult(false, 0.2f, "Please look directly at the terminal camera")
        }
        if (abs(currentFace.pitchAngle) > MAX_PITCH_ANGLE) {
            return LivenessResult(false, 0.2f, "Please level your head with the camera")
        }

        // 2. Minimum Face Dimensions (Reject distant background faces)
        if (currentFace.bounds.width() < 120 || currentFace.bounds.height() < 120) {
            return LivenessResult(false, 0.3f, "Please step closer to the attendance terminal")
        }

        // 3. Eye Open / Blink Micro-Expression Detection
        val leftOpen = currentFace.leftEyeOpenProb
        val rightOpen = currentFace.rightEyeOpenProb
        if (leftOpen != null && rightOpen != null) {
            val bothEyesClosed = leftOpen < BLINK_THRESHOLD && rightOpen < BLINK_THRESHOLD
            if (bothEyesClosed) {
                return LivenessResult(false, 0.5f, "Eyes closed, please keep eyes open")
            }
        }

        // 4. Temporal Micro-Movement Check across sequential frames
        if (faceHistory.size >= 2) {
            val prevFace = faceHistory.last()
            val dx = abs(currentFace.bounds.centerX() - prevFace.bounds.centerX())
            val dy = abs(currentFace.bounds.centerY() - prevFace.bounds.centerY())
            val dw = abs(currentFace.bounds.width() - prevFace.bounds.width())

            // Perfect zero movement across several frames indicates a static paper photo held in front of the lens
            if (faceHistory.size >= 5) {
                var totalDiff = 0
                for (i in 1 until faceHistory.size) {
                    totalDiff += abs(faceHistory[i].bounds.centerX() - faceHistory[i - 1].bounds.centerX())
                }
                if (totalDiff == 0 && dx == 0 && dy == 0) {
                    return LivenessResult(false, 0.3f, "Anti-Spoofing: Static image detected")
                }
            }
        }

        return LivenessResult(true, 0.95f, "Live human verified")
    }
}
