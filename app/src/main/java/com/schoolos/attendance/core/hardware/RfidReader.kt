package com.schoolos.attendance.core.hardware

import android.content.Context

sealed class RfidEvent {
    data class DeviceConnected(val model: String) : RfidEvent()
    data class DeviceDisconnected(val reason: String) : RfidEvent()
    data class CardDetected(val cardUid: String, val standard: String) : RfidEvent()
    data class Error(val message: String) : RfidEvent()
}

interface RfidReader {
    fun initialize(context: Context, onEvent: (RfidEvent) -> Unit)
    fun handleKeyEvent(keyCode: Int, unicodeChar: Int): Boolean
    fun onRawUidReceived(uid: String)
    fun isConnected(): Boolean
    fun getReaderType(): String
    fun release()
}
