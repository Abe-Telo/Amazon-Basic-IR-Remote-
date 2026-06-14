package com.example.amazonbasicsirremote;

import android.content.Context;
import android.hardware.ConsumerIrManager;

public class IrTransmitter {
    public static final int DEFAULT_FREQUENCY = 38_000;

    private final ConsumerIrManager consumerIrManager;

    public IrTransmitter(Context context) {
        consumerIrManager = (ConsumerIrManager) context.getSystemService(Context.CONSUMER_IR_SERVICE);
    }

    public boolean hasIrEmitter() {
        return consumerIrManager != null && consumerIrManager.hasIrEmitter();
    }

    public void transmit(int[] pattern) {
        transmit(pattern, DEFAULT_FREQUENCY);
    }

    public void transmit(int[] pattern, int frequency) {
        if (!hasIrEmitter()) {
            throw new IllegalStateException("Device does not have an IR blaster.");
        }

        consumerIrManager.transmit(frequency, pattern);
    }
}
