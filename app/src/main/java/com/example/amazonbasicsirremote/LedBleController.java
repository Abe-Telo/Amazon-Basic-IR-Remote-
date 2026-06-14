package com.example.amazonbasicsirremote;

import android.bluetooth.BluetoothAdapter;

final class LedBleController {
    private LedBleController() {}

    static boolean isAvailable() {
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        return adapter != null && adapter.isEnabled();
    }
}
