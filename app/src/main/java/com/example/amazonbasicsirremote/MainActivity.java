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
    private int temperatureF = 72;
    private AmazonBasicsCommands.Mode mode = AmazonBasicsCommands.Mode.COOL;
    private AmazonBasicsCommands.FanSpeed fanSpeed = AmazonBasicsCommands.FanSpeed.AUTO;
    private TextView status;
    private TextView tempLabel;
    private IrTransmitter irTransmitter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        irTransmitter = new IrTransmitter(this);
        setContentView(buildUi());
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

        tempLabel = new TextView(this);
        tempLabel.setTextSize(48);
        tempLabel.setGravity(Gravity.CENTER);
        root.addView(tempLabel, fullWidth());

        LinearLayout tempRow = new LinearLayout(this);
        tempRow.setGravity(Gravity.CENTER);
        Button down = button("− Temp");
        down.setOnClickListener(v -> { if (temperatureF > AmazonBasicsCommands.MIN_TEMP_F) temperatureF--; sendState(); });
        Button up = button("+ Temp");
        up.setOnClickListener(v -> { if (temperatureF < AmazonBasicsCommands.MAX_TEMP_F) temperatureF++; sendState(); });
        tempRow.addView(down);
        tempRow.addView(up);
        root.addView(tempRow, fullWidth());

        root.addView(label("Mode"), fullWidth());
        Spinner modeSpinner = new Spinner(this);
        modeSpinner.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, AmazonBasicsCommands.Mode.values()));
        modeSpinner.setSelection(mode.ordinal());
        modeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) { mode = AmazonBasicsCommands.Mode.values()[position]; updateStatus(); }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });
        root.addView(modeSpinner, fullWidth());

        root.addView(label("Fan speed"), fullWidth());
        Spinner fanSpinner = new Spinner(this);
        fanSpinner.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, AmazonBasicsCommands.FanSpeed.values()));
        fanSpinner.setSelection(fanSpeed.ordinal());
        fanSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) { fanSpeed = AmazonBasicsCommands.FanSpeed.values()[position]; updateStatus(); }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });
        root.addView(fanSpinner, fullWidth());

        Button send = button("Send current setting");
        send.setOnClickListener(v -> sendState());
        root.addView(send, fullWidth());

        Button led = button("LED Toggle");
        led.setOnClickListener(v -> sendCommand(AmazonBasicsCommands.ledToggle(), "LED toggle sent"));
        root.addView(led, fullWidth());

        Button energy = button("Energy Saver Toggle");
        energy.setOnClickListener(v -> sendCommand(AmazonBasicsCommands.energySaverToggle(), "Energy saver toggle sent"));
        root.addView(energy, fullWidth());

        return root;
    }

    private void sendState() {
        try {
            int[] command = AmazonBasicsCommands.forState(mode, fanSpeed, temperatureF);
            sendCommand(command, "Sent " + mode + " / " + fanSpeed + " / " + temperatureF + "°F");
        } catch (IllegalArgumentException ex) {
            Toast.makeText(this, ex.getMessage(), Toast.LENGTH_LONG).show();
        }
        updateStatus();
    }

    private void sendCommand(int[] command, String message) {
        try {
            irTransmitter.transmit(command);
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        } catch (IllegalStateException ex) {
            Toast.makeText(this, ex.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void updateStatus() {
        if (tempLabel != null) tempLabel.setText(temperatureF + "°F");
        if (status != null) status.setText(irTransmitter.hasEmitter() ? "IR blaster detected" : "No IR blaster detected");
    }

    private TextView label(String text) { TextView v = new TextView(this); v.setText(text); v.setTextSize(16); return v; }
    private Button button(String text) { Button b = new Button(this); b.setText(text); return b; }
    private LinearLayout.LayoutParams fullWidth() { return new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT); }
}
