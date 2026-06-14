package com.example.amazonbasicsirremote;

import android.app.Activity;
import android.os.Bundle;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
    private IrTransmitter irTransmitter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        irTransmitter = new IrTransmitter(this);
        setContentView(createRemoteLayout());
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
        root.addView(title, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView subtitle = new TextView(this);
        subtitle.setText("Tap a button to send a sample IR command.");
        subtitle.setGravity(Gravity.CENTER);
        subtitle.setPadding(0, dp(8), 0, dp(24));
        root.addView(subtitle, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        GridLayout buttons = new GridLayout(this);
        buttons.setColumnCount(2);
        buttons.setUseDefaultMargins(true);
        root.addView(buttons, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        addRemoteButton(buttons, "Power", 0x10EF);
        addRemoteButton(buttons, "Mode", 0x20DF);
        addRemoteButton(buttons, "Temp +", 0x40BF);
        addRemoteButton(buttons, "Temp -", 0x807F);
        addRemoteButton(buttons, "Fan", 0xA05F);
        addRemoteButton(buttons, "Swing", 0x609F);

        return root;
    }

    private void addRemoteButton(GridLayout parent, String label, int command) {
        Button button = new Button(this);
        button.setText(label);
        button.setMinWidth(dp(132));
        button.setMinHeight(dp(56));
        button.setOnClickListener(view -> transmitSampleCommand(label, command));
        parent.addView(button);
    }

    private void transmitSampleCommand(String label, int command) {
        if (!irTransmitter.hasIrEmitter()) {
            Toast.makeText(this, "This device does not have an IR blaster, so it cannot send " + label + ".", Toast.LENGTH_LONG).show();
            return;
        }

        irTransmitter.transmit(buildNecPattern(0x00FF, command));
        Toast.makeText(this, label + " command sent", Toast.LENGTH_SHORT).show();
    }

    private int[] buildNecPattern(int address, int command) {
        int[] pattern = new int[67];
        pattern[0] = 9000;
        pattern[1] = 4500;
        int data = (address << 16) | command;
        for (int i = 0; i < 32; i++) {
            pattern[2 + (i * 2)] = 560;
            pattern[3 + (i * 2)] = ((data & (1 << i)) == 0) ? 560 : 1690;
        }
        pattern[66] = 560;
        return pattern;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
