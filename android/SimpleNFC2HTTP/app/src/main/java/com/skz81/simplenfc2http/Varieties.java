package com.skz81.simplenfc2http;

import android.util.Base64;
import android.util.Log;
import android.widget.Toast;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
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
        private String image_base64; // TODO : have directly a Bitmap here
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

    private MainActivity activity;
    private List<Variety> varieties;
    private String server;
    private String imagesURLPrefix;

    public Varieties(MainActivity parent, String server,
                     String varietiesURL, String imagesURLPrefix) {
        this.activity = parent;
        this.varieties = new ArrayList<>();
        this.server = server;
        this.imagesURLPrefix = imagesURLPrefix;
        new SendToServerTask(this).GET(server + varietiesURL, null);
    }

    @Override
    public void onReplyFromServer(String data) {
        if (data == null) return; // filter error case (should not happend)

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
                fetchVarietyImage(variety, server + imagesURLPrefix + photoUrl);
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

    public List<Variety> getAll() {
        return varieties;
    }

    public Variety getById(int id) {
        for (Variety variety : varieties) {
            if (variety.id() == id) {
                return variety;
            }
        }
        return null; // Variety not found
    }

    public String getNameById(int id, String defaultVal) {
        Variety variety = getById(id);
        return variety == null ? defaultVal : variety.name;
    }

    public String getImageById(int id) {
        Variety variety = getById(id);
        // TODO: default image if not found ?
        return variety == null ? "" : variety.image_base64;
    }


    private void fetchVarietyImage(Variety variety, String image_url) {
        new SendToServerTask(new SendToServerTask.ReplyCB() {
            @Override
            public String decodeServerResponse(InputStream input) {
                ByteArrayOutputStream output = new ByteArrayOutputStream();
                try {
                    byte[] buffer = new byte[4096];
                    int n;

                    while ((n = input.read(buffer)) != -1)
                    {
                        output.write(buffer, 0, n);
                    }
                } catch(IOException e) {
                    Log.e(TAG, "Error fetching image for variety " +
                                variety.name + " (url: " + image_url +
                                "):" + e.getMessage());
                    return null;
                }
                return Base64.encodeToString(output.toByteArray(), Base64.DEFAULT);
            }
            @Override
            public void onReplyFromServer(String data) {
                Log.d(TAG, "Got BASE64 image: " + data.substring(0,63) + "...");
                variety.image_base64 = data;
            }
            @Override
            public void onError(String error) {
                Log.e(TAG, "Can't fetch variety image: " + error);
            }
        }).GET(image_url, null);
    }
}
