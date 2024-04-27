package com.skz81.simplenfc2http;

import android.util.Base64;
import android.util.Log;
import android.os.Handler;
import android.os.Looper;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;

import com.skz81.simplenfc2http.AppConfiguration;
import com.skz81.simplenfc2http.MainActivity;
import com.skz81.simplenfc2http.SendToServerTask;


public class Varieties extends ViewModel
                       implements SendToServerTask.ReplyListener {

    private static final String TAG = "AutoWatS.Varieties";

    public interface UpdateListener {
        public void onVarietiesUpdated(Varieties updated);
    }

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
        public String imageBase64() {return image_base64;}
        public int bloomingTimeDays() {return bloomingTimeDays;}
    }

    private static Varieties instance;
    public static Varieties instance() { return instance; }
    public static Varieties instance(MainActivity mainActivity) {
        if (instance == null) {
            AppConfiguration config = AppConfiguration.instance();
            instance = new Varieties(mainActivity,
                                      config.getServerURL(),
                                      config.VARIETIES_URL,
                                      config.VARIETIES_IMG_URL);
        } else {
            // simulate cnx ok
            //TBD : we should check the timestamp each time instead !
            // Also, should separate "varieties update" and "serverConnectionOK" signals
            mainActivity.onServerConnectionOk();
        }
        return instance;
    }

    private MainActivity mainActivity;
    private List<Variety> varieties;
    private String server;
    private String imagesURLPrefix;

    private Varieties(MainActivity parent, String server,
                     String varietiesURL, String imagesURLPrefix) {
        this.mainActivity = parent;
        this.varieties = new ArrayList<>();
        this.server = server;
        this.imagesURLPrefix = imagesURLPrefix;
        SendToServerTask serverTask = new SendToServerTask(this);
        serverTask.setConnectionWatcher(mainActivity);
        serverTask.GET(server + varietiesURL, null);
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
            // notify "Update Tab" Spinner (and any other client) of varieties update
            notifySharedViewUpdate(this);
        } catch (JSONException e) {
            mainActivity.displayError(TAG, "Error parsing varieties JSON data: " + e.getMessage());
            notifySharedViewUpdate(null);
        }
    }

    @Override
    public void onError(String error) {
        mainActivity.displayError(TAG, "Can't fetch varieties: " + error);
        notifySharedViewUpdate(null);
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
        new SendToServerTask(new SendToServerTask.ReplyListener() {
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

    private MutableLiveData<Varieties> sharedVarieties = new MutableLiveData<>();

    public LiveData<Varieties> sharedView() {
        return sharedVarieties;
    }

    public void observe(UpdateListener client) {
        sharedVarieties.observeForever(new Observer<Varieties>() {
            @Override
            public void onChanged(@Nullable Varieties varieties) {
                Log.i(TAG, "NOTIFY update varieties to " + client);
                client.onVarietiesUpdated(varieties);
            }
        });
    }

    protected void notifySharedViewUpdate(Varieties updated) {
        Handler mainHandler = new Handler(Looper.getMainLooper());
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                sharedVarieties.setValue(updated);
            }
        });
    }
}
