package com.skz81.simplenfc2http;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.LiveData;

import org.json.JSONObject;

// This is useful mostly when info is shared acrross fragment, even if not created yet
public class SharedJSONInfo extends ViewModel {
    private MutableLiveData<JSONObject> jsonInfo = new MutableLiveData<>();
    public SharedJSONInfo() {}

    public LiveData<JSONObject> getInfo() {
        return jsonInfo;
    }

    public void setInfo(JSONObject info) {
        jsonInfo.setValue(info);
    }
}
