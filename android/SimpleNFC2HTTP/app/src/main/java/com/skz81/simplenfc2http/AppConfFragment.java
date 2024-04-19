package com.skz81.simplenfc2http;

import android.content.Context;
import android.os.Bundle;
import android.text.TextWatcher;
import android.text.Editable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class AppConfFragment extends Fragment
                              implements TextWatcher,
                                         RadioGroup.OnCheckedChangeListener {
    private static final String TAG = "AutoWatS-NFC-config";

    private EditText serverEditText;
    private EditText portEditText;
    private RadioGroup protocolRadioGroup;

    private AppConfiguration config;

    public AppConfFragment() {
        Log.d(TAG, "AppConfFragment ctor...");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_app_config, container, false);

        serverEditText = view.findViewById(R.id.serverEditText);
        portEditText = view.findViewById(R.id.portEditText);
        protocolRadioGroup = view.findViewById(R.id.protocolRadioGroup);

        config = AppConfiguration.instance();

        serverEditText.setText(config.serverAddress());
        portEditText.setText(String.valueOf(config.port()));
        if (config.isHttps()) {
            protocolRadioGroup.check(R.id.httpsRadioButton);
        } else {
            protocolRadioGroup.check(R.id.httpRadioButton);
        }

        // Register listeners to update config object as soon as UI field is modifed
        serverEditText.addTextChangedListener(this);
        portEditText.addTextChangedListener(this);
        protocolRadioGroup.setOnCheckedChangeListener(this);

        return view;
    }

    @Override
    public void onPause() {
        super.onPause();
        config.save();
    }

    @Override
    public void afterTextChanged(Editable s) {
        // Save all text edits into config
        config.setServerAddress(serverEditText.getText().toString());
        String port = portEditText.getText().toString();
        if (!port.isEmpty()) {
            config.setPort(Integer.parseInt(port));
        } else {
            config.setPort(0);
        }
    }

    @Override
    public void onCheckedChanged(RadioGroup group, int checkedId) {
        // Save all radio groups into config
        config.setHttps(checkedId == R.id.httpsRadioButton);
    }

    // Implementation needed, but not used.
    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {}
}
