package com.example.amazonbasicsirremote

/**
 * Placeholder for local-network device discovery.
 *
 * Wi-Fi/LAN discovery is a separate capability from BLE scanning and manual IR
 * setup. Device-specific implementations can be added here without blocking BLE
 * or IR workflows.
 *
 * Planned discovery methods include:
 * - mDNS
 * - SSDP/UPnP
 * - direct IP
 * - vendor cloud/API
 */
class WifiDeviceDiscovery {
    fun plannedDiscoveryMethods(): List<String> = listOf(
        "mDNS",
        "SSDP/UPnP",
        "direct IP",
        "vendor cloud/API"
    )

    fun describePlannedDiscoveryMethods(): String = plannedDiscoveryMethods().joinToString(", ")
}
