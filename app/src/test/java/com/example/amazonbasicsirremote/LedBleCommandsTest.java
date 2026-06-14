package com.example.amazonbasicsirremote;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class LedBleCommandsTest {
    @Test public void buildsPowerPackets() {
        assertHex("7e 04 01 00 00 00 05 00 ef", LedBleCommandsJava.power(true));
        assertHex("7e 04 00 00 00 00 04 00 ef", LedBleCommandsJava.power(false));
    }

    @Test public void buildsRgbPacketsWithWrappedChecksum() {
        assertHex("7e 07 ff 00 00 00 06 00 ef", LedBleCommandsJava.rgb(255, 0, 0));
        assertHex("7e 07 00 ff 00 00 06 00 ef", LedBleCommandsJava.rgb(0, 255, 0));
        assertHex("7e 07 00 00 ff 00 06 00 ef", LedBleCommandsJava.rgb(0, 0, 255));
        assertHex("7e 07 ff ff ff 00 04 00 ef", LedBleCommandsJava.rgb(255, 255, 255));
    }

    @Test public void buildsBrightnessPackets() {
        assertHex("7e 01 00 00 00 00 01 00 ef", LedBleCommandsJava.brightness(0));
        assertHex("7e 01 32 00 00 00 33 00 ef", LedBleCommandsJava.brightness(50));
        assertHex("7e 01 64 00 00 00 65 00 ef", LedBleCommandsJava.brightness(100));
    }

    @Test public void buildsEffectPackets() {
        assertHex("7e 05 87 03 00 00 8f 00 ef", LedBleCommandsJava.effect(LedBleCommandsJava.Effect.JUMP_7_COLORS));
        assertHex("7e 05 8a 03 00 00 92 00 ef", LedBleCommandsJava.effect(LedBleCommandsJava.Effect.FADE_7_COLORS));
        assertHex("7e 05 87 1f 00 00 ab 00 ef", LedBleCommandsJava.effect(LedBleCommandsJava.Effect.JUMP_7_COLORS, 0x1f));
    }

    @Test public void exposesCapturedUuids() {
        assertEquals("0000fff0-0000-1000-8000-00805f9b34fb", LedBleCommandsJava.SERVICE_UUID.toString());
        assertEquals("0000fff3-0000-1000-8000-00805f9b34fb", LedBleCommandsJava.WRITE_CHARACTERISTIC_UUID.toString());
        assertEquals("0000fff4-0000-1000-8000-00805f9b34fb", LedBleCommandsJava.NOTIFY_CHARACTERISTIC_UUID.toString());
    }

    private void assertHex(String expected, byte[] actual) {
        String[] parts = expected.split(" ");
        byte[] expectedBytes = new byte[parts.length];
        for (int i = 0; i < parts.length; i++) expectedBytes[i] = (byte) Integer.parseInt(parts[i], 16);
        assertArrayEquals(expectedBytes, actual);
    }
}
