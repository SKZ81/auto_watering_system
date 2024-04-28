package com.skz81.simplenfc2http;

import android.util.Log;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.LiveData;

import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class FetchPlantInfo {

    private static final String TAG = "AutoWatS-NFC-fetchPlantInfo";
    private static final MutableLiveData<JSONObject> plantInfo = new MutableLiveData<>();

    public static LiveData<JSONObject> sharedJSONView() {
        return plantInfo;
    }

    public static void newQuery(String uuid, MainActivity mainActivity) {
        AppConfiguration config = AppConfiguration.instance();
        Map<String, String> params = new HashMap<>();
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
                    plantInfo.setValue(new JSONObject(data));
                }
                catch (JSONException e) {
                    onError("Error parsing JSON plant info: " + e.getMessage());
                }
            }
            @Override
            public void onError(String error) {
                mainActivity.toastDisplay(TAG, "Error fetching plant info: " + error, true);
                plantInfo.setValue(null);
            }
        }).GET(config.getServerURL() + config.PLANT_SEARCH_ID, params);
    }

}


