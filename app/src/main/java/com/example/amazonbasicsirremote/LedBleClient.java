package com.example.amazonbasicsirremote;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;

import java.util.Locale;
import java.util.UUID;

final class LedBleClient {
    interface Listener {
        void onStatusChanged(String status);
        void onDeviceFound(String name);
        void onServicesDiscovered();
    }

    private static final String[] TARGET_NAME_PREFIXES = {
            "LED BLE",
            "LEDNET",
            "LED LAMP",
            "ELK-BLEDOM",
            "LEDBLE-"
    };

    private final Context context;
    private final Listener listener;
    private final BluetoothAdapter bluetoothAdapter;
    private BluetoothLeScanner scanner;
    private BluetoothGatt gatt;
    private BluetoothGattCharacteristic writeCharacteristic;
    private Integer writeType;
    private UUID configuredServiceUuid;
    private UUID configuredCharacteristicUuid;

    LedBleClient(Context context, Listener listener) {
        this.context = context.getApplicationContext();
        this.listener = listener;
        BluetoothManager bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager == null ? null : bluetoothManager.getAdapter();
    }

    boolean isBluetoothReady() {
        return bluetoothAdapter != null && bluetoothAdapter.isEnabled();
    }

    void setWriteCharacteristic(UUID serviceUuid, UUID characteristicUuid, int bluetoothGattWriteType) {
        configuredServiceUuid = serviceUuid;
        configuredCharacteristicUuid = characteristicUuid;
        writeType = bluetoothGattWriteType;
        if (gatt != null) {
            BluetoothGattService service = gatt.getService(serviceUuid);
            writeCharacteristic = service == null ? null : service.getCharacteristic(characteristicUuid);
            if (writeCharacteristic != null) {
                writeCharacteristic.setWriteType(bluetoothGattWriteType);
            }
        }
    }

    void startScan() {
        if (!isBluetoothReady()) {
            notifyStatus("Bluetooth is unavailable or disabled");
            return;
        }
        if (!hasScanPermission() || !hasConnectPermission()) {
            notifyStatus("BLE permissions are required before scanning");
            return;
        }
        scanner = bluetoothAdapter.getBluetoothLeScanner();
        if (scanner == null) {
            notifyStatus("BLE scanner unavailable");
            return;
        }
        scanner.startScan(scanCallback);
        notifyStatus("Scanning for LED BLE controllers");
    }

    void stopScan() {
        if (scanner != null && hasScanPermission()) {
            scanner.stopScan(scanCallback);
        }
        scanner = null;
    }

    void disconnect() {
        stopScan();
        if (gatt != null && hasConnectPermission()) {
            gatt.disconnect();
            gatt.close();
        }
        gatt = null;
        writeCharacteristic = null;
    }

    void writeCommand(byte[] command) {
        if (gatt == null) {
            notifyStatus("No BLE controller connected");
            return;
        }
        if (!hasConnectPermission()) {
            notifyStatus("Bluetooth connect permission is required");
            return;
        }
        if (writeCharacteristic == null) {
            notifyStatus("BLE write characteristic not configured; capture device/service/characteristic/write type in nRF Connect first");
            return;
        }
        writeCharacteristic.setValue(command);
        boolean queued = gatt.writeCharacteristic(writeCharacteristic);
        notifyStatus(queued ? "BLE command queued" : "BLE command write failed to queue");
    }

    private final ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            BluetoothDevice device = result.getDevice();
            String name = getDeviceName(device);
            if (!isTargetName(name)) {
                return;
            }
            stopScan();
            if (listener != null) listener.onDeviceFound(name);
            connect(device);
        }
    };

    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt bluetoothGatt, int status, int newState) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                notifyStatus("Connected; discovering BLE services");
                if (hasConnectPermission()) bluetoothGatt.discoverServices();
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                notifyStatus("BLE controller disconnected");
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt bluetoothGatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                applyConfiguredCharacteristic(bluetoothGatt);
                if (listener != null) listener.onServicesDiscovered();
                notifyStatus(writeCharacteristic == null
                        ? "Services discovered; write characteristic still needs nRF Connect UUIDs"
                        : "Services discovered; BLE write characteristic ready");
            } else {
                notifyStatus("BLE service discovery failed: " + status);
            }
        }
    };

    private void connect(BluetoothDevice device) {
        if (!hasConnectPermission()) {
            notifyStatus("Bluetooth connect permission is required");
            return;
        }
        notifyStatus("Connecting to " + getDeviceName(device));
        gatt = device.connectGatt(context, false, gattCallback);
    }

    private void applyConfiguredCharacteristic(BluetoothGatt bluetoothGatt) {
        if (configuredServiceUuid == null || configuredCharacteristicUuid == null || writeType == null) {
            writeCharacteristic = null;
            return;
        }
        BluetoothGattService service = bluetoothGatt.getService(configuredServiceUuid);
        writeCharacteristic = service == null ? null : service.getCharacteristic(configuredCharacteristicUuid);
        if (writeCharacteristic != null) {
            writeCharacteristic.setWriteType(writeType);
        }
    }

    private String getDeviceName(BluetoothDevice device) {
        if (device == null || !hasConnectPermission()) return "";
        String name = device.getName();
        return name == null ? "" : name;
    }

    private boolean isTargetName(String name) {
        String normalized = name == null ? "" : name.toUpperCase(Locale.US);
        for (String prefix : TARGET_NAME_PREFIXES) {
            if (normalized.startsWith(prefix)) return true;
        }
        return false;
    }

    private boolean hasScanPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return context.checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED;
        }
        return context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private boolean hasConnectPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return context.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED;
        }
        return true;
    }

    private void notifyStatus(String status) {
        if (listener != null) listener.onStatusChanged(status);
    }
}
