package com.example.amazonbasicsirremote;

import java.util.ArrayList;
import java.util.List;

final class MideaIrEncoder {
    static final int FREQUENCY_HZ = 38_000;
    private static final int HEADER = 4480;
    private static final int MARK = 560;
    private static final int ZERO = 560;
    private static final int ONE = 1680;
    private static final int GAP = 5320;

    private MideaIrEncoder() {}

    static int[] encodeMsb(int[] bytes) {
        List<Integer> pulses = new ArrayList<>();
        addFrame(pulses, bytes, false);
        pulses.add(GAP);
        addFrame(pulses, bytes, true);
        return toIntArray(pulses);
    }

    private static void addFrame(List<Integer> pulses, int[] bytes, boolean inverted) {
        pulses.add(HEADER);
        pulses.add(HEADER);
        for (int value : bytes) {
            int b = inverted ? (~value & 0xFF) : (value & 0xFF);
            for (int bit = 7; bit >= 0; bit--) {
                pulses.add(MARK);
                pulses.add(((b >> bit) & 1) == 1 ? ONE : ZERO);
            }
        }
        pulses.add(MARK);
    }

    private static int[] toIntArray(List<Integer> values) {
        int[] output = new int[values.size()];
        for (int i = 0; i < values.size(); i++) output[i] = values.get(i);
        return output;
    }
}
