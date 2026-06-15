package com.example.amazonbasicsirremote

/**
 * Discovery entry points are intentionally separate from command transports so
 * BLE scanning, LAN discovery, and manual IR setup can evolve independently.
 */
enum class DeviceDiscoveryType {
    BLE,
    IR_MANUAL,
    WIFI_LAN
}
