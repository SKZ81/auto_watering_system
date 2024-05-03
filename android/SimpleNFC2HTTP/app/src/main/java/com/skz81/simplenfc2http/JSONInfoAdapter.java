package com.skz81.simplenfc2http;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Iterator;
import java.util.HashMap;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;

public class JSONInfoAdapter {
    private Map<Pattern, AttributeInfo> attributeMap = new HashMap<>();

    public JSONInfoAdapter(Fragment parent, LiveData<JSONObject> info) {
        info.observe(parent, new Observer<JSONObject>() {
            @Override
            public void onChanged(@Nullable JSONObject json) {
                clear();
                if (json != null) {
                    try {
                        parseJSON("", json);
                    } catch(JSONException e) {
                        Log.e("JSONInfoAdapter", "Error while extracting info from JSON: " + e.getMessage());
                    }
                }
            }
        });
    }

    public JSONInfoAdapter() {}

    public void addAttribute(String path, Class<?> expectedType,
                             AttributeInfo.SetterCallback set,
                             AttributeInfo.CleanerCallback clear) {
        attributeMap.put(Pattern.compile(Pattern.quote(path)), new AttributeInfo(expectedType, set, clear));
    }

    public void addAttributeRegex(String pathRegex, Class<?> expectedType,
                             AttributeInfo.SetterCallback set,
                             AttributeInfo.CleanerCallback clear) {
        attributeMap.put(Pattern.compile(pathRegex), new AttributeInfo(expectedType, set, clear));
    }

    public void clear() {
        attributeMap.forEach((k,v) -> v.cleaner.execute());
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
                setAttribute(attrPath, attrType, value);
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
                setAttribute(attrPath, attrType, value);
            }
        }
    }

    private void setAttribute(String path, String type, Object value) {
        attributeMap.forEach((pattern, attributeInfo) -> {
            if(pattern.matcher(path).find()) {
                if (attributeInfo != null && attributeInfo.expectedType.equals(type)) {
                    attributeInfo.setter.execute(value);
                }
            }
        });
        AttributeInfo attributeInfo = attributeMap.get(path);
    }


    private static class AttributeInfo {
        public interface SetterCallback {
            void execute(Object value);
        }
        public interface CleanerCallback {
            void execute();
        }
        private String expectedType;
        private SetterCallback setter;
        private CleanerCallback cleaner;

        public AttributeInfo(Class<?> expectedType, SetterCallback setter,
                                                    CleanerCallback cleaner) {
            this.expectedType = expectedType.getSimpleName();
            this.setter = setter;
            this.cleaner = cleaner;
        }
    }
}
