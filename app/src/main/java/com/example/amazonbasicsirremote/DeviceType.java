package com.example.amazonbasicsirremote;

enum DeviceType {
    AMAZON_BASICS_AC("Amazon Basics AC"),
    LED_IR_CONTROLLER("LED IR Controller");

    private final String displayName;

    DeviceType(String displayName) {
        this.displayName = displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
