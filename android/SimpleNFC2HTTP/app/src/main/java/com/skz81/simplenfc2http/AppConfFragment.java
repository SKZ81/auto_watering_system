package com.skz81.simplenfc2http;

import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

public class AppConfFragment extends Fragment
                              implements TextWatcher,
                                         RadioGroup.OnCheckedChangeListener {
    private static final String TAG = "AutoWatS-NFC-config";

    private EditText serverEditText;
    private EditText portEditText;
    private EditText prefixEditText;
    private EditText loginEditText;
    private EditText passwordEditText;
    private RadioGroup protocolRadioGroup;
    private Button connectButton;
    private MainActivity mainActivity;
    private AppConfiguration config;

    public AppConfFragment() {}

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        config = AppConfiguration.instance();
        mainActivity = (MainActivity) context;
        Log.d(TAG, "AppConfFragment onAttach... mainActivity=" + ((mainActivity!=null) ? mainActivity.toString() : "null"));
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_app_config, container, false);

        serverEditText = view.findViewById(R.id.serverEditText);
        portEditText = view.findViewById(R.id.portEditText);
        prefixEditText = view.findViewById(R.id.prefixEditText);
        loginEditText = view.findViewById(R.id.loginEditText);
        passwordEditText = view.findViewById(R.id.passwordEditText);
        protocolRadioGroup = view.findViewById(R.id.protocolRadioGroup);
        connectButton = view.findViewById(R.id.connectServerButton);
        config = AppConfiguration.instance();

        serverEditText.setText(config.serverAddress());
        if (config.isHttps()) {
            protocolRadioGroup.check(R.id.httpsRadioButton);
        } else {
            protocolRadioGroup.check(R.id.httpRadioButton);
        }
        setPortEnabled(config.isHttps());
        prefixEditText.setText(config.urlPrefix());

        loginEditText.setText(config.login());
        passwordEditText.setText(config.password());

        // Register listeners to update config object as soon as UI field is modifed
        serverEditText.addTextChangedListener(this);
        portEditText.addTextChangedListener(this);
        prefixEditText.addTextChangedListener(this);
        protocolRadioGroup.setOnCheckedChangeListener(this);
        loginEditText.addTextChangedListener(this);
        passwordEditText.addTextChangedListener(this);

        Log.d("CONFIG_DEBUG", "login: " +
            (view.findViewById(R.id.loginEditText) != null));
        Log.d("CONFIG_DEBUG", "password: " +
            (view.findViewById(R.id.passwordEditText) != null));
        loginEditText.post(() -> {
            Log.d("CONFIG_DEBUG", "login visibility: " +
                loginEditText.getVisibility());

            Log.d("CONFIG_DEBUG", "login dimensions: " +
                loginEditText.getWidth() + "x" +
                loginEditText.getHeight());
        });

        View parent = (View) loginEditText.getParent();
        Log.d("CONFIG_DEBUG", "parent dimensions: " +
            parent.getWidth() + "x" + parent.getHeight());
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
        config.setURLPrefix(prefixEditText.getText().toString());
        config.setLogin(loginEditText.getText().toString());
        config.setPassword(passwordEditText.getText().toString());
    }

    @Override
    public void onCheckedChanged(RadioGroup group, int checkedId) {
        // Save all radio groups into config
        boolean isHttps = (checkedId == R.id.httpsRadioButton);
        config.setHttps(isHttps);
        setPortEnabled(isHttps);
    }

    public void activateConnectButton() {
        connectButton.setEnabled(true);
        connectButton.setClickable(true);
        connectButton.setOnClickListener(v -> {
            mainActivity.connectServer();
            connectButton.setClickable(false);
            connectButton.setEnabled(false);
        });
    }

    // Implementation needed, but not used.
    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {}

    private void setPortEnabled(boolean enabled) {
        if (enabled)  {
            portEditText.setText("");      // Clear text
            portEditText.setEnabled(false); // Disable interaction
            portEditText.setFocusable(false);
            portEditText.setFocusableInTouchMode(false);
        } else {
            if (config.port() != 0)
                portEditText.setText(String.valueOf(config.port()));
            portEditText.setEnabled(true); // Enable interaction
            portEditText.setFocusable(true);
            portEditText.setFocusableInTouchMode(true);
        }
    }
}
