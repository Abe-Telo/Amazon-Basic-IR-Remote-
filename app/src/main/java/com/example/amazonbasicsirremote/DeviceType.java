package com.example.amazonbasicsirremote;

import java.util.EnumSet;
import java.util.Set;

enum DeviceType {
    AMAZON_BASICS_AC("Amazon Basics AC", EnumSet.of(ControlTransport.IR)),
    LED_IR_CONTROLLER("LED Controller", EnumSet.of(ControlTransport.IR, ControlTransport.BT));

    private final String displayName;
    private final Set<ControlTransport> supportedTransports;

    DeviceType(String displayName, Set<ControlTransport> supportedTransports) {
        this.displayName = displayName;
        this.supportedTransports = supportedTransports;
    }

    boolean supports(ControlTransport transport) {
        return supportedTransports.contains(transport);
    }

    boolean hasMultipleTransports() {
        return supportedTransports.size() > 1;
    }

    ControlTransport defaultTransport() {
        return supportedTransports.iterator().next();
    }

    @Override
    public String toString() {
        return displayName;
    }
}
