package com.skz81.simplenfc2http;

import android.util.Log;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.skz81.simplenfc2http.MainActivity;
import com.skz81.simplenfc2http.SendToServerTask;

public class Varieties implements SendToServerTask.ReplyCB {

    private static final String TAG = "AutoWatS.Varieties";

    public class Variety {
        private int id;
        private String name;
        private String shortDescription;
        private String photoUrl;
        private int bloomingTimeDays;

        public Variety(int id, String name, String shortDescription, String photoUrl, int bloomingTimeDays) {
            this.id = id;
            this.name = name;
            this.shortDescription = shortDescription;
            this.photoUrl = photoUrl;
            this.bloomingTimeDays = bloomingTimeDays;
        }

        public int id() {return id;}
        public String name() {return name;}
        public String shortDescription() {return shortDescription;}
        public String photoUrl() {return photoUrl;}
        public int bloomingTimeDays() {return bloomingTimeDays;}
    }

    private List<Variety> varieties = null;
    private MainActivity activity = null;

    public Varieties(MainActivity parent, String url) {
        this.activity = parent;
        this.varieties = new ArrayList<>();
        new SendToServerTask(this).GET("", url);
    }

    @Override
    public void onReplyFromServer(String data) {
        if (data == null) return; // error case

        try {
            Log.d(TAG, "Got JSON:\n" + data);
            JSONArray jsonArray = new JSONArray(data);
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject jsonObject = jsonArray.getJSONObject(i);
                int id = jsonObject.getInt("id");
                String name = jsonObject.getString("name");
                String shortDescription = jsonObject.getString("short_descrp");
                String photoUrl = jsonObject.getString("photo_url");
                int bloomingTimeDays = jsonObject.getInt("blooming_time_days");
                Variety variety = new Variety(id, name, shortDescription, photoUrl, bloomingTimeDays);
                varieties.add(variety);
            }
        } catch (JSONException e) {
            Log.e(TAG, "Error parsing JSON data: " + e.getMessage());
            varieties = null;
        }
    }

    @Override
    public void onError(String error) {
        Log.e(TAG, "Can't fetch varieties: " + error);
        if (activity != null) {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(activity,
                                "Cant fetch varieties:\n" + error,
                                Toast.LENGTH_LONG
                                ).show();
                }
            });
        }
    }

    public List<Variety> get() {
        return varieties;
    }
}