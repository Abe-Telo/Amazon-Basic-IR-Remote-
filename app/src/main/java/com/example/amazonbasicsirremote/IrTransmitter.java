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
        if (!hasEmitter()) throw new IllegalStateException("This phone does not report an IR blaster.");
        manager.transmit(MideaIrEncoder.FREQUENCY_HZ, MideaIrEncoder.encodeMsb(commandBytes));
    }
}
