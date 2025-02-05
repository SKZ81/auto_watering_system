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
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.Observer;

public class JSONInfoAdapter {
    private Map<Pattern, AttributeAdapter> attributeMap = new HashMap<>();

    public JSONInfoAdapter(LifecycleOwner parent, LiveData<JSONObject> info) {
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

    public void addAttribute(String path, AttributeAdapter attribute) {
        attributeMap.put(Pattern.compile(Pattern.quote(path)), attribute);
    }

    public void addRegexAttribute(String pathRegex, AttributeAdapter attribute) {
        attributeMap.put(Pattern.compile(pathRegex), attribute);
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
        // TODO: no need to have a Map<> under the hood if we only iterate
        attributeMap.forEach((pattern, attributeInfo) -> {
            boolean result = pattern.matcher(path).matches();
            if(result) {
                boolean checkPassed = attributeInfo.checker.execute(path, value);
                if (attributeInfo != null &&
                    attributeInfo.expectedType.equals(type) &&
                    checkPassed) {
                    attributeInfo.setter.execute(value);
                }
            }
        });
    }


    public static class AttributeAdapter {
        public interface Checker {
            boolean execute(String path, Object value);
        }
        public interface Setter {
            void execute(Object value);
        }
        public interface Cleaner {
            void execute();
        }
        private Setter setter;
        private Cleaner cleaner;
        private Checker checker;
        private String expectedType;

        private AttributeAdapter(Class<?> expectedType,
                                 Setter setter,
                                 Cleaner cleaner,
                                 Checker checker) {
            this.expectedType = expectedType.getSimpleName();
            this.setter = setter;
            this.cleaner = cleaner;
            this.checker = checker;
        }

        public static AttributeAdapter setterOnly(
                                        Class<?> expectedType,
                                        Setter setter) {
            return new AttributeAdapter(expectedType, setter, () -> {}, (p, v) -> true);
        }
        public static AttributeAdapter setterCleaner(
                                        Class<?> expectedType,
                                        Setter setter,
                                        Cleaner cleaner) {
            return new AttributeAdapter(expectedType, setter, cleaner, (p, v) -> true);
        }
        public static AttributeAdapter setterCleanerCheck(
                                        Class<?> expectedType,
                                        Setter setter,
                                        Cleaner cleaner,
                                        Checker checker) {
            return new AttributeAdapter(expectedType, setter, cleaner, checker);
        }
    }
}
