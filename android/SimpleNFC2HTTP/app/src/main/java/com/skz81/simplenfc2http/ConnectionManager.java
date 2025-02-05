package com.skz81.simplenfc2http;

import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import android.util.Log;
import java.util.concurrent.Executors;
import org.json.JSONException;
import org.json.JSONObject;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

public class ConnectionManager {

    private final static String TAG = "AutoWatS-ConnectionManager";

    private final MainActivity mainActivity;
    private final LocalDatabase database;
    private final AppConfiguration config;
    private final JSONInfoAdapter timestampsAdapter;
    private final MutableLiveData<JSONObject> timestampsView;

    private Timer pingTimer;
    // table name => timestamp
    private Map<String, Long> timestampCache;

    public ConnectionManager(MainActivity mainActivity) {
        this.mainActivity = mainActivity;
        this.database = LocalDatabase.getInstance(mainActivity);
        this.config = AppConfiguration.instance();
        timestampsView = new MutableLiveData<>();
        timestampsAdapter = new JSONInfoAdapter(mainActivity, timestampsView);
        timestampsAdapter.addAttribute(
            "varieties", JSONInfoAdapter.AttributeAdapter.setterCleanerCheck (
                Integer.class,
                lastUpdate -> mainActivity.varieties().updateFromServer(Long.valueOf((Integer)lastUpdate)),
                () -> {},
                (path, lastUpdate) -> needUpdate("varieties", Long.valueOf((Integer)lastUpdate)
        )));
    }

    private boolean needUpdate(String table, long lastUpdate) {
        return lastUpdate > timestampCache.get(table);
    }

    public void requestTimestamps() {
        new SendToServerTask(new SendToServerTask.ReplyListener() {
            @Override
            public void onReplyFromServer(String data) {
                mainActivity.onServerConnectionOk();
                try {
                    timestampCache = new HashMap<String, Long>();
                    for (LocalDatabase.LastUpdate update :
                            database.lastUpdateDao().getAll()) {
                        timestampCache.put(update.getTableName(),
                                           update.getTimestamp());
                    }
                    JSONObject json = new JSONObject(data);
                    mainActivity.runOnUiThread(
                        () -> timestampsView.setValue(json)
                    );
                } catch (JSONException e) {
                    onError("Error while parsing timestamps JSON: " + e.getMessage());
                }
            }

            @Override
            public void onError(String data) {
                mainActivity.onServerConnectionError(data);
            }
        }).GET(config.getServerURL() + config.TIMESTAMPS_URL, null);
    }

    void startServerPing(int pingIntervalMs) {
        this.pingTimer = new Timer("server-ping");
        this.pingTimer.schedule(new TimerTask() {
            @Override
            public void run () {requestTimestamps();}
        }, 0, pingIntervalMs);
    }

    void stopServerPing() {
        this.pingTimer.cancel();
        this.pingTimer = null;
    }
}
