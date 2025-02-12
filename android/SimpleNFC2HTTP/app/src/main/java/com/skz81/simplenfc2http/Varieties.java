package com.skz81.simplenfc2http;

import android.content.Context;
import android.util.Base64;
import android.util.Log;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.sql.Timestamp;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.room.Entity;
import androidx.room.Dao;
import androidx.room.Database;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.PrimaryKey;
import androidx.room.Query;
// import androidx.room.Room;
// import androidx.room.RoomDatabase;

import com.skz81.simplenfc2http.AppConfiguration;
import com.skz81.simplenfc2http.MainActivity;
import com.skz81.simplenfc2http.SendToServerTask;


public class Varieties extends ViewModel
                       implements SendToServerTask.ReplyListener {

    private static final String TAG = "AutoWatS.Varieties";

    public interface UpdateListener {
        public void onVarietiesUpdated(Varieties updated);
    }

    // Define the Room Entity for Variety
    @Entity(tableName = "varieties")
    public static class Variety {
        @PrimaryKey
        private int id;
        private String name;
        private String shortDescription;
        private String photoUrl;
        private String imageBase64; // TODO : have directly byte[] here ??
        private int bloomingTimeDays;

        public Variety(int id, String name, String shortDescription, String photoUrl, int bloomingTimeDays) {
            this.id = id;
            this.name = name;
            this.shortDescription = shortDescription;
            this.photoUrl = photoUrl;
            this.bloomingTimeDays = bloomingTimeDays;
        }

        public int getId() {return id;}
        public String getName() {return name;}
        public String getShortDescription() {return shortDescription;}
        public String getPhotoUrl() {return photoUrl;}
        public String getImageBase64() {return imageBase64;}
        public int getBloomingTimeDays() {return bloomingTimeDays;}

        public void setImageBase64(String imageBase64) {
            this.imageBase64 = imageBase64;
        }
    }

    private MainActivity mainActivity;
    private LocalDatabase database;
    private LiveData<List<Variety>> view;
    private List<Variety> varieties;
    private AppConfiguration config;

    public Varieties(MainActivity mainActivity) {
        this.config = AppConfiguration.instance();
        this.mainActivity = mainActivity;
        this.database = LocalDatabase.getInstance(mainActivity);
        this.view = database.varietiesDao().getVarietiesLive();
    }

    public void updateFromServer(long lastUpdate) {
        new SendToServerTask(this).GET(config.getServerURL() + config.VARIETIES_URL, null);
    }

    @Override
    public void onReplyFromServer(String data) {
        if (data == null) return; // filter error case (should not happend)

        List<Variety> updatedVarieties = new ArrayList<>();
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
                updatedVarieties.add(variety);
                fetchVarietyImage(variety, config.getServerURL() + config.VARIETIES_IMG_URL + photoUrl);
            }
        } catch (JSONException e) {
            mainActivity.displayError(TAG, "Error parsing varieties JSON data: " + e.getMessage());
        }
        this.varieties = updatedVarieties;
        database.varietiesDao().insertVarieties(updatedVarieties);
        // database.varietiesDao().insertLastUpdate("varieties", )
    }

    @Override
    public void onError(String error) {
        mainActivity.displayError(TAG, "Can't fetch varieties: " + error);
        // notifySharedViewUpdate(null);
    }

    // ublic List<Variety> getAll() {
    //    return view.getValue();
    //}
    public LiveData<List<Variety>> getSharedView() {
        return view;
    }

    public Variety getById(int id) {
        for(Variety variety: this.varieties) {
            if (variety.id == id) {
                return variety;
            }
        }
        return null;
    }

    public String getNameById(int id, String defaultVal) {
        Variety variety = getById(id);
        return variety == null ? defaultVal : variety.name;
    }

    public String getImageById(int id) {
        Variety variety = getById(id);
        // TODO: default image if not found ?
        return variety == null ? "" : variety.imageBase64;
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
                variety.imageBase64 = data;
                database.varietiesDao().insertVariety(variety);
            }
            @Override
            public void onError(String error) {
                Log.e(TAG, "Can't fetch variety image: " + error);
            }
        }).GET(image_url, null);
    }

    // private MutableLiveData<Varieties> sharedVarieties = new MutableLiveData<>();
    // public LiveData<Varieties> sharedView() {
    //     return sharedVarieties;
    // }

    // public void observe(UpdateListener client) {
    //     sharedVarieties.observeForever(new Observer<Varieties>() {
    //         @Override
    //         public void onChanged(@Nullable Varieties updated) {
    //             Log.i(TAG, "NOTIFY update varieties to " + client);
    //             client.onVarietiesUpdated(updated);
    //         }
    //     });
    // }
    //
    // protected void notifySharedViewUpdate(Varieties updated) {
    //     Handler mainHandler = new Handler(Looper.getMainLooper());
    //     mainHandler.post(new Runnable() {
    //         @Override
    //         public void run() {
    //             sharedVarieties.setValue(updated);
    //         }
    //     });
    // }

    // Define the Room DAO for Variety
    @Dao
    public interface VarietiesDao {
        @Query("SELECT * FROM varieties")
        LiveData<List<Variety>> getVarietiesLive();

        @Query("SELECT * FROM varieties")
        List<Variety> getVarieties();

        @Query("SELECT * FROM varieties WHERE id = :varietyId")
        LiveData<Variety> getVarietyLiveById(int varietyId);

        @Query("SELECT * FROM varieties WHERE id = :varietyId")
        Variety getVarietyById(int varietyId);

        @Insert(onConflict = OnConflictStrategy.REPLACE)
        void insertVarieties(List<Variety> varieties);

        @Insert(onConflict = OnConflictStrategy.REPLACE)
        void insertVariety(Variety variety);
    }

}
