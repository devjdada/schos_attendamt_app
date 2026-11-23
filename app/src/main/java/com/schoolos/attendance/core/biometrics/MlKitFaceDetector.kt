package com.schoolos.attendance.core.biometrics

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.schoolos.attendance.core.hardware.DetectedFace
import com.schoolos.attendance.core.hardware.FaceDetectionEngine
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

class MlKitFaceDetector : FaceDetectionEngine {

    private val detectorOptions = FaceDetectorOptions.Builder()
        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
        .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
        .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
        .setMinFaceSize(0.15f)
        .enableTracking()
        .build()

    private val detector = FaceDetection.getClient(detectorOptions)

    override suspend fun detectFaces(bitmap: Bitmap): List<DetectedFace> =
        suspendCancellableCoroutine { continuation ->
            try {
                val image = InputImage.fromBitmap(bitmap, 0)
                detector.process(image)
                    .addOnSuccessListener { faces ->
                        val results = faces.map { face ->
                            DetectedFace(
                                bounds = face.boundingBox,
                                yawAngle = face.headEulerAngleY,
                                pitchAngle = face.headEulerAngleX,
                                rollAngle = face.headEulerAngleZ,
                                leftEyeOpenProb = face.leftEyeOpenProbability,
                                rightEyeOpenProb = face.rightEyeOpenProbability,
                                smilingProb = face.smilingProbability,
                                trackingId = face.trackingId
                            )
                        }
                        if (continuation.isActive) {
                            continuation.resume(results)
                        }
                    }
                    .addOnFailureListener {
                        if (continuation.isActive) {
                            continuation.resume(emptyList())
                        }
                    }
            } catch (e: Exception) {
                if (continuation.isActive) {
                    continuation.resume(emptyList())
                }
            }
        }

    override fun release() {
        try {
            detector.close()
        } catch (_: Exception) {}
    }
}
