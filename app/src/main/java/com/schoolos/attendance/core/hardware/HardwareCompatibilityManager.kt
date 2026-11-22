package com.schoolos.attendance.core.hardware

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.os.Environment
import android.os.StatFs

data class HardwareDiagnosticStatus(
    val phoneFrontCameraAvailable: Boolean,
    val externalUvcCameraConnected: Boolean,
    val activeCameraSource: CameraSource,
    val fingerprintScannerConnected: Boolean,
    val fingerprintScannerModel: String,
    val rfidReaderConnected: Boolean,
    val rfidReaderType: String,
    val usbHubConnected: Boolean,
    val batteryPercentage: Int,
    val isCharging: Boolean,
    val chargingSource: String, // "AC", "USB", "Power Delivery"
    val internalStorageFreeMb: Long,
    val internalStorageTotalMb: Long,
    val isNetworkOnline: Boolean,
    val networkType: String, // "Wi-Fi", "Cellular", "Ethernet", "None"
    val pendingSyncRecords: Int
)

class HardwareCompatibilityManager(
    private val context: Context,
    private val usbDeviceManager: UsbDeviceManager
) {
    fun getHardwareDiagnostics(
        activeCameraSource: CameraSource,
        fingerprintReader: FingerprintReader?,
        rfidReader: RfidReader?,
        pendingSyncCount: Int = 0
    ): HardwareDiagnosticStatus {
        val usbPeripherals = usbDeviceManager.getConnectedPeripherals()
        val hasExternalCamera = usbPeripherals.any { it.type == UsbPeripheralType.WEBCAM_UVC }
        val hasFingerprint = (fingerprintReader?.isConnected() == true) || usbPeripherals.any { it.type == UsbPeripheralType.FINGERPRINT_SCANNER }
        val hasRfid = (rfidReader?.isConnected() == true) || usbPeripherals.any { it.type == UsbPeripheralType.RFID_READER }
        val hasHub = usbPeripherals.any { it.type == UsbPeripheralType.USB_HUB } || usbPeripherals.size >= 2

        val (batteryPct, isCharging, chargeSource) = readBatteryState()
        val (storageFree, storageTotal) = readStorageMb()
        val (isOnline, netType) = readNetworkState()

        return HardwareDiagnosticStatus(
            phoneFrontCameraAvailable = true,
            externalUvcCameraConnected = hasExternalCamera,
            activeCameraSource = activeCameraSource,
            fingerprintScannerConnected = hasFingerprint,
            fingerprintScannerModel = fingerprintReader?.getDeviceModel() ?: if (hasFingerprint) "ZKTeco ZK9500 (Detected)" else "Not Connected",
            rfidReaderConnected = hasRfid,
            rfidReaderType = rfidReader?.getReaderType() ?: if (hasRfid) "USB 125kHz HID Keyboard" else "Not Connected",
            usbHubConnected = hasHub,
            batteryPercentage = batteryPct,
            isCharging = isCharging,
            chargingSource = chargeSource,
            internalStorageFreeMb = storageFree,
            internalStorageTotalMb = storageTotal,
            isNetworkOnline = isOnline,
            networkType = netType,
            pendingSyncRecords = pendingSyncCount
        )
    }

    private fun readBatteryState(): Triple<Int, Boolean, String> {
        val batteryIntent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val pct = if (level != -1 && scale != -1) ((level.toFloat() / scale.toFloat()) * 100).toInt() else 100

        val status = batteryIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL

        val plugged = batteryIntent?.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1) ?: -1
        val source = when (plugged) {
            BatteryManager.BATTERY_PLUGGED_AC -> "AC Wall Adapter"
            BatteryManager.BATTERY_PLUGGED_USB -> "USB-C Power Delivery / Hub"
            BatteryManager.BATTERY_PLUGGED_WIRELESS -> "Wireless"
            else -> if (isCharging) "Charging" else "On Battery"
        }

        return Triple(pct, isCharging, source)
    }

    private fun readStorageMb(): Pair<Long, Long> {
        val path = Environment.getDataDirectory()
        val stat = StatFs(path.path)
        val blockSize = stat.blockSizeLong
        val totalBlocks = stat.blockCountLong
        val availableBlocks = stat.availableBlocksLong

        val totalMb = (totalBlocks * blockSize) / (1024 * 1024)
        val freeMb = (availableBlocks * blockSize) / (1024 * 1024)
        return Pair(freeMb, totalMb)
    }

    private fun readNetworkState(): Pair<Boolean, String> {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return Pair(false, "None")
        val activeNet = cm.activeNetwork ?: return Pair(false, "None")
        val caps = cm.getNetworkCapabilities(activeNet) ?: return Pair(false, "None")

        val hasInternet = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        val type = when {
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "Wi-Fi"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Cellular (4G/5G)"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet"
            else -> "Connected"
        }
        return Pair(hasInternet, type)
    }
}
