package com.example.amazonbasicsirremote;

final class AcRemoteState {
    private AmazonBasicsCommands.Mode mode;
    private AmazonBasicsCommands.FanSpeed fanSpeed;
    private int temperatureF;

    AcRemoteState(AmazonBasicsCommands.Mode mode, AmazonBasicsCommands.FanSpeed fanSpeed, int temperatureF) {
        this.mode = mode;
        this.fanSpeed = fanSpeed;
        setTemperatureF(temperatureF);
    }

    AmazonBasicsCommands.Mode getMode() {
        return mode;
    }

    void setMode(AmazonBasicsCommands.Mode mode) {
        this.mode = mode;
    }

    AmazonBasicsCommands.FanSpeed getFanSpeed() {
        return fanSpeed;
    }

    void setFanSpeed(AmazonBasicsCommands.FanSpeed fanSpeed) {
        this.fanSpeed = fanSpeed;
    }

    int getTemperatureF() {
        return temperatureF;
    }

    void setTemperatureF(int temperatureF) {
        if (temperatureF < AmazonBasicsCommands.MIN_TEMP_F || temperatureF > AmazonBasicsCommands.MAX_TEMP_F) {
            throw new IllegalArgumentException("Temperature must be 62-86°F");
        }
        this.temperatureF = temperatureF;
    }

    int[] toCommandBytes() {
        return AmazonBasicsCommands.forState(mode, fanSpeed, temperatureF);
    }

    String describe() {
        return mode + " / " + fanSpeed + " / " + temperatureF + "°F";
    }
}
