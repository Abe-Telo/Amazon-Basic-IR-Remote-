package com.example.amazonbasicsirremote;

import java.util.UUID;

/** Java bridge for the LED BLE packet builder while the Android module is Java-first. */
final class LedBleCommandsJava {
    static final UUID SERVICE_UUID = UUID.fromString("0000fff0-0000-1000-8000-00805f9b34fb");
    static final UUID WRITE_CHARACTERISTIC_UUID = UUID.fromString("0000fff3-0000-1000-8000-00805f9b34fb");
    static final UUID NOTIFY_CHARACTERISTIC_UUID = UUID.fromString("0000fff4-0000-1000-8000-00805f9b34fb");

    private LedBleCommandsJava() {}

    static byte[] power(boolean on) {
        return packet(0x04, on ? 0x01 : 0x00, 0x00, 0x00, 0x00);
    }

    static byte[] rgb(int red, int green, int blue) {
        return packet(0x07, checkedByte(red, "red"), checkedByte(green, "green"), checkedByte(blue, "blue"), 0x00);
    }

    static byte[] brightness(int percent) {
        if (percent < 0 || percent > 100) throw new IllegalArgumentException("Brightness percent must be in 0..100: " + percent);
        return packet(0x01, percent, 0x00, 0x00, 0x00);
    }

    static byte[] effect(Effect effect) {
        return effect(effect, 0x03);
    }

    static byte[] effect(Effect effect, int speed) {
        if (speed < 0x01 || speed > 0x1f) throw new IllegalArgumentException("Effect speed must be in 1..31: " + speed);
        return packet(0x05, effect.id, speed, 0x00, 0x00);
    }

    static byte[] packet(int opcode, int payload1, int payload2, int payload3, int payload4) {
        int[] fields = {opcode, payload1, payload2, payload3, payload4};
        for (int field : fields) checkedByte(field, "packet field");
        int checksum = (opcode + payload1 + payload2 + payload3 + payload4) & 0xff;
        return new byte[] {(byte) 0x7e, (byte) opcode, (byte) payload1, (byte) payload2, (byte) payload3, (byte) payload4, (byte) checksum, 0x00, (byte) 0xef};
    }

    private static int checkedByte(int value, String name) {
        if (value < 0x00 || value > 0xff) throw new IllegalArgumentException(name + " must be an unsigned byte: " + value);
        return value;
    }

    enum Effect {
        JUMP_7_COLORS(0x87),
        FADE_7_COLORS(0x8a);

        private final int id;

        Effect(int id) { this.id = id; }
    }
}
