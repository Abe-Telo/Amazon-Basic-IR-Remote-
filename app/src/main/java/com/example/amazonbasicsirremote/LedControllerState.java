package com.example.amazonbasicsirremote;

final class LedControllerState {
    private boolean powerOn = true;
    private int brightnessPercent = 100;

    boolean isPowerOn() {
        return powerOn;
    }

    void setPowerOn(boolean powerOn) {
        this.powerOn = powerOn;
    }

    int getBrightnessPercent() {
        return brightnessPercent;
    }

    void setBrightnessPercent(int brightnessPercent) {
        if (brightnessPercent < 0 || brightnessPercent > 100) {
            throw new IllegalArgumentException("Brightness must be 0-100%");
        }
        this.brightnessPercent = brightnessPercent;
    }

    String describe() {
        return (powerOn ? "On" : "Off") + " / " + brightnessPercent + "% brightness";
    }
}
