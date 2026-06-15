package com.example.amazonbasicsirremote

/**
 * Encodes six-byte Midea R09B/BGCE commands into Flipper-style raw IR pulses.
 *
 * The checked-in Flipper captures use MSB-first byte order. Each command is sent
 * as one six-byte frame followed by a frame gap and a second frame containing
 * the bitwise complement of the first frame.
 */
class MideaIrCommandEncoder {
    fun encodeCommand(bytes: IntArray): IntArray {
        require(bytes.size == CommandByteCount) { "Midea commands must contain exactly $CommandByteCount bytes." }
        bytes.forEach { byte ->
            require(byte in 0x00..0xFF) { "Command bytes must be unsigned byte values: $byte" }
        }

        val pulses = ArrayList<Int>(EncodedPulseCount)
        appendFrame(pulses, bytes)
        appendFrameSeparator(pulses)
        appendFrame(pulses, bytes.map { it xor 0xFF }.toIntArray())
        pulses.add(BitMark)
        return pulses.toIntArray()
    }

    private fun appendFrameSeparator(pulses: MutableList<Int>) {
        pulses.add(BitMark)
        pulses.add(FrameSeparatorGap)
    }

    private fun appendFrame(pulses: MutableList<Int>, bytes: IntArray) {
        pulses.add(HeaderMark)
        pulses.add(HeaderSpace)

        for (byte in bytes) {
            for (bitIndex in 7 downTo 0) {
                pulses.add(BitMark)
                pulses.add(if (((byte shr bitIndex) and 1) == 1) OneSpace else ZeroSpace)
            }
        }
    }

    companion object {
        const val CarrierFrequency = 38_000
        const val HeaderMark = 4_480
        const val HeaderSpace = 4_480
        const val BitMark = 560
        const val ZeroSpace = 560
        const val OneSpace = 1_680
        const val FrameSeparatorGap = 5_320

        private const val CommandByteCount = 6
        private const val EncodedPulseCount = 199
    }
}

internal object MideaIrEncoderValidation {
    private val autoAuto62FCommand = intArrayOf(0xA1, 0x82, 0x60, 0xFF, 0xFF, 0x6C)

    private val autoAuto62FRaw = intArrayOf(
        4480, 4480, 560, 1680, 560, 560, 560, 1680, 560, 560, 560, 560, 560, 560,
        560, 560, 560, 1680, 560, 1680, 560, 560, 560, 560, 560, 560, 560, 560,
        560, 560, 560, 1680, 560, 560, 560, 560, 560, 1680, 560, 1680, 560, 560,
        560, 560, 560, 560, 560, 560, 560, 560, 560, 1680, 560, 1680, 560, 1680,
        560, 1680, 560, 1680, 560, 1680, 560, 1680, 560, 1680, 560, 1680, 560, 1680,
        560, 1680, 560, 1680, 560, 1680, 560, 1680, 560, 1680, 560, 1680, 560, 560,
        560, 1680, 560, 1680, 560, 560, 560, 1680, 560, 1680, 560, 560, 560, 560,
        560, 5320, 4480, 4480, 560, 560, 560, 1680, 560, 560, 560, 1680, 560, 1680,
        560, 1680, 560, 1680, 560, 560, 560, 560, 560, 1680, 560, 1680, 560, 1680,
        560, 1680, 560, 1680, 560, 560, 560, 1680, 560, 1680, 560, 560, 560, 560,
        560, 1680, 560, 1680, 560, 1680, 560, 1680, 560, 1680, 560, 560, 560, 560,
        560, 560, 560, 560, 560, 560, 560, 560, 560, 560, 560, 560, 560, 560,
        560, 560, 560, 560, 560, 560, 560, 560, 560, 560, 560, 560, 560, 560,
        560, 1680, 560, 560, 560, 560, 560, 1680, 560, 560, 560, 560, 560, 1680,
        560, 1680, 560,
    )

    fun validateAutoAuto62F(): Boolean =
        MideaIrCommandEncoder().encodeCommand(autoAuto62FCommand).contentEquals(autoAuto62FRaw)
}
