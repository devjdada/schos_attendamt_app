package com.schoolos.attendance.data.repository

import android.content.Context
import android.content.SharedPreferences

data class TerminalCredentials(
    val isActivated: Boolean,
    val terminalId: String?,
    val terminalName: String?,
    val tenantId: String?,
    val schoolName: String?,
    val campusName: String?,
    val apiKey: String?,
    val direction: String, // "in", "out", "both"
    val attendancePolicy: String // "FACE_ONLY", "FINGERPRINT_ONLY", "RFID_ONLY", "RFID_PLUS_FACE", "ANY_BIOMETRIC"
)

class DeviceActivationManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "schoolos_terminal_secure_prefs"
        private const val KEY_IS_ACTIVATED = "is_activated"
        private const val KEY_TERMINAL_ID = "terminal_id"
        private const val KEY_TERMINAL_NAME = "terminal_name"
        private const val KEY_TENANT_ID = "tenant_id"
        private const val KEY_SCHOOL_NAME = "school_name"
        private const val KEY_CAMPUS_NAME = "campus_name"
        private const val KEY_API_KEY = "api_key"
        private const val KEY_DIRECTION = "direction"
        private const val KEY_POLICY = "attendance_policy"
        private const val KEY_KIOSK_PIN = "kiosk_admin_pin"
    }

    fun getCredentials(): TerminalCredentials {
        return TerminalCredentials(
            isActivated = prefs.getBoolean(KEY_IS_ACTIVATED, false),
            terminalId = prefs.getString(KEY_TERMINAL_ID, null),
            terminalName = prefs.getString(KEY_TERMINAL_NAME, "Main Entrance Kiosk"),
            tenantId = prefs.getString(KEY_TENANT_ID, null),
            schoolName = prefs.getString(KEY_SCHOOL_NAME, "Atlantic Hall College"),
            campusName = prefs.getString(KEY_CAMPUS_NAME, "Lekki Main Campus"),
            apiKey = prefs.getString(KEY_API_KEY, null),
            direction = prefs.getString(KEY_DIRECTION, "in") ?: "in",
            attendancePolicy = prefs.getString(KEY_POLICY, "ANY_BIOMETRIC") ?: "ANY_BIOMETRIC"
        )
    }

    fun saveActivation(
        terminalId: String,
        terminalName: String,
        tenantId: String,
        schoolName: String,
        campusName: String,
        apiKey: String,
        direction: String = "in",
        policy: String = "ANY_BIOMETRIC"
    ) {
        prefs.edit()
            .putBoolean(KEY_IS_ACTIVATED, true)
            .putString(KEY_TERMINAL_ID, terminalId)
            .putString(KEY_TERMINAL_NAME, terminalName)
            .putString(KEY_TENANT_ID, tenantId)
            .putString(KEY_SCHOOL_NAME, schoolName)
            .putString(KEY_CAMPUS_NAME, campusName)
            .putString(KEY_API_KEY, apiKey)
            .putString(KEY_DIRECTION, direction)
            .putString(KEY_POLICY, policy)
            .apply()
    }

    fun updatePolicy(policy: String) {
        prefs.edit().putString(KEY_POLICY, policy).apply()
    }

    fun updateDirection(direction: String) {
        prefs.edit().putString(KEY_DIRECTION, direction).apply()
    }

    fun getKioskAdminPin(): String {
        return prefs.getString(KEY_KIOSK_PIN, "1234") ?: "1234"
    }

    fun setKioskAdminPin(pin: String) {
        prefs.edit().putString(KEY_KIOSK_PIN, pin).apply()
    }

    fun deactivate() {
        prefs.edit().clear().apply()
    }
}
