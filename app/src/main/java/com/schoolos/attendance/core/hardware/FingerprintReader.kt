package com.schoolos.attendance.core.hardware

import android.content.Context

sealed class FingerprintEvent {
    data class DeviceConnected(val model: String) : FingerprintEvent()
    data class DeviceDisconnected(val reason: String) : FingerprintEvent()
    data class ImageCaptured(val imageBytes: ByteArray, val width: Int, val height: Int) : FingerprintEvent()
    data class TemplateExtracted(val templateBytes: ByteArray) : FingerprintEvent()
    data class Error(val code: Int, val message: String) : FingerprintEvent()
}

interface FingerprintReader {
    fun initialize(context: Context): Boolean
    fun startCapture(listener: (FingerprintEvent) -> Unit)
    fun stopCapture()
    fun match(templateA: ByteArray, templateB: ByteArray): Float
    fun isConnected(): Boolean
    fun getDeviceModel(): String
    fun release()
}
