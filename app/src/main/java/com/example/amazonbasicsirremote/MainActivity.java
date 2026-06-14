package com.example.amazonbasicsirremote;

import android.app.Activity;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Locale;

public class MainActivity extends Activity {
    private IrTransmitter irTransmitter;
    private final MideaIrEncoder irEncoder = new MideaIrEncoder();

    private RemoteState remoteState = new RemoteState(
            RemoteState.Mode.COOL,
            RemoteState.FanSpeed.AUTO,
            72
    );

    private TextView temperatureDisplay;
    private TextView commandPreview;
    private RadioGroup modeSelector;
    private RadioGroup fanSelector;
    private boolean updatingSelectors;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        irTransmitter = new IrTransmitter(this);
        setContentView(createRemoteLayout());
        renderState();
    }

    private LinearLayout createRemoteLayout() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        int padding = dp(24);
        root.setPadding(padding, padding, padding, padding);

        TextView title = new TextView(this);
        title.setText("Amazon Basics IR Remote");
        title.setTextSize(24);
        title.setGravity(Gravity.CENTER);
        root.addView(title, matchWrapParams());

        TextView subtitle = new TextView(this);
        subtitle.setText("Choose a state, then tap Send. Toggles transmit immediately.");
        subtitle.setGravity(Gravity.CENTER);
        subtitle.setPadding(0, dp(8), 0, dp(16));
        root.addView(subtitle, matchWrapParams());

        LinearLayout temperatureRow = horizontalRow();
        Button temperatureDown = new Button(this);
        temperatureDown.setText("Temp −");
        temperatureDown.setOnClickListener(view -> changeTemperature(-1));
        temperatureRow.addView(temperatureDown, weightedButtonParams());

        temperatureDisplay = new TextView(this);
        temperatureDisplay.setTextSize(32);
        temperatureDisplay.setGravity(Gravity.CENTER);
        temperatureRow.addView(temperatureDisplay, new LinearLayout.LayoutParams(dp(120), ViewGroup.LayoutParams.WRAP_CONTENT));

        Button temperatureUp = new Button(this);
        temperatureUp.setText("Temp +");
        temperatureUp.setOnClickListener(view -> changeTemperature(1));
        temperatureRow.addView(temperatureUp, weightedButtonParams());
        root.addView(temperatureRow, matchWrapParams());

        root.addView(sectionLabel("Mode"), matchWrapParams());
        modeSelector = horizontalRadioGroup();
        addModeButton(modeSelector, "Auto", RemoteState.Mode.AUTO);
        addModeButton(modeSelector, "Cool", RemoteState.Mode.COOL);
        addModeButton(modeSelector, "Dry", RemoteState.Mode.DRY);
        addModeButton(modeSelector, "Fan", RemoteState.Mode.FAN);
        modeSelector.setOnCheckedChangeListener((group, checkedId) -> {
            if (!updatingSelectors) {
                changeMode((RemoteState.Mode) group.findViewById(checkedId).getTag());
            }
        });
        root.addView(modeSelector, matchWrapParams());

        root.addView(sectionLabel("Fan"), matchWrapParams());
        fanSelector = horizontalRadioGroup();
        addFanButton(fanSelector, "Auto", RemoteState.FanSpeed.AUTO);
        addFanButton(fanSelector, "Low", RemoteState.FanSpeed.LOW);
        addFanButton(fanSelector, "Medium", RemoteState.FanSpeed.MEDIUM);
        addFanButton(fanSelector, "High", RemoteState.FanSpeed.HIGH);
        fanSelector.setOnCheckedChangeListener((group, checkedId) -> {
            if (!updatingSelectors) {
                changeFanSpeed((RemoteState.FanSpeed) group.findViewById(checkedId).getTag());
            }
        });
        root.addView(fanSelector, matchWrapParams());

        Button sendButton = new Button(this);
        sendButton.setText("Send / Apply");
        sendButton.setOnClickListener(view -> transmitStateCommand());
        root.addView(sendButton, matchWrapParams());

        LinearLayout toggleRow = horizontalRow();
        Button ledToggle = new Button(this);
        ledToggle.setText("LED Toggle");
        ledToggle.setOnClickListener(view -> transmitToggle(ToggleCommand.LED_TOGGLE, "LED"));
        toggleRow.addView(ledToggle, weightedButtonParams());

        Button energySaverToggle = new Button(this);
        energySaverToggle.setText("Energy Saver");
        energySaverToggle.setOnClickListener(view -> transmitToggle(ToggleCommand.ENERGY_SAVER_TOGGLE, "Energy Saver"));
        toggleRow.addView(energySaverToggle, weightedButtonParams());
        root.addView(toggleRow, matchWrapParams());

        commandPreview = new TextView(this);
        commandPreview.setGravity(Gravity.CENTER);
        commandPreview.setPadding(0, dp(16), 0, 0);
        root.addView(commandPreview, matchWrapParams());

        return root;
    }

    private void changeTemperature(int delta) {
        int nextTemperature = Math.max(RemoteState.MinTemperatureF, Math.min(RemoteState.MaxTemperatureF, remoteState.getTemperatureF() + delta));
        updateRemoteState(remoteState.getMode(), remoteState.getFanSpeed(), nextTemperature);
    }

    private void changeMode(RemoteState.Mode mode) {
        RemoteState.FanSpeed fanSpeed = remoteState.getFanSpeed();
        if (mode == RemoteState.Mode.AUTO || mode == RemoteState.Mode.DRY) {
            fanSpeed = RemoteState.FanSpeed.AUTO;
        }
        updateRemoteState(mode, fanSpeed, remoteState.getTemperatureF());
    }

    private void changeFanSpeed(RemoteState.FanSpeed fanSpeed) {
        RemoteState.Mode mode = remoteState.getMode();
        if ((mode == RemoteState.Mode.AUTO || mode == RemoteState.Mode.DRY) && fanSpeed != RemoteState.FanSpeed.AUTO) {
            mode = RemoteState.Mode.COOL;
            Toast.makeText(this, "Switched to Cool for selectable fan speeds.", Toast.LENGTH_SHORT).show();
        }
        updateRemoteState(mode, fanSpeed, remoteState.getTemperatureF());
    }

    private void updateRemoteState(RemoteState.Mode mode, RemoteState.FanSpeed fanSpeed, int temperatureF) {
        remoteState = new RemoteState(mode, fanSpeed, temperatureF);
        renderState();
    }

    private void renderState() {
        temperatureDisplay.setText(String.format(Locale.US, "%d°F", remoteState.getTemperatureF()));
        updatingSelectors = true;
        checkTaggedButton(modeSelector, remoteState.getMode());
        checkTaggedButton(fanSelector, remoteState.getFanSpeed());
        updatingSelectors = false;
        int[] commandBytes = remoteState.toCommandBytes();
        commandPreview.setText("Command bytes: " + formatBytes(commandBytes) + "\nRaw pulses: " + irEncoder.encodeCommand(commandBytes).length);
    }

    private void transmitStateCommand() {
        transmitCommand(remoteState.toCommandBytes(), "Remote state");
    }

    private void transmitToggle(ToggleCommand toggleCommand, String label) {
        transmitCommand(AmazonBasicsCommands.INSTANCE.toggleCommand(toggleCommand), label);
    }

    private void transmitCommand(int[] commandBytes, String label) {
        if (!irTransmitter.hasIrEmitter()) {
            Toast.makeText(this, "This device does not have an IR blaster, so it cannot send " + label + ".", Toast.LENGTH_LONG).show();
            return;
        }

        int[] rawPulses = irEncoder.encodeCommand(commandBytes);
        irTransmitter.transmit(rawPulses, MideaIrEncoder.CarrierFrequency);
        Toast.makeText(this, label + " sent: " + formatBytes(commandBytes), Toast.LENGTH_SHORT).show();
    }

    private void addModeButton(RadioGroup parent, String label, RemoteState.Mode mode) {
        addTaggedRadioButton(parent, label, mode);
    }

    private void addFanButton(RadioGroup parent, String label, RemoteState.FanSpeed fanSpeed) {
        addTaggedRadioButton(parent, label, fanSpeed);
    }

    private void addTaggedRadioButton(RadioGroup parent, String label, Object tag) {
        RadioButton button = new RadioButton(this);
        button.setText(label);
        button.setTag(tag);
        button.setId(View.generateViewId());
        parent.addView(button);
    }

    private void checkTaggedButton(RadioGroup group, Object tag) {
        for (int i = 0; i < group.getChildCount(); i++) {
            View child = group.getChildAt(i);
            if (tag.equals(child.getTag())) {
                group.check(child.getId());
                return;
            }
        }
    }

    private TextView sectionLabel(String label) {
        TextView view = new TextView(this);
        view.setText(label);
        view.setTextSize(18);
        view.setPadding(0, dp(16), 0, 0);
        return view;
    }

    private RadioGroup horizontalRadioGroup() {
        RadioGroup group = new RadioGroup(this);
        group.setOrientation(RadioGroup.HORIZONTAL);
        group.setGravity(Gravity.CENTER);
        return group;
    }

    private LinearLayout horizontalRow() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER);
        return row;
    }

    private LinearLayout.LayoutParams matchWrapParams() {
        return new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    private LinearLayout.LayoutParams weightedButtonParams() {
        return new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
    }

    private String formatBytes(int[] bytes) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < bytes.length; i++) {
            if (i > 0) {
                builder.append(' ');
            }
            builder.append(String.format(Locale.US, "%02X", bytes[i]));
        }
        return builder.toString();
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
