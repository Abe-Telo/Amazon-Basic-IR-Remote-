package com.example.amazonbasicsirremote;

import android.content.Context;
import android.hardware.ConsumerIrManager;

final class IrTransmitter {
    private final ConsumerIrManager manager;

    IrTransmitter(Context context) {
        manager = (ConsumerIrManager) context.getSystemService(Context.CONSUMER_IR_SERVICE);
    }

    boolean hasEmitter() {
        return manager != null && manager.hasIrEmitter();
    }

    void transmit(int[] commandBytes) {
        transmitRaw(MideaIrEncoder.FREQUENCY_HZ, MideaIrEncoder.encodeMsb(commandBytes));
    }

    void transmitRaw(int frequencyHz, int[] pattern) {
        if (!hasEmitter()) throw new IllegalStateException("This phone does not report an IR blaster.");
        if (pattern.length == 0) throw new IllegalStateException("LED IR code has not been captured yet.");
        manager.transmit(frequencyHz, pattern);
    }
}
