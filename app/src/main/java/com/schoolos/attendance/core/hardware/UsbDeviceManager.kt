package com.schoolos.attendance.core.hardware

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Build

enum class UsbPeripheralType {
    WEBCAM_UVC,
    FINGERPRINT_SCANNER,
    RFID_READER,
    USB_HUB,
    UNKNOWN
}

data class ConnectedUsbPeripheral(
    val deviceId: Int,
    val deviceName: String,
    val vendorId: Int,
    val productId: Int,
    val type: UsbPeripheralType,
    val manufacturerName: String?,
    val productName: String?,
    val hasPermission: Boolean
)

class UsbDeviceManager(private val context: Context) {

    private val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
    private val ACTION_USB_PERMISSION = "com.schoolos.attendance.USB_PERMISSION"

    var onDeviceAttached: ((ConnectedUsbPeripheral) -> Unit)? = null
    var onDeviceDetached: ((Int) -> Unit)? = null

    private val usbReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context, intent: Intent) {
            when (intent.action) {
                UsbManager.ACTION_USB_DEVICE_ATTACHED -> {
                    val device = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(UsbManager.EXTRA_DEVICE, UsbDevice::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
                    }
                    device?.let {
                        val peripheral = mapUsbDevice(it)
                        onDeviceAttached?.invoke(peripheral)
                    }
                }
                UsbManager.ACTION_USB_DEVICE_DETACHED -> {
                    val device = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(UsbManager.EXTRA_DEVICE, UsbDevice::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
                    }
                    device?.let {
                        onDeviceDetached?.invoke(it.deviceId)
                    }
                }
                ACTION_USB_PERMISSION -> {
                    // Handled upon permission request resolution
                }
            }
        }
    }

    fun startListening() {
        val filter = IntentFilter().apply {
            addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
            addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
            addAction(ACTION_USB_PERMISSION)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(usbReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            context.registerReceiver(usbReceiver, filter)
        }
    }

    fun stopListening() {
        try {
            context.unregisterReceiver(usbReceiver)
        } catch (_: Exception) {}
    }

    fun getConnectedPeripherals(): List<ConnectedUsbPeripheral> {
        return usbManager.deviceList.values.map { mapUsbDevice(it) }
    }

    fun requestPermission(device: UsbDevice) {
        if (!usbManager.hasPermission(device)) {
            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }
            val permissionIntent = PendingIntent.getBroadcast(
                context, 0, Intent(ACTION_USB_PERMISSION), flags
            )
            usbManager.requestPermission(device, permissionIntent)
        }
    }

    private fun mapUsbDevice(device: UsbDevice): ConnectedUsbPeripheral {
        val type = classifyDevice(device)
        return ConnectedUsbPeripheral(
            deviceId = device.deviceId,
            deviceName = device.deviceName,
            vendorId = device.vendorId,
            productId = device.productId,
            type = type,
            manufacturerName = device.manufacturerName,
            productName = device.productName,
            hasPermission = usbManager.hasPermission(device)
        )
    }

    private fun classifyDevice(device: UsbDevice): UsbPeripheralType {
        // Check interface class
        for (i in 0 until device.interfaceCount) {
            val usbInterface = device.getInterface(i)
            // UVC Video Class: 14 (0x0E)
            if (usbInterface.interfaceClass == 14 || device.deviceClass == 14) {
                return UsbPeripheralType.WEBCAM_UVC
            }
            // USB Hub: Class 9
            if (usbInterface.interfaceClass == UsbConstants.USB_CLASS_HUB || device.deviceClass == UsbConstants.USB_CLASS_HUB) {
                return UsbPeripheralType.USB_HUB
            }
            // HID Class: 3 (RFID readers often report as HID)
            if (usbInterface.interfaceClass == UsbConstants.USB_CLASS_HID) {
                return UsbPeripheralType.RFID_READER
            }
        }

        // ZKTeco Vendor IDs: 0x1B55, 0x1B1A, 0x24C8, etc.
        // DigitalPersona Vendor ID: 0x05BA
        // Futronic: 0x1491
        if (device.vendorId in listOf(0x1B55, 0x1B1A, 0x24C8, 0x05BA, 0x1491)) {
            return UsbPeripheralType.FINGERPRINT_SCANNER
        }

        // Logitech Vendor ID: 0x046D
        if (device.vendorId == 0x046D) {
            return UsbPeripheralType.WEBCAM_UVC
        }

        return UsbPeripheralType.UNKNOWN
    }
}
