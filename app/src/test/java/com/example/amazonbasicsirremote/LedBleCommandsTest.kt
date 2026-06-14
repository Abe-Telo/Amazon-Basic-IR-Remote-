package com.example.amazonbasicsirremote

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class LedBleCommandsTest {
    @Test
    fun buildsElkBledomPowerPackets() {
        assertHex("7e 04 01 00 00 00 05 00 ef", LedBleCommands.powerOn(LedBleProfile.ElkBledom))
        assertHex("7e 04 00 00 00 00 04 00 ef", LedBleCommands.powerOff(LedBleProfile.ElkBledom))
    }

    @Test
    fun buildsElkBledomRgbPacketsWithWrappedChecksum() {
        assertHex("7e 07 ff 00 00 00 06 00 ef", LedBleCommands.rgb(255, 0, 0, LedBleProfile.ElkBledom))
        assertHex("7e 07 00 ff 00 00 06 00 ef", LedBleCommands.rgb(0, 255, 0, LedBleProfile.ElkBledom))
        assertHex("7e 07 00 00 ff 00 06 00 ef", LedBleCommands.rgb(0, 0, 255, LedBleProfile.ElkBledom))
        assertHex("7e 07 ff ff ff 00 04 00 ef", LedBleCommands.rgb(255, 255, 255, LedBleProfile.ElkBledom))
    }

    @Test
    fun buildsElkBledomBrightnessAndEffectPackets() {
        assertHex("7e 01 00 00 00 00 01 00 ef", LedBleCommands.brightness(0, LedBleProfile.ElkBledom))
        assertHex("7e 01 64 00 00 00 65 00 ef", LedBleCommands.brightness(100, LedBleProfile.ElkBledom))
        assertHex("7e 05 87 03 00 00 8f 00 ef", LedBleCommands.effect(LedBleCommands.Effect.Jump7Colors, profile = LedBleProfile.ElkBledom))
    }

    @Test
    fun buildsLedBleLedLampPowerAndColorPackets() {
        assertHex("7e 00 04 01 00 00 00 00 ef", LedBleCommands.powerOn(LedBleProfile.LedBleLedLamp))
        assertHex("7e 00 04 00 00 00 00 00 ef", LedBleCommands.powerOff(LedBleProfile.LedBleLedLamp))
        assertHex("7e 00 05 03 0c 22 38 00 ef", LedBleCommands.rgb(12, 34, 56, LedBleProfile.LedBleLedLamp))
    }

    @Test
    fun buildsLedBleLedLampBrightnessAndEffectPackets() {
        assertHex("7e 00 01 32 00 00 00 00 ef", LedBleCommands.brightness(50, LedBleProfile.LedBleLedLamp))
        assertHex("7e 00 03 8a 1f 00 00 00 ef", LedBleCommands.effect(LedBleCommands.Effect.Fade7Colors, 0x1f, LedBleProfile.LedBleLedLamp))
    }

    @Test
    fun exposesProfileUuidsAndNameMatching() {
        assertEquals("0000ffe0-0000-1000-8000-00805f9b34fb", LedBleProfile.LedBleLedLamp.serviceUuid.toString())
        assertEquals("0000ffe1-0000-1000-8000-00805f9b34fb", LedBleProfile.LedBleLedLamp.writeCharacteristicUuid.toString())
        assertEquals("0000fff0-0000-1000-8000-00805f9b34fb", LedBleProfile.ElkBledom.serviceUuid.toString())
        assertEquals("0000fff3-0000-1000-8000-00805f9b34fb", LedBleProfile.ElkBledom.writeCharacteristicUuid.toString())
        assertEquals(LedBleProfile.LedBleLedLamp, LedBleProfile.fromAdvertisedName("LED LAMP"))
        assertEquals(LedBleProfile.ElkBledom, LedBleProfile.fromAdvertisedName("ELK-BLEDOM"))
    }

    private fun assertHex(expected: String, actual: ByteArray) {
        val expectedBytes = expected.split(" ").map { it.toInt(16).toByte() }.toByteArray()
        assertArrayEquals(expectedBytes, actual)
    }
}
