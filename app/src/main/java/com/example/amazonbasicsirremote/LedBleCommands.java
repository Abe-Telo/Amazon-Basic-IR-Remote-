package com.example.amazonbasicsirremote;

import java.util.UUID;

/** Java bridge for the LED BLE packet builder while the Android module is Java-first. */
final class LedBleCommandsJava {
    static final UUID SERVICE_UUID = LedBleCommands.INSTANCE.getServiceUuid();
    static final UUID WRITE_CHARACTERISTIC_UUID = LedBleCommands.INSTANCE.getWriteCharacteristicUuid();
    static final UUID NOTIFY_CHARACTERISTIC_UUID = LedBleCommands.INSTANCE.getNotifyCharacteristicUuid();

    private LedBleCommandsJava() {}

    static byte[] power(boolean on) {
        return LedBleCommands.INSTANCE.power(on, LedBleCommands.INSTANCE.getDefaultProfile());
    }

    static byte[] power(boolean on, LedBleProfile profile) {
        return LedBleCommands.INSTANCE.power(on, profile);
    }

    static byte[] rgb(int red, int green, int blue) {
        return LedBleCommands.INSTANCE.rgb(red, green, blue, LedBleCommands.INSTANCE.getDefaultProfile());
    }

    static byte[] rgb(int red, int green, int blue, LedBleProfile profile) {
        return LedBleCommands.INSTANCE.rgb(red, green, blue, profile);
    }

    static byte[] brightness(int percent) {
        return LedBleCommands.INSTANCE.brightness(percent, LedBleCommands.INSTANCE.getDefaultProfile());
    }

    static byte[] brightness(int percent, LedBleProfile profile) {
        return LedBleCommands.INSTANCE.brightness(percent, profile);
    }

    static byte[] effect(Effect effect) {
        return effect(effect, 0x03, LedBleCommands.INSTANCE.getDefaultProfile());
    }

    static byte[] effect(Effect effect, int speed) {
        return effect(effect, speed, LedBleCommands.INSTANCE.getDefaultProfile());
    }

    static byte[] effect(Effect effect, int speed, LedBleProfile profile) {
        return LedBleCommands.INSTANCE.effect(effect.kotlinEffect, speed, profile);
    }

    enum Effect {
        JUMP_7_COLORS(LedBleCommands.Effect.Jump7Colors),
        FADE_7_COLORS(LedBleCommands.Effect.Fade7Colors);

        private final LedBleCommands.Effect kotlinEffect;

        Effect(LedBleCommands.Effect kotlinEffect) { this.kotlinEffect = kotlinEffect; }
    }
}
