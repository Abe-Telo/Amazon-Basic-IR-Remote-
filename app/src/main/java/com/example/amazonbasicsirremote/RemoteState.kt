package com.example.amazonbasicsirremote

/**
 * Logical state for the Amazon Basics/Midea AC remote captures.
 *
 * Commands are six unsigned bytes. Temperature commands use the captured
 * Fahrenheit range (62F..86F). Fan-only commands always use temperature byte
 * 0x7E (126), matching the provided Fan_* captures.
 */
data class RemoteState(
    val mode: Mode,
    val fanSpeed: FanSpeed = FanSpeed.AUTO,
    val temperatureF: Int = MinTemperatureF,
) {
    init {
        require(temperatureF in TemperatureRange) {
            "temperatureF must be in $MinTemperatureF..$MaxTemperatureF: $temperatureF"
        }
        require(mode != Mode.AUTO || fanSpeed == FanSpeed.AUTO) {
            "AUTO mode is only available with AUTO fan speed in the provided command data."
        }
        require(mode != Mode.DRY || fanSpeed == FanSpeed.AUTO) {
            "DRY mode is only available with AUTO fan speed in the provided command data."
        }
    }

    /** Returns the six command bytes for this remote state. */
    fun toCommandBytes(): IntArray {
        val commandByte = mode.commandByte(fanSpeed)
        val temperatureByte = if (mode.supportsTemperature) {
            TemperatureByteBase + (temperatureF - MinTemperatureF)
        } else {
            FanOnlyTemperatureByte
        }

        return intArrayOf(
            HeaderByte,
            commandByte,
            temperatureByte,
            FixedByte,
            FixedByte,
            checksum(commandByte, temperatureByte),
        )
    }

    enum class Mode {
        AUTO,
        COOL,
        DRY,
        FAN;

        val supportsTemperature: Boolean
            get() = this != FAN

        internal fun commandByte(fanSpeed: FanSpeed): Int = when (this) {
            AUTO -> 0x82
            COOL -> when (fanSpeed) {
                FanSpeed.AUTO -> 0xA0
                FanSpeed.LOW -> 0x88
                FanSpeed.MEDIUM -> 0x90
                FanSpeed.HIGH -> 0x98
            }
            DRY -> 0x81
            FAN -> when (fanSpeed) {
                FanSpeed.AUTO -> 0xA4
                FanSpeed.LOW -> 0x8C
                FanSpeed.MEDIUM -> 0x94
                FanSpeed.HIGH -> 0x9C
            }
        }
    }

    enum class FanSpeed {
        AUTO,
        LOW,
        MEDIUM,
        HIGH,
    }

    companion object {
        const val MinTemperatureF = 62
        const val MaxTemperatureF = 86
        val TemperatureRange: IntRange = MinTemperatureF..MaxTemperatureF

        private const val HeaderByte = 0xA1
        private const val FixedByte = 0xFF
        private const val TemperatureByteBase = 0x60
        private const val FanOnlyTemperatureByte = 126

        private val FanOnlyChecksums = mapOf(
            0xA4 to 0x5B,
            0x8C to 0x73,
            0x94 to 0x6B,
            0x9C to 0x63,
        )

        private fun checksum(commandByte: Int, temperatureByte: Int): Int {
            FanOnlyChecksums[commandByte]?.let { checksum ->
                require(temperatureByte == FanOnlyTemperatureByte) {
                    "Fan-only commands must use fixed temperature byte $FanOnlyTemperatureByte."
                }
                return checksum
            }

            val temperatureOffset = temperatureByte - TemperatureByteBase
            require(temperatureOffset in 0 until TemperatureRange.count()) {
                "Temperature byte must represent $MinTemperatureF..$MaxTemperatureF: $temperatureByte"
            }

            return when (commandByte) {
                0x82 -> autoChecksum(temperatureOffset)
                0xA0 -> coolAutoChecksum(temperatureOffset)
                0x88 -> coolLowChecksum(temperatureOffset)
                0x90 -> coolMediumChecksum(temperatureOffset)
                0x98 -> coolHighChecksum(temperatureOffset)
                0x81 -> dryChecksum(temperatureOffset)
                else -> error("Unsupported mode/fan command byte: $commandByte")
            }
        }

        private fun autoChecksum(offset: Int): Int = checksumFromTable(offset, intArrayOf(
            0x6C, 0x6D, 0x6F, 0x6E, 0x68, 0x69, 0x6B, 0x6A, 0x64, 0x65,
            0x67, 0x66, 0x60, 0x61, 0x63, 0x62, 0x74, 0x75, 0x77, 0x76,
            0x70, 0x71, 0x73, 0x72, 0x78,
        ))

        private fun coolAutoChecksum(offset: Int): Int = checksumFromTable(offset, intArrayOf(
            0x4E, 0x4F, 0x4C, 0x4D, 0x4A, 0x4B, 0x48, 0x49, 0x46, 0x47,
            0x44, 0x45, 0x42, 0x43, 0x40, 0x41, 0x56, 0x57, 0x54, 0x55,
            0x52, 0x53, 0x50, 0x51, 0x5A,
        ))

        private fun coolLowChecksum(offset: Int): Int = checksumFromTable(offset, intArrayOf(
            0x66, 0x67, 0x64, 0x65, 0x62, 0x63, 0x60, 0x61, 0x6A, 0x6B,
            0x68, 0x69, 0x6C, 0x6D, 0x6F, 0x6E, 0x7A, 0x7B, 0x78, 0x79,
            0x7C, 0x7D, 0x7F, 0x7E, 0x72,
        ))

        private fun coolMediumChecksum(offset: Int): Int = checksumFromTable(offset, intArrayOf(
            0x76, 0x77, 0x74, 0x75, 0x72, 0x73, 0x70, 0x71, 0x7A, 0x7B,
            0x78, 0x79, 0x7C, 0x7D, 0x7F, 0x7E, 0x66, 0x67, 0x64, 0x65,
            0x62, 0x63, 0x60, 0x61, 0x6A,
        ))

        private fun coolHighChecksum(offset: Int): Int = checksumFromTable(offset, intArrayOf(
            0x7A, 0x7B, 0x78, 0x79, 0x7C, 0x7D, 0x7F, 0x7E, 0x72, 0x73,
            0x70, 0x71, 0x74, 0x75, 0x77, 0x76, 0x6A, 0x6B, 0x68, 0x69,
            0x6C, 0x6D, 0x6F, 0x6E, 0x62,
        ))

        private fun dryChecksum(offset: Int): Int = checksumFromTable(offset, intArrayOf(
            0x6F, 0x6E, 0x6D, 0x6C, 0x6B, 0x6A, 0x69, 0x68, 0x67, 0x66,
            0x65, 0x64, 0x63, 0x62, 0x61, 0x60, 0x77, 0x76, 0x75, 0x74,
            0x73, 0x72, 0x71, 0x70, 0x7B,
        ))

        private fun checksumFromTable(offset: Int, table: IntArray): Int = table[offset]
    }
}
