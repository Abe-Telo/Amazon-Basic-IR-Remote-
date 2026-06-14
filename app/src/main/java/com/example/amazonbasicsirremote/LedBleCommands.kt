package com.example.amazonbasicsirremote

import java.util.UUID

/** Builds protocol-specific command packets for supported LED BLE controllers. */
object LedBleCommands {
    val DefaultProfile: LedBleProfile = LedBleProfile.ElkBledom

    val ServiceUuid: UUID = DefaultProfile.serviceUuid
    val WriteCharacteristicUuid: UUID = DefaultProfile.writeCharacteristicUuid
    val NotifyCharacteristicUuid: UUID = DefaultProfile.notifyCharacteristicUuid!!

    private const val Prefix = 0x7e
    private const val Padding = 0x00
    private const val Terminator = 0xef

    fun powerOn(profile: LedBleProfile = DefaultProfile): ByteArray = power(true, profile)

    fun powerOff(profile: LedBleProfile = DefaultProfile): ByteArray = power(false, profile)

    fun power(on: Boolean, profile: LedBleProfile = DefaultProfile): ByteArray = when (profile.packetFormat) {
        LedBleProfile.PacketFormat.LedBleLedLamp -> ledBlePacket(0x04, if (on) 0x01 else 0x00, 0x00, 0x00, 0x00)
        LedBleProfile.PacketFormat.ElkBledom -> elkPacket(0x04, if (on) 0x01 else 0x00, 0x00, 0x00, 0x00)
    }

    fun rgb(red: Int, green: Int, blue: Int, profile: LedBleProfile = DefaultProfile): ByteArray {
        val r = checkedByte(red, "red")
        val g = checkedByte(green, "green")
        val b = checkedByte(blue, "blue")
        return when (profile.packetFormat) {
            LedBleProfile.PacketFormat.LedBleLedLamp -> ledBlePacket(0x05, 0x03, r, g, b)
            LedBleProfile.PacketFormat.ElkBledom -> elkPacket(0x07, r, g, b, 0x00)
        }
    }

    fun brightness(percent: Int, profile: LedBleProfile = DefaultProfile): ByteArray {
        require(percent in 0..100) { "Brightness percent must be in 0..100: $percent" }
        return when (profile.packetFormat) {
            LedBleProfile.PacketFormat.LedBleLedLamp -> ledBlePacket(0x01, percent, 0x00, 0x00, 0x00)
            LedBleProfile.PacketFormat.ElkBledom -> elkPacket(0x01, percent, 0x00, 0x00, 0x00)
        }
    }

    fun effect(effect: Effect, speed: Int = 0x03, profile: LedBleProfile = DefaultProfile): ByteArray {
        require(speed in 0x01..0x1f) { "Effect speed must be in 1..31: $speed" }
        return when (profile.packetFormat) {
            LedBleProfile.PacketFormat.LedBleLedLamp -> ledBlePacket(0x03, effect.ledBleId, speed, 0x00, 0x00)
            LedBleProfile.PacketFormat.ElkBledom -> elkPacket(0x05, effect.elkBledomId, speed, 0x00, 0x00)
        }
    }

    fun packet(opcode: Int, payload1: Int, payload2: Int, payload3: Int, payload4: Int): ByteArray =
        elkPacket(opcode, payload1, payload2, payload3, payload4)

    private fun ledBlePacket(opcode: Int, payload1: Int, payload2: Int, payload3: Int, payload4: Int): ByteArray {
        val fields = intArrayOf(opcode, payload1, payload2, payload3, payload4)
        fields.forEach { checkedByte(it, "packet field") }
        return intArrayOf(Prefix, Padding, opcode, payload1, payload2, payload3, payload4, Padding, Terminator)
            .map(Int::toByte)
            .toByteArray()
    }

    private fun elkPacket(opcode: Int, payload1: Int, payload2: Int, payload3: Int, payload4: Int): ByteArray {
        val fields = intArrayOf(opcode, payload1, payload2, payload3, payload4)
        fields.forEach { checkedByte(it, "packet field") }
        val checksum = fields.sum() and 0xff
        return intArrayOf(Prefix, opcode, payload1, payload2, payload3, payload4, checksum, Padding, Terminator)
            .map(Int::toByte)
            .toByteArray()
    }

    private fun checkedByte(value: Int, name: String): Int {
        require(value in 0x00..0xff) { "$name must be an unsigned byte: $value" }
        return value
    }

    enum class Effect(val ledBleId: Int, val elkBledomId: Int) {
        Jump7Colors(0x87, 0x87),
        Fade7Colors(0x8a, 0x8a),
    }
}
