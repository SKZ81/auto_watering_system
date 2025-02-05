package com.skz81.simplenfc2http;

import android.nfc.tech.Ndef;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LifecycleRegistry;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


public class FetchPlantInfo implements MainActivity.NdefTagListener {

    private static final String TAG = "AutoWatS-NFC-fetchPlantInfo";
    private static final MutableLiveData<JSONObject> plantInfo = new MutableLiveData<>();
    private MainActivity mainActivity;

    public static LiveData<JSONObject> sharedJSONView() {
        return plantInfo;
    }

    public FetchPlantInfo(MainActivity mainActivity) {
        this.mainActivity = mainActivity;
    }

    public static String getUUIDFromNdefMessage(NdefMessage message,
                                                String appName)
                                                throws IOException {
        NdefRecord[] records = message.getRecords();
        if (records == null || records.length < 2) {
            throw new IOException("Bad TAG format: no message or not enough records");
        }
        if (records[0].getType().equals("T") ||
            records[1].getPayload().equals(appName)) {
            throw new IOException("Bad TAG format: bad record type/content");
        }
        // TODO: utility fonction / class to extract UUID
        byte[] payload = records[0].getPayload();
        byte[] raw_uuid = new byte[payload.length - payload[0] - 1];
        System.arraycopy(payload, payload[0] + 1, raw_uuid, 0, raw_uuid.length);
        return new String(raw_uuid);
    }

    @Override
    public void onNDEFDiscovered(MainActivity.NdefAdapter ndef) throws IOException {
        AppConfiguration config = AppConfiguration.instance();
        Map<String, String> params = new HashMap<>();

        NdefMessage message = ndef.getNdefMessage();
        if (message == null) {
            throw new IOException("Tag is empty");
        }
        String uuid = getUUIDFromNdefMessage(message, mainActivity.appName());
        Log.i(TAG, "Read tag UUID: " + uuid);
        params.put("uuid", uuid);
        new SendToServerTask(new SendToServerTask.ReplyListener() {
            @Override
            public void onRequestFailure(int errorCode, String data) {
                if (errorCode == 404) {
                    onError("Unknown Tag...");
                }
                onError("Server error " + errorCode + ((data != null) ? ", " + data : ""));
            }
            @Override
            public void onReplyFromServer(String data) {
                try {
                    JSONObject json = new JSONObject(data);
                    mainActivity.runOnUiThread(
                        () -> plantInfo.setValue(json)
                    );
                }
                catch (JSONException e) {
                    onError("Error parsing JSON plant info: " + e.getMessage());
                }
            }
            @Override
            public void onError(String error) {
                mainActivity.toastDisplay(TAG, "Error fetching plant info: " + error, true);
                mainActivity.runOnUiThread( () -> plantInfo.setValue(null) );
            }
        }).GET(config.getServerURL() + config.PLANT_SEARCH_ID, params);
    }

    public static class PlantInfoLifecycleOwner implements LifecycleOwner {
        private LifecycleRegistry lifecycleRegistry;

        public PlantInfoLifecycleOwner() {
            lifecycleRegistry = new LifecycleRegistry(this);
            // Set the initial state to CREATED
            lifecycleRegistry.setCurrentState(Lifecycle.State.CREATED);
        }

        public void start() {
            // Update the state to STARTED
            lifecycleRegistry.setCurrentState(Lifecycle.State.STARTED);
        }

        public void stop() {
            // Update the state to STOPPED
            lifecycleRegistry.setCurrentState(Lifecycle.State.DESTROYED);
        }

        @NonNull
        @Override
        public Lifecycle getLifecycle() {
            return lifecycleRegistry;
        }
    }

}


