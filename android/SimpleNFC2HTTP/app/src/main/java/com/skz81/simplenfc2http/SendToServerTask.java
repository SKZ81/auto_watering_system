package com.skz81.simplenfc2http;

import android.os.AsyncTask;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

class SendToServerTask extends AsyncTask<String, Void, String> {
    private static final String TAG = "AutoWatS.HTTP";
    private ReplyCB replyCallback = null;
    private boolean error = false;

    public interface ReplyCB {
        void onReplyFromServer(String data);
        void onError(String error);
    }


    // Constructor to pass the fragment instance
    public SendToServerTask(ReplyCB replyCallback) {
        this.replyCallback = replyCallback;
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

        execute("GET", url + queryString.toString());
    }

    public void POST(String url, Map<String, String> params) {
        String str_params = "";

        if (params != null) {
            JSONObject jsonParams = new JSONObject();
            try {
                for (Map.Entry<String, String> entry : params.entrySet()) {
                    jsonParams.put(entry.getKey(), entry.getValue());
                }
                str_params = jsonParams.toString();
            } catch (JSONException e) {
                if (replyCallback != null) {
                    replyCallback.onError(("error encoding JSON params: " + e.getMessage()));
                }
            }
        }

        execute("POST", url, str_params);
    }

    @Override
    protected String doInBackground(String... params) {
        // Check if correct number of parameters provided based on HTTP method
        if ((params[0].equals("GET") && params.length != 2) || (params[0].equals("POST") && params.length != 3)) {
            if (replyCallback != null) {
                replyCallback.onError("Invalid number of parameters provided.");
            }
            return null;
        }

        // Check if HTTP method is valid (either GET or POST)
        if (!params[0].equals("GET") && !params[0].equals("POST")) {
            if (replyCallback != null) {
                replyCallback.onError("Invalid HTTP method: " + params[0]);
            }
            return null;
        }

        HttpURLConnection urlConnection = null;
        BufferedReader reader = null;
        StringBuilder reply = new StringBuilder();

        try {
            URL url = new URL(params[1]);
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod(params[0]);

            // Set request headers for POST method
            if (params[0].equals("POST")) {
                urlConnection.setRequestProperty("Content-Type", "application/json");
            }

            // Set request body if provided for POST method
            if (params[0].equals("POST") && params[3] != null && !params[3].isEmpty()) {
                urlConnection.setDoOutput(true);
                urlConnection.setChunkedStreamingMode(0);
                OutputStream out = urlConnection.getOutputStream();
                out.write(params[3].getBytes());
                out.flush();
                out.close();
            }

            int responseCode = urlConnection.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                throw new IOException("Server response code: " + responseCode);
            }

            // Read the response
            reader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                reply.append(line).append("\n");
            }
        } catch (IOException e) {
            error = true;
            if (replyCallback != null) {
                replyCallback.onError("HTTP server communication error: " + e.getMessage());
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
                if (replyCallback != null) {
                    replyCallback.onError("Error while HTTP connection: " + e.getMessage());
                }
            }
        }
        return reply.toString();
    }


    @Override
    protected void onPostExecute(String result) {
        super.onPostExecute(result);
        if (!error && result != null) {
            // Pass the result back to the fragment
            replyCallback.onReplyFromServer(result);
        }
    }
}
