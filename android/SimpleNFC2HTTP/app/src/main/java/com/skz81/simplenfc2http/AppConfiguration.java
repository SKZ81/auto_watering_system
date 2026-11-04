package com.skz81.simplenfc2http;

import android.content.SharedPreferences;
import android.content.Context;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;

public class AppConfiguration {

    public static final String LOGIN_URL = "/login";
    public static final String LOGOUT_URL = "/logout";

    public static final String TIMESTAMPS_URL = "/timestamps";
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
    private static final String KEY_LOGIN = "login";
    private static final String KEY_PASSWORD = "password";


    private boolean isHttps;
    private String serverAddress;
    private int port;
    private String urlPrefix;
    private String login;
    private String password;
    private CookieManager cookieManager;

    private AppConfiguration() {
        isHttps = sharedPreferences.getBoolean(KEY_HTTPS, false);
        serverAddress = sharedPreferences.getString(KEY_SERVER, "www.example.com");
        port = sharedPreferences.getInt(KEY_PORT, 80);
        urlPrefix = sharedPreferences.getString(KEY_PREFIX, "");
        login = sharedPreferences.getString(KEY_LOGIN, "");
        password = sharedPreferences.getString(KEY_PASSWORD, "");
        cookieManager = new CookieManager(null, CookiePolicy.ACCEPT_ORIGINAL_SERVER);
        CookieHandler.setDefault(cookieManager);
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
        editor.putString(KEY_LOGIN, login);
        editor.putString(KEY_PASSWORD, password);
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

    public String login() {
        return this.login;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    public String password() {
        return this.password;
    }

    public void setPassword(String password) {
        this.password = password;
    }


    public String getServerURL() {
        return (isHttps ? "https://" : "http://") +
                serverAddress +
                (!isHttps && port != 0 ? ":" + port : "") +
                (urlPrefix != "" ? "/"+urlPrefix : "");
    }

    public CookieManager getCookieManager() {
        return cookieManager;
    }
}
