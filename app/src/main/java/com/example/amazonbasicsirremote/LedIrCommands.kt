package com.example.amazonbasicsirremote

/**
 * LED-controller IR command table.
 *
 * These commands use the common 24-key RGB LED strip remote protocol captured
 * by Flipper as NEC frames: address 0x00FF plus an 8-bit command and its bitwise
 * inverse. The pulse output is generated directly from that LED protocol instead
 * of reusing the Amazon Basics / Midea AC encoder.
 */
object LedIrCommands {
    const val CARRIER_FREQUENCY_HZ: Int = 38_000

    private const val NEC_ADDRESS_LOW = 0x00
    private const val NEC_ADDRESS_HIGH = 0xff
    private const val LEADER_MARK_US = 9_000
    private const val LEADER_SPACE_US = 4_500
    private const val BIT_MARK_US = 560
    private const val ZERO_SPACE_US = 560
    private const val ONE_SPACE_US = 1_690

    enum class Command(private val displayName: String, internal val necCommand: Int) {
        POWER_ON_OFF("Power On/Off", 0x45),
        BRIGHTNESS_UP("Brightness Up", 0x46),
        BRIGHTNESS_DOWN("Brightness Down", 0x15),
        RED("Red", 0x1c),
        GREEN("Green", 0x18),
        BLUE("Blue", 0x5e),
        WHITE("White", 0x08),
        MODE_EFFECT("Mode/Effect", 0x1d),
        SPEED_UP("Speed Up", 0x40),
        SPEED_DOWN("Speed Down", 0x19);

        override fun toString(): String = displayName
    }

    @JvmStatic
    fun hasCapturedCode(command: Command): Boolean = rawPulsesFor(command).isNotEmpty()

    @JvmStatic
    fun rawPulsesFor(command: Command): IntArray = necPulses(command.necCommand)

    @JvmStatic
    fun protocolFrameFor(command: Command): IntArray {
        val code = command.necCommand and 0xff
        return intArrayOf(NEC_ADDRESS_LOW, NEC_ADDRESS_HIGH, code, code xor 0xff)
    }

    private fun necPulses(command: Int): IntArray {
        val bytes = intArrayOf(NEC_ADDRESS_LOW, NEC_ADDRESS_HIGH, command and 0xff, (command xor 0xff) and 0xff)
        val pulses = ArrayList<Int>(67)
        pulses.add(LEADER_MARK_US)
        pulses.add(LEADER_SPACE_US)
        for (byte in bytes) {
            for (bit in 0 until 8) {
                pulses.add(BIT_MARK_US)
                pulses.add(if (((byte shr bit) and 0x01) == 1) ONE_SPACE_US else ZERO_SPACE_US)
            }
        }
        pulses.add(BIT_MARK_US)
        return pulses.toIntArray()
    }
}
