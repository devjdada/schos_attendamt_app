package com.schoolos.attendance

import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.schoolos.attendance.core.biometrics.UsbHidRfidAdapter
import com.schoolos.attendance.core.biometrics.ZkFingerprintAdapter
import com.schoolos.attendance.core.hardware.UsbDeviceManager
import com.schoolos.attendance.core.service.AttendanceCaptureService
import com.schoolos.attendance.data.local.AttendanceDatabase
import com.schoolos.attendance.data.repository.DeviceActivationManager
import com.schoolos.attendance.ui.screens.ActivationScreen
import com.schoolos.attendance.ui.screens.DiagnosticsScreen
import com.schoolos.attendance.ui.screens.EnrolmentScreen
import com.schoolos.attendance.ui.screens.KioskAttendanceScreen
import com.schoolos.attendance.ui.theme.SchoolOSAttendanceTheme

class MainActivity : ComponentActivity() {

    private lateinit var usbDeviceManager: UsbDeviceManager
    private lateinit var rfidAdapter: UsbHidRfidAdapter
    private lateinit var fingerprintAdapter: ZkFingerprintAdapter
    private lateinit var captureService: AttendanceCaptureService
    private lateinit var activationManager: DeviceActivationManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. Keep Screen Always On for Kiosk Attendance Terminal
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        enableImmersiveKioskMode()

        // 2. Initialize Hardware Adapters & Core Services
        val db = AttendanceDatabase.getInstance(this)
        activationManager = DeviceActivationManager(this)
        usbDeviceManager = UsbDeviceManager(this)
        usbDeviceManager.startListening()

        rfidAdapter = UsbHidRfidAdapter()
        rfidAdapter.initialize(this) { event ->
            if (event is com.schoolos.attendance.core.hardware.RfidEvent.CardDetected) {
                captureService.processRfidCard(event.cardUid)
            }
        }

        fingerprintAdapter = ZkFingerprintAdapter()
        fingerprintAdapter.initialize(this)

        captureService = AttendanceCaptureService(
            db = db,
            activationManager = activationManager,
            scope = lifecycleScope
        )

        // 3. Render Compose User Interface
        setContent {
            SchoolOSAttendanceTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val creds = activationManager.getCredentials()
                    var currentScreen by remember {
                        mutableStateOf(if (creds.isActivated) "kiosk" else "activation")
                    }

                    when (currentScreen) {
                        "activation" -> {
                            ActivationScreen(
                                onActivationSuccess = {
                                    currentScreen = "kiosk"
                                }
                            )
                        }
                        "kiosk" -> {
                            KioskAttendanceScreen(
                                captureService = captureService,
                                onNavigateToDiagnostics = { currentScreen = "diagnostics" },
                                onNavigateToEnrolment = { currentScreen = "enrolment" },
                                onNavigateToActivation = { currentScreen = "activation" }
                            )
                        }
                        "diagnostics" -> {
                            DiagnosticsScreen(
                                usbDeviceManager = usbDeviceManager,
                                onBackToKiosk = { currentScreen = "kiosk" }
                            )
                        }
                        "enrolment" -> {
                            EnrolmentScreen(
                                captureService = captureService,
                                onBack = { currentScreen = "kiosk" }
                            )
                        }
                    }
                }
            }
        }
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        // Intercept USB HID RFID Scanner keystrokes before they hit the window focus
        if (event.action == KeyEvent.ACTION_DOWN) {
            val handled = rfidAdapter.handleKeyEvent(event.keyCode, event.unicodeChar)
            if (handled) return true
        }
        return super.dispatchKeyEvent(event)
    }

    private fun enableImmersiveKioskMode() {
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN
        )
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            enableImmersiveKioskMode()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        usbDeviceManager.stopListening()
        rfidAdapter.release()
        fingerprintAdapter.release()
        captureService.release()
    }
}
