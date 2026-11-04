package com.skz81.simplenfc2http;

import java.net.HttpCookie;
import java.net.URI;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import android.util.Log;


/* Encapsulate a nested ReplyListener and manages Authentication for it */
class AuthenticationManager implements SendToServerTask.ReplyListener {
    private static final String TAG = "AutoWatS.HTTP.AuthMgr";
    private static final String SESSION_COOKIE = "session_key";

    private final SendToServerTask.ReplyListener nestedListener;
    private final AppConfiguration config;

    private String targetUrl;
    private /*Map<String, */String targetParams;

    private boolean retry = false;

    public AuthenticationManager(SendToServerTask.ReplyListener nestedListener) {
        this.nestedListener = nestedListener;
        this.config = AppConfiguration.instance();
    }

    public void POST(String url, /*Map<String, */String params) {
        this.targetUrl = url;
        this.targetParams = params;

        if (hasSessionCookie()) {
            Log.d(TAG, "Cookie found, send main request.");
            retry = true;
            sendMainRequest();
        } else {
            Log.d(TAG, "Cookie not found, send login request.");
            login();
        }
    }

    private void sendMainRequest() {
        new SendToServerTask(this).POST(targetUrl, targetParams);
    }

    private void login() {
        Map<String, String> params = new HashMap<>();
        params.put("username", config.login());
        params.put("password", config.password());

        new SendToServerTask(new SendToServerTask.ReplyListener() {
            @Override
            public void onReplyFromServer(String data) {
                Log.i(TAG, "Authentication successful");
                sendMainRequest();
            }

            @Override
            public void onRequestFailure(int errorCode, String data) {
                // TODO: unsure if we should forward auth error as-is, here
                Log.e(TAG, "Login request failure: " + errorCode +", data:" + data);
                nestedListener.onRequestFailure(errorCode, data);
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Login error: " + error);
                nestedListener.onError("Server Authentication Error (user: " + config.login() + "), error:" + error);
            }
        }).POST(config.getServerURL() + AppConfiguration.LOGIN_URL, params);
    }

    @Override
    public void onReplyFromServer(String data) {
        nestedListener.onReplyFromServer(data);
    }

    @Override
    public void onRequestFailure(int errorCode, String data) {
        if (errorCode == 401 && retry) {
            retry = false;
            login();
        } else {
            nestedListener.onRequestFailure(errorCode, data);
        }
    }

    @Override
    public void onError(String data) {
        nestedListener.onError(data);
    }

    public boolean hasSessionCookie() {
        URI uri = URI.create(config.getServerURL());

        List<HttpCookie> cookies =
            config.getCookieManager().getCookieStore().get(uri);

        for (HttpCookie cookie : cookies) {
            if (SESSION_COOKIE.equals(cookie.getName())
                    && !cookie.hasExpired()) {
                return true;
            }
        }

        return false;
    }
}

