package com.skz81.simplenfc2http;

import android.content.SharedPreferences;
import android.content.Context;

public class AppConfiguration {

    public static final String VARIETIES_URL = "/varieties";
    public static final String VARIETIES_IMG_URL = "/images/varieties/";
    public static final String UPDATE_PLANT_URL = "/plant/update";
    public static final String PLANT_SEARCH_ID = "/plant/get";
    public static final String CREATE_TAG_URL = "/plant/create_id";
    public static final String DISCARD_UUID_URL = "/plant/discard_id";
    public static final String DEBUG_GET_UUIDS ="/debug/get_uuids";

    private static AppConfiguration instance = null;
    private static SharedPreferences sharedPreferences = null;

    private static final String PREF_NAME = "ServerSettingsPref";
    private static final String KEY_HTTPS = "isHttps";
    private static final String KEY_SERVER = "serverAddress";
    private static final String KEY_PORT = "port";
    private static final String KEY_PREFIX = "urlPrefix";

    private boolean isHttps;
    private String serverAddress;
    private int port;
    private String urlPrefix;

    private AppConfiguration() {
        isHttps = sharedPreferences.getBoolean(KEY_HTTPS, false);
        serverAddress = sharedPreferences.getString(KEY_SERVER, "www.example.com");
        port = sharedPreferences.getInt(KEY_PORT, 80);
        urlPrefix = sharedPreferences.getString(KEY_PREFIX, "");
    }

    public static void static_init(MainActivity main) {
        sharedPreferences = main.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public static AppConfiguration instance() {
        if (instance == null && sharedPreferences != null) {
            instance = new AppConfiguration();
        }
        return instance;
    }

    public void save() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(KEY_HTTPS, isHttps);
        editor.putString(KEY_SERVER, serverAddress);
        editor.putInt(KEY_PORT, port);
        editor.putString(KEY_PREFIX, urlPrefix);
        editor.apply();
    }

    public boolean isHttps() {
        return isHttps;
    }

    public void setHttps(boolean https) {
        isHttps = https;
    }

    public String serverAddress() {
        return serverAddress;
    }

    public void setServerAddress(String serverAddress) {
        this.serverAddress = serverAddress;
    }

    public int port() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String urlPrefix() {
        return this.urlPrefix;
    }

    public void setURLPrefix(String urlPrefix) {
        this.urlPrefix = urlPrefix;
    }

    public String getServerURL() {
        return (isHttps ? "https://" : "http://") +
                serverAddress +
                (!isHttps && port != 0 ? ":" + port : "") +
                (urlPrefix != "" ? "/"+urlPrefix : "");
    }
}
