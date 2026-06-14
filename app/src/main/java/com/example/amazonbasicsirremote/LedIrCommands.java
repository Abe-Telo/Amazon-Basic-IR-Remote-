package com.example.amazonbasicsirremote;

/**
 * LED-controller IR command table.
 *
 * No LED IR timings are checked in yet because the LED controller remote must be
 * captured first and its protocol identified. Add captured raw pulse durations
 * here only after verifying they match the physical LED remote. Do not route
 * these through the Midea AC encoder unless the capture matches that frame
 * format.
 */
final class LedIrCommands {
    static final int CARRIER_FREQUENCY_HZ = 38_000;

    enum Command {
        POWER_ON_OFF("Power On/Off"),
        BRIGHTNESS_UP("Brightness Up"),
        BRIGHTNESS_DOWN("Brightness Down"),
        RED("Red"),
        GREEN("Green"),
        BLUE("Blue"),
        WHITE("White"),
        MODE_EFFECT("Mode/Effect"),
        SPEED_UP("Speed Up"),
        SPEED_DOWN("Speed Down");

        private final String displayName;

        Command(String displayName) {
            this.displayName = displayName;
        }

        @Override
        public String toString() {
            return displayName;
        }
    }

    private LedIrCommands() {}

    static boolean hasCapturedCode(Command command) {
        return rawPulsesFor(command).length > 0;
    }

    static int[] rawPulsesFor(Command command) {
        switch (command) {
            case POWER_ON_OFF:
                return new int[0];
            case BRIGHTNESS_UP:
                return new int[0];
            case BRIGHTNESS_DOWN:
                return new int[0];
            case RED:
                return new int[0];
            case GREEN:
                return new int[0];
            case BLUE:
                return new int[0];
            case WHITE:
                return new int[0];
            case MODE_EFFECT:
                return new int[0];
            case SPEED_UP:
                return new int[0];
            case SPEED_DOWN:
                return new int[0];
            default:
                throw new IllegalArgumentException("Unsupported LED command: " + command);
        }
    }
}
