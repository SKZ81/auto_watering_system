package com.skz81.simplenfc2http;

import android.util.Log;
import java.util.Iterator;
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

public class SearchPlantId {
    private static final String TAG = "AutoWatS-NFC-searchPlantID";
    // TODO : manage HTTP request

    public interface Listener {
        public interface AttributeCallback {
            void execute(Object value);
        }
        boolean setAttribute(String path, String type, Object value);
    }

    // When a new SearchPlantId is instanciated, all listeners will be notified of the result.
    private static List<Listener> listeners = null;
    public static void addListener(Listener client) {
        if (listeners == null) {listeners = new ArrayList<>();}
        listeners.add(client);
    }
    public static void removeListener(Listener client) {
        if (listeners == null) {return;}
        listeners.remove(client);
    }

    public SearchPlantId(String uuid) {
        AppConfiguration config = AppConfiguration.instance();
        Map<String, String> params = new HashMap<>();
        params.put("uuid", uuid);
        SendToServerTask serverTask =new SendToServerTask(new SendToServerTask.ReplyCB() {
            @Override
            public void onRequestFailure(int errorCode) {
                if (errorCode == 404 /*&& mainActivity != null*/) {
                    Log.e(TAG, "Unknown Tag...");
                    // Toast.makeText(mainActivity, "Unknown Tag...", Toast.LENGTH_LONG).show();
                }
            }
            @Override
            public void onReplyFromServer(String data) {
                try {
                    JSONObject json = new JSONObject(data);
                    parseJSON("", json);
                }
                catch (JSONException e) {
                    onError("Error parsing JSON plant info: " + e.getMessage());
                }
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Error fetching data: " + error);
            }
        });
        serverTask.GET(config.getServerURL() + config.PLANT_SEARCH_ID, params);
    }

    public void parseJSON(String path, JSONObject jsonObject) throws JSONException {
        for(Iterator<String> it = jsonObject.keys(); it.hasNext(); ) {
            String key = it.next();
            Object value = jsonObject.get(key);
            String attrPath = path.isEmpty() ? key : path + '.' + key;
            String attrType = value.getClass().getSimpleName();

            if (value instanceof JSONObject) {
                parseJSON(attrPath, (JSONObject) value);
            } else if (value instanceof JSONArray) {
                parseJSONArray(attrPath, (JSONArray) value);
            } else {
                for (Listener listener : listeners) {
                    listener.setAttribute(attrPath, attrType, value);
                }
            }
        }
    }

    public void parseJSONArray(String path, JSONArray jsonArray) throws JSONException {
        for (int i = 0; i < jsonArray.length(); i++) {
            Object value = jsonArray.get(i);
            String attrPath = path + "[" + i + "]";
            String attrType = value.getClass().getSimpleName();

            if (value instanceof JSONObject) {
                parseJSON(attrPath, (JSONObject) value);
            } else if (value instanceof JSONArray) {
                parseJSONArray(attrPath, (JSONArray) value);
            } else {
                for (Listener listener : listeners) {
                    listener.setAttribute(attrPath, attrType, value);
                }
            }
        }
    }

    public static class BasicListener implements Listener {
        private Map<String, AttributeInfo> attributeMap;

        public BasicListener() {
            attributeMap = new HashMap<>();
        }

        public void addAttribute(String path, Class<?> expectedType, AttributeCallback callback) {
            attributeMap.put(path, new AttributeInfo(expectedType, callback));
        }

        @Override
        public boolean setAttribute(String path, String type, Object value) {
            AttributeInfo attributeInfo = attributeMap.get(path);
            if (attributeInfo != null && attributeInfo.expectedType.equals(type)) {
                attributeInfo.callback.execute(value);
                return true;
            } else {
                return false; // Skip the value if the path doesn't match or the types don't match
            }
        }

        private class AttributeInfo {
            private String expectedType;
            private AttributeCallback callback;

            public AttributeInfo(Class<?> expectedType, AttributeCallback callback) {
                this.expectedType = expectedType.getSimpleName();
                this.callback = callback;
            }
        }
    }
}


