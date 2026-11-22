package com.schoolos.attendance.core.biometrics

import android.content.Context
import android.view.KeyEvent
import com.schoolos.attendance.core.hardware.RfidEvent
import com.schoolos.attendance.core.hardware.RfidReader

class UsbHidRfidAdapter : RfidReader {

    private var eventCallback: ((RfidEvent) -> Unit)? = null
    private val buffer = StringBuilder()
    private var lastTapTime = 0L
    private var lastCardUid = ""
    private var isReaderConnected = true

    companion object {
        private const val DEBOUNCE_MS = 2500L
    }

    override fun initialize(context: Context, onEvent: (RfidEvent) -> Unit) {
        this.eventCallback = onEvent
        this.isReaderConnected = true
        onEvent(RfidEvent.DeviceConnected("USB 125kHz / 13.56MHz HID Proximity Reader"))
    }

    override fun handleKeyEvent(keyCode: Int, unicodeChar: Int): Boolean {
        if (keyCode == KeyEvent.KEYCODE_ENTER || unicodeChar == '\n'.code || unicodeChar == '\r'.code) {
            val rawUid = buffer.toString().trim()
            buffer.clear()
            if (rawUid.isNotEmpty()) {
                processCardUid(rawUid)
                return true
            }
            return false
        }

        if (unicodeChar > 0) {
            val char = unicodeChar.toChar()
            if (char.isLetterOrDigit()) {
                buffer.append(char)
                // If 10-digit standard EM4100 length reached without enter
                if (buffer.length == 10 && buffer.all { it.isDigit() }) {
                    val uid = buffer.toString()
                    buffer.clear()
                    processCardUid(uid)
                    return true
                }
                return true
            }
        }
        return false
    }

    override fun onRawUidReceived(uid: String) {
        val clean = uid.trim()
        if (clean.isNotEmpty()) {
            processCardUid(clean)
        }
    }

    private fun processCardUid(cardUid: String) {
        val now = System.currentTimeMillis()
        if (cardUid == lastCardUid && (now - lastTapTime) < DEBOUNCE_MS) {
            // Debounce repeated accidental tap
            return
        }

        lastCardUid = cardUid
        lastTapTime = now

        val standard = if (cardUid.length == 10 && cardUid.all { it.isDigit() }) {
            "125kHz Proximity (EM4100/TK4100)"
        } else {
            "13.56MHz ISO14443A (MIFARE/NFC)"
        }

        eventCallback?.invoke(RfidEvent.CardDetected(cardUid, standard))
    }

    override fun isConnected(): Boolean = isReaderConnected

    override fun getReaderType(): String = "USB HID Keyboard Emulation (Plug & Play)"

    override fun release() {
        buffer.clear()
        eventCallback = null
        isReaderConnected = false
    }
}
