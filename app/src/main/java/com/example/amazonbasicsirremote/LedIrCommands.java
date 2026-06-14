package com.example.amazonbasicsirremote;

final class LedIrCommands {
    enum Command {
        POWER_TOGGLE("Power Toggle"),
        BRIGHTNESS_DOWN("Brightness Down"),
        BRIGHTNESS_UP("Brightness Up");

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

    static int[] forCommand(Command command) {
        switch (command) {
            case POWER_TOGGLE:
                return new int[]{0x00, 0xFF, 0x02, 0xFD};
            case BRIGHTNESS_DOWN:
                return new int[]{0x00, 0xFF, 0x04, 0xFB};
            case BRIGHTNESS_UP:
                return new int[]{0x00, 0xFF, 0x05, 0xFA};
            default:
                throw new IllegalArgumentException("Unsupported LED command: " + command);
        }
    }
}
