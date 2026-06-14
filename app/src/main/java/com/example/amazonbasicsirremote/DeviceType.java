package com.example.amazonbasicsirremote;

enum DeviceType {
    AMAZON_BASICS_AC("Amazon Basics AC"),
    LED_BLE_CONTROLLER("LED BLE Controller");

    private final String displayName;

    DeviceType(String displayName) {
        this.displayName = displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
