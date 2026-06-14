package com.example.amazonbasicsirremote;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.Map;

public class MainActivity extends Activity {
    private DeviceType selectedDevice = DeviceType.AMAZON_BASICS_AC;
    private ControlTransport selectedTransport = ControlTransport.IR;
    private final AcRemoteState acState = new AcRemoteState(
            AmazonBasicsCommands.Mode.COOL,
            AmazonBasicsCommands.FanSpeed.AUTO,
            72
    );
    private final LedControllerState ledState = new LedControllerState();

    private TextView status;
    private TextView tempLabel;
    private TextView ledStateLabel;
    private LinearLayout acControls;
    private LinearLayout ledControls;
    private RadioGroup transportSelector;
    private RadioButton irTransportButton;
    private RadioButton btTransportButton;
    private final Map<LedIrCommands.Command, Button> ledButtons = new EnumMap<>(LedIrCommands.Command.class);
    private IrTransmitter irTransmitter;
    private LedBleClient ledBleClient;
    private final Handler bluetoothHandler = new Handler(Looper.getMainLooper());
    private String bluetoothStatus = "Bluetooth scanning";
    private static final int REQUEST_BLE_PERMISSIONS = 1001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestBlePermissionsIfNeeded();
        irTransmitter = new IrTransmitter(this);
        ledBleClient = new LedBleClient(this, new LedBleClient.Listener() {
            @Override public void onStatusChanged(String status) {
                bluetoothStatus = normalizeBluetoothStatus(status);
                runOnUiThread(() -> updateStatus());
            }
            @Override public void onDeviceFound(String name) {
                bluetoothStatus = "Bluetooth connected";
                runOnUiThread(() -> updateStatus());
            }
            @Override public void onServicesDiscovered() {
                bluetoothStatus = "Bluetooth connected";
                runOnUiThread(() -> updateStatus());
            }
        });
        setContentView(buildUi());
        updateDeviceControls();
        updateStatus();
    }

    private View buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setPadding(32, 48, 32, 32);

        TextView title = new TextView(this);
        title.setText("Amazon Basics IR Remote");
        title.setTextSize(24);
        title.setGravity(Gravity.CENTER);
        root.addView(title, fullWidth());

        status = new TextView(this);
        status.setGravity(Gravity.CENTER);
        root.addView(status, fullWidth());

        root.addView(label("Device"), fullWidth());
        Spinner deviceSpinner = new Spinner(this);
        deviceSpinner.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, DeviceType.values()));
        deviceSpinner.setSelection(selectedDevice.ordinal());
        deviceSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedDevice = DeviceType.values()[position];
                updateDeviceControls();
                updateStatus();
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });
        root.addView(deviceSpinner, fullWidth());

        acControls = buildAcControls();
        root.addView(acControls, fullWidth());

        ledControls = buildLedControls();
        root.addView(ledControls, fullWidth());

        transportSelector = buildTransportSelector();
        root.addView(transportSelector, fullWidth());

        return root;
    }

    private LinearLayout buildAcControls() {
        LinearLayout controls = new LinearLayout(this);
        controls.setOrientation(LinearLayout.VERTICAL);
        controls.setGravity(Gravity.CENTER_HORIZONTAL);

        tempLabel = new TextView(this);
        tempLabel.setTextSize(48);
        tempLabel.setGravity(Gravity.CENTER);
        controls.addView(tempLabel, fullWidth());

        LinearLayout tempRow = new LinearLayout(this);
        tempRow.setGravity(Gravity.CENTER);
        Button down = button("− Temp");
        down.setOnClickListener(v -> {
            if (acState.getTemperatureF() > AmazonBasicsCommands.MIN_TEMP_F) {
                acState.setTemperatureF(acState.getTemperatureF() - 1);
            }
            sendAcState();
        });
        Button up = button("+ Temp");
        up.setOnClickListener(v -> {
            if (acState.getTemperatureF() < AmazonBasicsCommands.MAX_TEMP_F) {
                acState.setTemperatureF(acState.getTemperatureF() + 1);
            }
            sendAcState();
        });
        tempRow.addView(down);
        tempRow.addView(up);
        controls.addView(tempRow, fullWidth());

        controls.addView(label("Mode"), fullWidth());
        Spinner modeSpinner = new Spinner(this);
        modeSpinner.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, AmazonBasicsCommands.Mode.values()));
        modeSpinner.setSelection(acState.getMode().ordinal());
        modeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                acState.setMode(AmazonBasicsCommands.Mode.values()[position]);
                updateStatus();
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });
        controls.addView(modeSpinner, fullWidth());

        controls.addView(label("Fan speed"), fullWidth());
        Spinner fanSpinner = new Spinner(this);
        fanSpinner.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, AmazonBasicsCommands.FanSpeed.values()));
        fanSpinner.setSelection(acState.getFanSpeed().ordinal());
        fanSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                acState.setFanSpeed(AmazonBasicsCommands.FanSpeed.values()[position]);
                updateStatus();
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });
        controls.addView(fanSpinner, fullWidth());

        Button send = button("Send current AC setting");
        send.setOnClickListener(v -> sendAcState());
        controls.addView(send, fullWidth());

        Button led = button("AC Display LED Toggle");
        led.setOnClickListener(v -> sendAcCommand(AmazonBasicsCommands.ledToggle(), "AC display LED toggle sent"));
        controls.addView(led, fullWidth());

        Button energy = button("Energy Saver Toggle");
        energy.setOnClickListener(v -> sendAcCommand(AmazonBasicsCommands.energySaverToggle(), "Energy saver toggle sent"));
        controls.addView(energy, fullWidth());

        return controls;
    }

    private RadioGroup buildTransportSelector() {
        RadioGroup group = new RadioGroup(this);
        group.setOrientation(LinearLayout.HORIZONTAL);
        group.setGravity(Gravity.CENTER);

        irTransportButton = new RadioButton(this);
        irTransportButton.setText("IR");
        irTransportButton.setId(View.generateViewId());
        group.addView(irTransportButton);

        btTransportButton = new RadioButton(this);
        btTransportButton.setText("BT");
        btTransportButton.setId(View.generateViewId());
        group.addView(btTransportButton);

        group.setOnCheckedChangeListener((radioGroup, checkedId) -> {
            selectedTransport = checkedId == btTransportButton.getId() ? ControlTransport.BT : ControlTransport.IR;
            updateLedButtonStates();
            if (selectedTransport == ControlTransport.BT) {
                bluetoothStatus = "Bluetooth scanning";
                if (hasBlePermissions()) {
                    ledBleClient.connectFirstKnownLedDevice();
                } else {
                    requestBlePermissionsIfNeeded();
                    bluetoothStatus = "BLE permissions are required before scanning";
                }
                bluetoothHandler.postDelayed(() -> {
                    if (selectedTransport == ControlTransport.BT && "Bluetooth scanning".equals(bluetoothStatus)) {
                        ledBleClient.stopScan();
                        bluetoothStatus = "No BLE device found";
                        updateStatus();
                    }
                }, 10000);
            } else {
                ledBleClient.stopScan();
                bluetoothHandler.removeCallbacksAndMessages(null);
            }
            updateStatus();
        });
        return group;
    }

    private LinearLayout buildLedControls() {
        LinearLayout controls = new LinearLayout(this);
        controls.setOrientation(LinearLayout.VERTICAL);
        controls.setGravity(Gravity.CENTER_HORIZONTAL);

        ledStateLabel = new TextView(this);
        ledStateLabel.setGravity(Gravity.CENTER);
        controls.addView(ledStateLabel, fullWidth());

        controls.addView(label("LED commands"), fullWidth());

        addLedButton(controls, LedIrCommands.Command.POWER_ON_OFF);

        LinearLayout brightnessRow = new LinearLayout(this);
        brightnessRow.setGravity(Gravity.CENTER);
        addLedButton(brightnessRow, LedIrCommands.Command.BRIGHTNESS_DOWN);
        addLedButton(brightnessRow, LedIrCommands.Command.BRIGHTNESS_UP);
        controls.addView(brightnessRow, fullWidth());

        LinearLayout colorRow = new LinearLayout(this);
        colorRow.setGravity(Gravity.CENTER);
        addLedButton(colorRow, LedIrCommands.Command.RED);
        addLedButton(colorRow, LedIrCommands.Command.GREEN);
        addLedButton(colorRow, LedIrCommands.Command.BLUE);
        addLedButton(colorRow, LedIrCommands.Command.WHITE);
        controls.addView(colorRow, fullWidth());

        LinearLayout effectRow = new LinearLayout(this);
        effectRow.setGravity(Gravity.CENTER);
        addLedButton(effectRow, LedIrCommands.Command.MODE_EFFECT);
        addLedButton(effectRow, LedIrCommands.Command.SPEED_DOWN);
        addLedButton(effectRow, LedIrCommands.Command.SPEED_UP);
        controls.addView(effectRow, fullWidth());

        return controls;
    }

    private void addLedButton(LinearLayout parent, LedIrCommands.Command command) {
        Button button = button(command.toString());
        ledButtons.put(command, button);
        button.setOnClickListener(v -> sendLedCommand(command));
        parent.addView(button);
    }

    private void sendAcState() {
        try {
            sendAcCommand(acState.toCommandBytes(), "Sent " + acState.describe());
        } catch (IllegalArgumentException ex) {
            Toast.makeText(this, ex.getMessage(), Toast.LENGTH_LONG).show();
        }
        updateStatus();
    }

    private void sendAcCommand(int[] command, String message) {
        if (selectedTransport != ControlTransport.IR) {
            Toast.makeText(this, "Amazon Basics AC supports IR only", Toast.LENGTH_LONG).show();
            selectedTransport = ControlTransport.IR;
            syncTransportSelector();
            updateStatus();
            return;
        }
        try {
            irTransmitter.transmit(command);
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        } catch (IllegalStateException ex) {
            Toast.makeText(this, ex.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void sendLedCommand(LedIrCommands.Command command) {
        if (selectedTransport == ControlTransport.BT) {
            sendBleCommand(command);
            Toast.makeText(this, command + " sent over BT", Toast.LENGTH_SHORT).show();
            updateLedState(command);
            updateStatus();
            return;
        }
        try {
            irTransmitter.transmitRaw(LedIrCommands.CARRIER_FREQUENCY_HZ, LedIrCommands.rawPulsesFor(command));
            Toast.makeText(this, command + " sent", Toast.LENGTH_SHORT).show();
            updateLedState(command);
        } catch (IllegalStateException ex) {
            Toast.makeText(this, ex.getMessage(), Toast.LENGTH_LONG).show();
        }
        updateStatus();
    }

    private void sendBleCommand(LedIrCommands.Command command) {
        switch (command) {
            case POWER_ON_OFF:
                ledBleClient.sendPower(!ledState.isPowerOn());
                break;
            case BRIGHTNESS_DOWN:
                ledBleClient.sendBrightness(Math.max(0, ledState.getBrightnessPercent() - 10));
                break;
            case BRIGHTNESS_UP:
                ledBleClient.sendBrightness(Math.min(100, ledState.getBrightnessPercent() + 10));
                break;
            case RED:
                ledBleClient.sendColor(255, 0, 0);
                break;
            case GREEN:
                ledBleClient.sendColor(0, 255, 0);
                break;
            case BLUE:
                ledBleClient.sendColor(0, 0, 255);
                break;
            case WHITE:
                ledBleClient.sendColor(255, 255, 255);
                break;
            case MODE_EFFECT:
                ledBleClient.writeCommand(LedBleCommandsJava.effect(LedBleCommandsJava.Effect.JUMP_7_COLORS));
                break;
            case SPEED_DOWN:
                ledBleClient.writeCommand(LedBleCommandsJava.effect(LedBleCommandsJava.Effect.FADE_7_COLORS, 0x01));
                break;
            case SPEED_UP:
                ledBleClient.writeCommand(LedBleCommandsJava.effect(LedBleCommandsJava.Effect.FADE_7_COLORS, 0x1f));
                break;
            default:
                throw new IllegalArgumentException("No BLE command mapped for " + command);
        }
    }

    private void updateLedState(LedIrCommands.Command command) {
        switch (command) {
            case POWER_ON_OFF:
                ledState.setPowerOn(!ledState.isPowerOn());
                break;
            case BRIGHTNESS_DOWN:
                ledState.setBrightnessPercent(Math.max(0, ledState.getBrightnessPercent() - 10));
                break;
            case BRIGHTNESS_UP:
                ledState.setBrightnessPercent(Math.min(100, ledState.getBrightnessPercent() + 10));
                break;
        }
    }

    private void updateDeviceControls() {
        if (!selectedDevice.supports(selectedTransport)) {
            selectedTransport = selectedDevice.defaultTransport();
        }
        if (acControls != null) acControls.setVisibility(selectedDevice == DeviceType.AMAZON_BASICS_AC ? View.VISIBLE : View.GONE);
        if (ledControls != null) ledControls.setVisibility(selectedDevice == DeviceType.LED_IR_CONTROLLER ? View.VISIBLE : View.GONE);
        updateLedButtonStates();
        syncTransportSelector();
    }

    private void updateLedButtonStates() {
        for (Map.Entry<LedIrCommands.Command, Button> entry : ledButtons.entrySet()) {
            entry.getValue().setEnabled(selectedTransport == ControlTransport.BT || LedIrCommands.hasCapturedCode(entry.getKey()));
        }
    }

    private void syncTransportSelector() {
        if (transportSelector == null) return;
        transportSelector.setVisibility(selectedDevice.hasMultipleTransports() ? View.VISIBLE : View.GONE);
        irTransportButton.setEnabled(selectedDevice.supports(ControlTransport.IR));
        btTransportButton.setEnabled(selectedDevice.supports(ControlTransport.BT));
        transportSelector.check(selectedTransport == ControlTransport.BT ? btTransportButton.getId() : irTransportButton.getId());
        if (selectedTransport != ControlTransport.BT) {
            ledBleClient.stopScan();
            bluetoothHandler.removeCallbacksAndMessages(null);
        }
    }

    private void updateStatus() {
        if (tempLabel != null) tempLabel.setText(acState.getTemperatureF() + "°F");
        if (ledStateLabel != null) ledStateLabel.setText(ledState.describe());
        if (status != null) {
            if (selectedTransport == ControlTransport.BT) {
                status.setText(bluetoothStatus);
            } else {
                status.setText(irTransmitter.hasEmitter() ? "IR ready" : "No IR blaster detected");
            }
        }
    }

    private String normalizeBluetoothStatus(String status) {
        if (status == null) return "Bluetooth scanning";
        if (status.startsWith("Scanning")) return "Bluetooth scanning";
        if (status.contains("Connected") || status.contains("ready")) return "Bluetooth connected";
        if (status.contains("unavailable") || status.contains("No BLE")) return "No BLE device found";
        return status;
    }

    private void requestBlePermissionsIfNeeded() {
        if (hasBlePermissions()) return;
        ArrayList<String> permissions = new ArrayList<>();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.BLUETOOTH_SCAN);
            }
            if (checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.BLUETOOTH_CONNECT);
            }
        } else if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }
        if (!permissions.isEmpty()) {
            requestPermissions(permissions.toArray(new String[0]), REQUEST_BLE_PERMISSIONS);
        }
    }

    private boolean hasBlePermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED
                    && checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED;
        }
        return checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_BLE_PERMISSIONS) {
            bluetoothStatus = hasBlePermissions() ? "Bluetooth permissions granted" : "BLE permissions are required before scanning";
            if (selectedTransport == ControlTransport.BT && hasBlePermissions()) {
                ledBleClient.connectFirstKnownLedDevice();
            }
            updateStatus();
        }
    }

    @Override
    protected void onDestroy() {
        bluetoothHandler.removeCallbacksAndMessages(null);
        ledBleClient.disconnect();
        super.onDestroy();
    }

    private TextView label(String text) { TextView v = new TextView(this); v.setText(text); v.setTextSize(16); return v; }
    private Button button(String text) { Button b = new Button(this); b.setText(text); return b; }
    private LinearLayout.LayoutParams fullWidth() { return new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT); }
}
