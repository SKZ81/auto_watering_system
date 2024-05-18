package com.skz81.simplenfc2http;

import java.util.HashMap;
import java.util.Map;
import java.sql.Timestamp;
// import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class ConnectionManager {

    private final static String TAG = "AutoWatS-ConnectionManager";

    private MainActivity mainActivity;
    private LocalDatabase database;
    private AppConfiguration config;

    public ConnectionManager(MainActivity mainActivity) {
        this.mainActivity = mainActivity;
        this.database = LocalDatabase.getInstance(mainActivity);
        this.config = AppConfiguration.instance();
    }

    public interface UpdateMethod {
        public void execute(Timestamp lastUpdate);
    }

    private static final Map<String, UpdateMethod> updateMethods = new HashMap<>();
    public static void addUpdateMethod(String tableName, UpdateMethod method) {
        updateMethods.put(tableName, method);
    }

    public void checkTimestamps() {
        new SendToServerTask(new SendToServerTask.ReplyListener() {
            @Override
            public void onReplyFromServer(String data) {
                mainActivity.onServerConnectionOk();
                try {
                    JSONObject json = new JSONObject(data);
                    for(String tableName : database.lastUpdateDao().getTableNames().getValue()) {
                        Timestamp timestamp = new Timestamp(json.getLong(tableName));
                        Timestamp local = new Timestamp( database.lastUpdateDao().getLastUpdateByDataName(tableName).getValue().getTimestamp());
                        if (timestamp.after(local)) {
                            UpdateMethod method = updateMethods.get(tableName);
                            if (method != null) {
                                method.execute(timestamp);
                            }
                        }
                    }
                } catch (JSONException e) {
                    onError("Error while parsing timestamps JSON: " + e.getMessage());
                }
            }
            // default void onRequestFailure(int errorCode, String data) {
            //     onError("Received (unhandled) error " + errorCode + " from server" +
            //             ((data != null) ? ", " + data : ""));
            // }
            @Override
            public void onError(String data) {
                mainActivity.onServerConnectionError(data);
            }
        }).GET(config.getServerURL() + config.TIMESTAMPS_URL, null);
    }

}
