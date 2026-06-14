package com.example.amazonbasicsirremote

import java.util.UUID

/**
 * Supported LED BLE command profiles.
 *
 * Many low-cost BLE LED controllers advertise similar names and services while
 * accepting different nine-byte command layouts. A profile captures both the
 * GATT endpoint and the packet dialect used by a device family.
 */
enum class LedBleProfile(
    val displayName: String,
    val advertisedNameTokens: Set<String>,
    val serviceUuid: UUID,
    val writeCharacteristicUuid: UUID,
    val notifyCharacteristicUuid: UUID?,
    internal val packetFormat: PacketFormat,
) {
    /** LEDBLE / LED LAMP style controllers that use command packets without a checksum byte. */
    LedBleLedLamp(
        displayName = "LEDBLE / LED LAMP",
        advertisedNameTokens = setOf("LEDBLE", "LEDLAMP", "LED LAMP", "LEDNET"),
        serviceUuid = UUID.fromString("0000ffe0-0000-1000-8000-00805f9b34fb"),
        writeCharacteristicUuid = UUID.fromString("0000ffe1-0000-1000-8000-00805f9b34fb"),
        notifyCharacteristicUuid = null,
        packetFormat = PacketFormat.LedBleLedLamp,
    ),

    /** ELK-BLEDOM style controllers that use the FFF0/FFF3 service and a checksum byte. */
    ElkBledom(
        displayName = "ELK-BLEDOM",
        advertisedNameTokens = setOf("ELK-BLEDOM", "ELKBLEDOM"),
        serviceUuid = UUID.fromString("0000fff0-0000-1000-8000-00805f9b34fb"),
        writeCharacteristicUuid = UUID.fromString("0000fff3-0000-1000-8000-00805f9b34fb"),
        notifyCharacteristicUuid = UUID.fromString("0000fff4-0000-1000-8000-00805f9b34fb"),
        packetFormat = PacketFormat.ElkBledom,
    );

    internal enum class PacketFormat { LedBleLedLamp, ElkBledom }

    companion object {
        fun fromAdvertisedName(name: String): LedBleProfile? {
            val normalized = name.uppercase().replace(" ", "")
            return values().firstOrNull { profile ->
                profile.advertisedNameTokens.any { normalized.contains(it.uppercase().replace(" ", "")) }
            }
        }
    }
}
