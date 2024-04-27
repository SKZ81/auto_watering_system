package com.skz81.simplenfc2http;

import android.os.Handler;
import android.os.Looper;
import androidx.annotation.NonNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.json.JSONException;
import org.json.JSONObject;

public class SendToServerTask {
    private static final String TAG = "AutoWatS.HTTP";

    private ReplyListener replyListener;
    private boolean error = false;
    private boolean cancelled = false;
    private boolean done = false;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private ConnectionStateWatcher connectionWatcher = null;

    public interface ReplyListener {
        default String decodeServerResponse(InputStream input) {
            StringBuilder reply = new StringBuilder();
            try {
                BufferedReader reader =
                        new BufferedReader(new InputStreamReader(input));
                String line;
                while ((line = reader.readLine()) != null) {
                    reply.append(line).append("\n");
                }
            } catch (IOException e) {
                onError(e.getMessage());
                return null;
            }
            return reply.toString();
        }


        void onReplyFromServer(String data);

        default void onRequestFailure(int errorCode, String data) {
            onError("Received (unhandled) error " + errorCode + " from server" +
                       ((data != null) ? ", " + data : ""));
        }
        void onError(String data);
    }

    public interface ConnectionStateWatcher {
        public void onServerConnectionOk();
        public void onServerConnectionError(String error);
    }

    public void setConnectionWatcher(ConnectionStateWatcher watcher) {
        connectionWatcher = watcher;
    }

    public SendToServerTask(@NonNull ReplyListener replyListener) {
        this.replyListener = replyListener;
    }

    public boolean isCancelled() {
        return cancelled;
    }

    public boolean cancel() {
        if (done || cancelled) {
            return false;
        }
        return cancelled;
    }

    public void GET(String url, Map<String, String> params) {
        StringBuilder queryString = new StringBuilder();

        if (params != null) {
            for (Map.Entry<String, String> entry : params.entrySet()) {
                if (queryString.length() == 0) {
                    queryString.append("?");
                } else {
                    queryString.append("&");
                }
                queryString.append(entry.getKey()).append("=").append(entry.getValue());
            }
        }

        execute("GET", url, queryString.toString());
    }

    public void POST(final String url, final String param) {
        execute("POST", url, param);
    }

    public void POST(final String url, final Map<String, String> params) {
        String strParams = "";

        if (params != null) {
            JSONObject jsonParams = new JSONObject();
            try {
                for (Map.Entry<String, String> entry : params.entrySet()) {
                    jsonParams.put(entry.getKey(), entry.getValue());
                }
                strParams = jsonParams.toString();
            } catch (JSONException e) {
                if (replyListener != null) {
                    replyListener.onError("error encoding JSON params: " + e.getMessage());
                }
            }
        }
        POST(url, strParams);
    }

    public void execute(final String method, final String url, final String param) {
        if (cancelled) {
            replyListener.onError("Sever task cancelled.");
            return;
        }
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<String> future = executor.submit(new Callable<String>() {
            @Override
            public String call() throws Exception {
                return doInBackground(method, url, param);
            }
        });

        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    handler.post(new Runnable() {
                    final String result = (!cancelled && !error) ? future.get() : null;
                        @Override
                        public void run() {
                            done = true;
                            if (!cancelled && !error) {
                                replyListener.onReplyFromServer(result);
                                if (connectionWatcher != null) {
                                    connectionWatcher.onServerConnectionOk();
                                }
                            } else if (cancelled) {
                                replyListener.onError("Sever task cancelled.");
                            } // else onError() should be called directly in error cases
                        }
                    });
                } catch (Exception e) {
                    error = true;
                    done = true;

                    StringBuilder message = new StringBuilder();
                    message.append("Server task error: ");
                    message.append(e.getMessage());
                    StackTraceElement[] stackTrace = e.getStackTrace();
                    for(StackTraceElement elem : stackTrace) {
                        message.append("\n");
                        message.append(elem.toString());
                    }
                    replyListener.onError(message.toString());
                }
            }
        });
    }

    protected String doInBackground(String... params) {
        // Check if correct number of parameters provided based on HTTP method
        if ((params[0].equals("GET") && params.length != 3) ||
            (params[0].equals("POST") && params.length != 3)) {
            replyListener.onError("Invalid number of parameters provided.");
            error = true;
            return null;
        }

        // Check if HTTP method is valid (either GET or POST)
        if (!params[0].equals("GET") && !params[0].equals("POST")) {
            replyListener.onError("Invalid HTTP method: " + params[0]);
            error = true;
            return null;
        }

        HttpURLConnection urlConnection = null;
        BufferedReader reader = null;
        String reply;

        try {
            URL url;
            if (params[0].equals("GET")) {
                url = new URL(params[1] + params[2]);
            } else {
                url = new URL(params[1]);
            }
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod(params[0]);
            urlConnection.setConnectTimeout(5000);
            // Set request headers for POST method
            if (params[0].equals("POST")) {
                urlConnection.setRequestProperty("Content-Type", "application/json");
            }

            // Set request body if provided for POST method
            if (params[0].equals("POST") && params[2] != null && !params[2].isEmpty()) {
                urlConnection.setDoOutput(true);
                urlConnection.setChunkedStreamingMode(0);
                OutputStream out = urlConnection.getOutputStream();
                out.write(params[2].getBytes());
                out.flush();
                out.close();
            }

            int responseCode = urlConnection.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                InputStream errorStream = urlConnection.getErrorStream();
                String errorData = "";
                if (errorStream != null) {
                    errorData = replyListener.decodeServerResponse(errorStream);
                }
                replyListener.onRequestFailure(responseCode, errorData);
                return null;
            }
            reply = replyListener.decodeServerResponse(urlConnection.getInputStream());

        } catch (IOException e) {
            error = true;
            replyListener.onError("HTTP server communication error: " + e.getMessage());
            if (connectionWatcher != null) {
                connectionWatcher.onServerConnectionError("HTTP server communication error: " + e.getMessage());
            }
            return null; // return null on error
        } finally {
            try {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
                if (reader != null) {
                    reader.close();
                }
            } catch (Exception e) {
                replyListener.onError("Error while disconnectong from server: " + e.getMessage());

                if (connectionWatcher != null) {
                    connectionWatcher.onServerConnectionError("Error while disconnectong from server: " + e.getMessage());
                }
            }
        }
        return reply;
    }

}
