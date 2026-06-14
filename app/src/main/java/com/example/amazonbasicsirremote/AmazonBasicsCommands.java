package com.example.amazonbasicsirremote;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

final class AmazonBasicsCommands {
    static final int MIN_TEMP_F = 62;
    static final int MAX_TEMP_F = 86;

    enum Mode { AUTO, COOL, DRY, FAN }
    enum FanSpeed { AUTO, LOW, MEDIUM, HIGH }

    private static final Map<String, int[]> COMMANDS = buildCommands();

    private AmazonBasicsCommands() {}

    static int[] forState(Mode mode, FanSpeed fanSpeed, int temperatureF) {
        String key = key(mode, fanSpeed, temperatureF);
        int[] command = COMMANDS.get(key);
        if (command == null) throw new IllegalArgumentException("Unsupported command: " + key);
        return Arrays.copyOf(command, command.length);
    }

    static int[] ledToggle() { return copy("LED_Toggle"); }
    static int[] energySaverToggle() { return copy("EnergySaver_Toggle"); }

    static Map<String, int[]> allCommands() { return COMMANDS; }

    private static int[] copy(String key) {
        int[] command = COMMANDS.get(key);
        return Arrays.copyOf(command, command.length);
    }

    private static String key(Mode mode, FanSpeed fanSpeed, int temperatureF) {
        if (mode == Mode.FAN) return "Fan_" + fanSpeed.name();
        if (temperatureF < MIN_TEMP_F || temperatureF > MAX_TEMP_F) {
            throw new IllegalArgumentException("Temperature must be 62-86°F");
        }
        return mode.name() + "_" + fanSpeed.name() + "_" + temperatureF + "F";
    }

    private static Map<String, int[]> buildCommands() {
        LinkedHashMap<String, int[]> map = new LinkedHashMap<>();
        addRange(map, Mode.AUTO, FanSpeed.AUTO, 130, new int[]{108,109,111,110,104,105,107,106,100,101,103,102,96,97,99,98,116,117,119,118,112,113,115,114,120});
        addRange(map, Mode.COOL, FanSpeed.AUTO, 160, new int[]{78,79,76,77,74,75,72,73,70,71,68,69,66,67,64,65,86,87,84,85,82,83,80,81,90});
        addRange(map, Mode.DRY, FanSpeed.AUTO, 129, new int[]{111,110,109,108,107,106,105,104,103,102,101,100,99,98,97,96,119,118,117,116,115,114,113,112,123});
        addRange(map, Mode.COOL, FanSpeed.LOW, 136, new int[]{102,103,100,101,98,99,96,97,106,107,104,105,108,109,111,110,122,123,120,121,124,125,127,126,114});
        addRange(map, Mode.COOL, FanSpeed.MEDIUM, 144, new int[]{118,119,116,117,114,115,112,113,122,123,120,121,124,125,127,126,102,103,100,101,98,99,96,97,106});
        addRange(map, Mode.COOL, FanSpeed.HIGH, 152, new int[]{122,123,120,121,124,125,127,126,114,115,112,113,116,117,119,118,106,107,104,105,108,109,111,110,98});
        map.put("Fan_AUTO", new int[]{161,164,126,255,255,91});
        map.put("Fan_LOW", new int[]{161,140,126,255,255,115});
        map.put("Fan_MEDIUM", new int[]{161,148,126,255,255,107});
        map.put("Fan_HIGH", new int[]{161,156,126,255,255,99});
        map.put("LED_Toggle", new int[]{162,8,255,255,255,117});
        map.put("EnergySaver_Toggle", new int[]{162,2,255,255,255,126});
        return Collections.unmodifiableMap(map);
    }

    private static void addRange(Map<String, int[]> map, Mode mode, FanSpeed fanSpeed, int modeFanByte, int[] checksums) {
        for (int i = 0; i < checksums.length; i++) {
            int temp = MIN_TEMP_F + i;
            map.put(key(mode, fanSpeed, temp), new int[]{161, modeFanByte, 96 + i, 255, 255, checksums[i]});
        }
    }
}
