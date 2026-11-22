package com.schoolos.attendance.core.hardware

import android.content.Context
import android.graphics.Bitmap

enum class CameraSource {
    FRONT_CAMERA,
    BACK_CAMERA,
    EXTERNAL_USB_UVC
}

interface CameraProvider {
    fun initialize(
        context: Context,
        lifecycleOwner: androidx.lifecycle.LifecycleOwner,
        previewView: androidx.camera.view.PreviewView,
        onFrameAnalyzed: (Bitmap) -> Unit
    )
    fun switchCamera(source: CameraSource)
    fun getActiveSource(): CameraSource
    fun isExternalCameraAvailable(): Boolean
    fun release()
}
