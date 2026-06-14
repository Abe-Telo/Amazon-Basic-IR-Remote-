package com.example.amazonbasicsirremote

import java.util.UUID

/**
 * Packet builder for the captured LED BLE controller protocol.
 *
 * The controller accepts nine-byte packets on the FFF3 write characteristic:
 * 7e <opcode> <payload 1..4> <checksum> 00 ef. The checksum is the low byte of
 * the opcode plus the four payload bytes.
 */
object LedBleCommands {
    val ServiceUuid: UUID = UUID.fromString("0000fff0-0000-1000-8000-00805f9b34fb")
    val WriteCharacteristicUuid: UUID = UUID.fromString("0000fff3-0000-1000-8000-00805f9b34fb")
    val NotifyCharacteristicUuid: UUID = UUID.fromString("0000fff4-0000-1000-8000-00805f9b34fb")

    private const val Prefix = 0x7e
    private const val SuffixPadding = 0x00
    private const val SuffixTerminator = 0xef

    fun power(on: Boolean): ByteArray = packet(0x04, if (on) 0x01 else 0x00, 0x00, 0x00, 0x00)

    fun rgb(red: Int, green: Int, blue: Int): ByteArray =
        packet(0x07, checkedByte(red, "red"), checkedByte(green, "green"), checkedByte(blue, "blue"), 0x00)

    fun brightness(percent: Int): ByteArray {
        require(percent in 0..100) { "Brightness percent must be in 0..100: $percent" }
        return packet(0x01, percent, 0x00, 0x00, 0x00)
    }

    fun effect(effect: Effect, speed: Int = 0x03): ByteArray {
        require(speed in 0x01..0x1f) { "Effect speed must be in 1..31: $speed" }
        return packet(0x05, effect.id, speed, 0x00, 0x00)
    }

    fun packet(opcode: Int, payload1: Int, payload2: Int, payload3: Int, payload4: Int): ByteArray {
        val fields = intArrayOf(opcode, payload1, payload2, payload3, payload4)
        fields.forEach { checkedByte(it, "packet field") }
        val checksum = fields.sum() and 0xff
        return intArrayOf(Prefix, opcode, payload1, payload2, payload3, payload4, checksum, SuffixPadding, SuffixTerminator)
            .map { it.toByte() }
            .toByteArray()
    }

    private fun checkedByte(value: Int, name: String): Int {
        require(value in 0x00..0xff) { "$name must be an unsigned byte: $value" }
        return value
    }

    enum class Effect(val id: Int) {
        Jump7Colors(0x87),
        Fade7Colors(0x8a),
    }
}
