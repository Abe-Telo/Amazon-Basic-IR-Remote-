package com.example.amazonbasicsirremote

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LedIrCommandsTest {
    private val flipperCaptures = parseFlipperRawFile("flipper/led_24_key_nec.ir")

    @Test
    fun exposesNecProtocolFramesForLedRemote() {
        assertArrayEquals(intArrayOf(0x00, 0xff, 0x45, 0xba), LedIrCommands.protocolFrameFor(LedIrCommands.Command.POWER_ON_OFF))
        assertArrayEquals(intArrayOf(0x00, 0xff, 0x1c, 0xe3), LedIrCommands.protocolFrameFor(LedIrCommands.Command.RED))
        assertArrayEquals(intArrayOf(0x00, 0xff, 0x1d, 0xe2), LedIrCommands.protocolFrameFor(LedIrCommands.Command.MODE_EFFECT))
    }

    @Test
    fun outputsUseLedCarrierAndFullNecFrameShape() {
        val pulses = LedIrCommands.rawPulsesFor(LedIrCommands.Command.POWER_ON_OFF)
        assertEquals(38_000, LedIrCommands.CARRIER_FREQUENCY_HZ)
        assertEquals(67, pulses.size)
        assertEquals(9_000, pulses.first())
        assertEquals(560, pulses.last())
        assertTrue(LedIrCommands.hasCapturedCode(LedIrCommands.Command.POWER_ON_OFF))
    }

    @Test
    fun generatedPulsesMatchCapturedFlipperRawData() {
        for (command in LedIrCommands.Command.entries) {
            assertArrayEquals("Flipper capture mismatch for $command", flipperCaptures.getValue(command.name), LedIrCommands.rawPulsesFor(command))
        }
    }

    private fun parseFlipperRawFile(resourceName: String): Map<String, IntArray> {
        val text = requireNotNull(javaClass.classLoader?.getResource(resourceName)) { "Missing test resource $resourceName" }.readText()
        val captures = linkedMapOf<String, IntArray>()
        var currentName: String? = null
        for (line in text.lineSequence()) {
            when {
                line.startsWith("name:") -> currentName = line.substringAfter(':').trim()
                line.startsWith("frequency:") -> assertEquals("38000", line.substringAfter(':').trim())
                line.startsWith("data:") -> {
                    val name = requireNotNull(currentName) { "data block appeared before a name" }
                    captures[name] = line.substringAfter(':').trim().split(Regex("\\s+")).map(String::toInt).toIntArray()
                }
            }
        }
        return captures
    }
}
