package com.example.amazonbasicsirremote;

import android.app.Activity;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
    private DeviceType selectedDevice = DeviceType.AMAZON_BASICS_AC;
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
    private IrTransmitter irTransmitter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        irTransmitter = new IrTransmitter(this);
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

    private LinearLayout buildLedControls() {
        LinearLayout controls = new LinearLayout(this);
        controls.setOrientation(LinearLayout.VERTICAL);
        controls.setGravity(Gravity.CENTER_HORIZONTAL);

        ledStateLabel = new TextView(this);
        ledStateLabel.setGravity(Gravity.CENTER);
        controls.addView(ledStateLabel, fullWidth());

        Button power = button("LED Power Toggle");
        power.setOnClickListener(v -> sendLedCommand(LedIrCommands.Command.POWER_TOGGLE));
        controls.addView(power, fullWidth());

        LinearLayout brightnessRow = new LinearLayout(this);
        brightnessRow.setGravity(Gravity.CENTER);
        Button brightnessDown = button("− Brightness");
        brightnessDown.setOnClickListener(v -> sendLedCommand(LedIrCommands.Command.BRIGHTNESS_DOWN));
        Button brightnessUp = button("+ Brightness");
        brightnessUp.setOnClickListener(v -> sendLedCommand(LedIrCommands.Command.BRIGHTNESS_UP));
        brightnessRow.addView(brightnessDown);
        brightnessRow.addView(brightnessUp);
        controls.addView(brightnessRow, fullWidth());

        return controls;
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
        try {
            irTransmitter.transmit(command);
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        } catch (IllegalStateException ex) {
            Toast.makeText(this, ex.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void sendLedCommand(LedIrCommands.Command command) {
        LedIrCommands.forCommand(command);
        switch (command) {
            case POWER_TOGGLE:
                ledState.setPowerOn(!ledState.isPowerOn());
                break;
            case BRIGHTNESS_DOWN:
                ledState.setBrightnessPercent(Math.max(0, ledState.getBrightnessPercent() - 10));
                break;
            case BRIGHTNESS_UP:
                ledState.setBrightnessPercent(Math.min(100, ledState.getBrightnessPercent() + 10));
                break;
        }
        Toast.makeText(this, command + " staged; BLE transport not implemented yet", Toast.LENGTH_SHORT).show();
        updateStatus();
    }

    private void updateDeviceControls() {
        if (acControls != null) acControls.setVisibility(selectedDevice == DeviceType.AMAZON_BASICS_AC ? View.VISIBLE : View.GONE);
        if (ledControls != null) ledControls.setVisibility(selectedDevice == DeviceType.LED_BLE_CONTROLLER ? View.VISIBLE : View.GONE);
    }

    private void updateStatus() {
        if (tempLabel != null) tempLabel.setText(acState.getTemperatureF() + "°F");
        if (ledStateLabel != null) ledStateLabel.setText(ledState.describe());
        if (status != null) {
            if (selectedDevice == DeviceType.AMAZON_BASICS_AC) {
                status.setText(irTransmitter.hasEmitter() ? "AC selected / IR blaster detected" : "AC selected / No IR blaster detected");
            } else {
                status.setText(LedBleController.isAvailable() ? "LED selected / BLE controller ready" : "LED selected / BLE controller not implemented");
            }
        }
    }

    private TextView label(String text) { TextView v = new TextView(this); v.setText(text); v.setTextSize(16); return v; }
    private Button button(String text) { Button b = new Button(this); b.setText(text); return b; }
    private LinearLayout.LayoutParams fullWidth() { return new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT); }
}
