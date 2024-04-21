package com.skz81.simplenfc2http;

import android.util.Log;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.skz81.simplenfc2http.AppConfiguration;
import com.skz81.simplenfc2http.MainActivity;
import com.skz81.simplenfc2http.NdefTagCallback;
import com.skz81.simplenfc2http.SendToServerTask;
import com.skz81.simplenfc2http.JSONInfoAdapter;
import com.skz81.simplenfc2http.SharedJSONInfo;

public class SearchPlantId {
    private static final String TAG = "AutoWatS-NFC-searchPlantID";
    // TODO : manage HTTP request

    public static void newQuery(SharedJSONInfo sharedJSON, String uuid) {
        AppConfiguration config = AppConfiguration.instance();
        Map<String, String> params = new HashMap<>();
        params.put("uuid", uuid);

        new SendToServerTask(new SendToServerTask.ReplyCB() {
            @Override
            public void onRequestFailure(int errorCode) {
                if (errorCode == 404 /*&& mainActivity != null*/) {
                    Log.e(TAG, "Unknown Tag...");
                    sharedJSON.setInfo(null);
                    // Toast.makeText(mainActivity, "Unknown Tag...", Toast.LENGTH_LONG).show();
                }
            }
            @Override
            public void onReplyFromServer(String data) {
                try {
                    sharedJSON.setInfo(new JSONObject(data));
                }
                catch (JSONException e) {
                    onError("Error parsing JSON plant info: " + e.getMessage());
                }
            }
            @Override
            public void onError(String error) {
                Log.e(TAG, "Error fetching data: " + error);
                sharedJSON.setInfo(null);
            }
        }).GET(config.getServerURL() + config.PLANT_SEARCH_ID, params);
    }
}


