package com.example.amazonbasicsirremote

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import java.util.UUID

class LedBleClient(context: Context, private val listener: Listener?) {
    interface Listener {
        fun onStatusChanged(status: String)
        fun onDeviceFound(name: String)
        fun onServicesDiscovered()
    }

    private val context: Context = context.applicationContext
    private val bluetoothAdapter: BluetoothAdapter? =
        (context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter
    private var scanner: BluetoothLeScanner? = null
    private var gatt: BluetoothGatt? = null
    private var writeCharacteristic: BluetoothGattCharacteristic? = null
    private var writeType: Int = BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
    private var activeProfile: LedBleProfile = LedBleCommands.DefaultProfile

    fun isBluetoothReady(): Boolean = bluetoothAdapter?.isEnabled == true

    fun setWriteCharacteristic(serviceUuid: UUID, characteristicUuid: UUID, bluetoothGattWriteType: Int) {
        writeType = bluetoothGattWriteType
        writeCharacteristic = gatt?.getService(serviceUuid)?.getCharacteristic(characteristicUuid)
            ?: writeCharacteristic
        writeCharacteristic?.writeType = bluetoothGattWriteType
    }

    fun connectFirstKnownLedDevice() = startScan()

    fun startScan() {
        if (!isBluetoothReady()) {
            notifyStatus("Bluetooth is unavailable or disabled")
            return
        }
        if (!hasScanPermission() || !hasConnectPermission()) {
            notifyStatus("BLE permissions are required before scanning")
            return
        }
        scanner = bluetoothAdapter?.bluetoothLeScanner
        if (scanner == null) {
            notifyStatus("BLE scanner unavailable")
            return
        }
        scanner?.startScan(scanCallback)
        notifyStatus("Scanning for LED BLE controllers")
    }

    fun stopScan() {
        if (hasScanPermission()) scanner?.stopScan(scanCallback)
        scanner = null
    }

    fun disconnect() {
        stopScan()
        if (hasConnectPermission()) {
            gatt?.disconnect()
            gatt?.close()
        }
        gatt = null
        writeCharacteristic = null
    }

    fun sendPower(on: Boolean) = writeCommand(LedBleCommands.power(on, activeProfile))

    fun sendColor(r: Int, g: Int, b: Int) = writeCommand(LedBleCommands.rgb(r, g, b, activeProfile))

    fun sendBrightness(value: Int) = writeCommand(LedBleCommands.brightness(value.coerceIn(0, 100), activeProfile))

    fun writeCommand(command: ByteArray) {
        val bluetoothGatt = gatt
        if (bluetoothGatt == null) {
            notifyStatus("No BLE controller connected")
            return
        }
        if (!hasConnectPermission()) {
            notifyStatus("Bluetooth connect permission is required")
            return
        }
        val characteristic = writeCharacteristic
        if (characteristic == null) {
            notifyStatus("No compatible LED BLE write characteristic found")
            return
        }
        characteristic.writeType = writeType
        characteristic.value = command
        val queued = bluetoothGatt.writeCharacteristic(characteristic)
        notifyStatus(if (queued) "BLE command queued" else "BLE command write failed to queue")
    }

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val device = result.device ?: return
            val name = deviceName(device, result)
            if (!isTargetName(name)) return
            stopScan()
            activeProfile = LedBleProfile.fromAdvertisedName(name) ?: LedBleCommands.DefaultProfile
            listener?.onDeviceFound(name)
            connect(device, name)
        }

        override fun onScanFailed(errorCode: Int) {
            notifyStatus("BLE scan failed: $errorCode")
        }
    }

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(bluetoothGatt: BluetoothGatt, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                notifyStatus("Connected; discovering BLE services")
                if (hasConnectPermission()) bluetoothGatt.discoverServices()
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                notifyStatus("BLE controller disconnected")
            }
        }

        override fun onServicesDiscovered(bluetoothGatt: BluetoothGatt, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                writeCharacteristic = findLedWriteCharacteristic(bluetoothGatt.services)
                writeCharacteristic?.writeType = writeType
                listener?.onServicesDiscovered()
                notifyStatus(
                    if (writeCharacteristic == null) "Services discovered; no known LED write characteristic found"
                    else "Services discovered; BLE write characteristic ready"
                )
            } else {
                notifyStatus("BLE service discovery failed: $status")
            }
        }
    }

    private fun connect(device: BluetoothDevice, name: String) {
        if (!hasConnectPermission()) {
            notifyStatus("Bluetooth connect permission is required")
            return
        }
        notifyStatus("Connecting to $name")
        gatt = device.connectGatt(context, false, gattCallback)
    }

    private fun findLedWriteCharacteristic(services: List<BluetoothGattService>): BluetoothGattCharacteristic? {
        LedBleProfile.values().firstNotNullOfOrNull { profile ->
            services.firstOrNull { it.uuid == profile.serviceUuid }
                ?.getCharacteristic(profile.writeCharacteristicUuid)
                ?.also { activeProfile = profile }
        }?.let { return configureWriteType(it) }

        services.asSequence()
            .flatMap { it.characteristics.asSequence() }
            .firstOrNull { it.uuid == LedBleProfile.LedBleLedLamp.writeCharacteristicUuid && it.isWritable() }
            ?.let { activeProfile = LedBleProfile.LedBleLedLamp; return configureWriteType(it) }

        return services.asSequence()
            .flatMap { it.characteristics.asSequence() }
            .firstOrNull { it.isWritable() }
            ?.let { configureWriteType(it) }
    }

    private fun configureWriteType(characteristic: BluetoothGattCharacteristic): BluetoothGattCharacteristic {
        writeType = if ((characteristic.properties and BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) != 0) {
            BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
        } else {
            BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
        }
        characteristic.writeType = writeType
        return characteristic
    }

    private fun BluetoothGattCharacteristic.isWritable(): Boolean =
        (properties and (BluetoothGattCharacteristic.PROPERTY_WRITE or BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE)) != 0

    private fun deviceName(device: BluetoothDevice, result: ScanResult): String {
        val scanName = result.scanRecord?.deviceName
        if (!scanName.isNullOrBlank()) return scanName
        if (!hasConnectPermission()) return ""
        return device.name ?: ""
    }

    private fun isTargetName(name: String): Boolean {
        return LedBleProfile.fromAdvertisedName(name) != null
    }

    private fun hasScanPermission(): Boolean = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        context.checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED
    } else {
        context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    private fun hasConnectPermission(): Boolean = Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
        context.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED

    private fun notifyStatus(status: String) = listener?.onStatusChanged(status)
}
