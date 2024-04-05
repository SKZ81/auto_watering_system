package com.skz81.simplenfc2http;

import android.os.AsyncTask;
import android.util.Log;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

class SendToServerTask extends AsyncTask<String, Void, String> {
    private static final String TAG = "AutoWatS.HTTP";
    private ReplyCB replyCallback = null;

    public interface ReplyCB {
        void onReplyFromServer(String data);
        void onError(String error);
    }


    // Constructor to pass the fragment instance
    public SendToServerTask(ReplyCB replyCallback) {
        this.replyCallback = replyCallback;
    }

    public void GET(String input, String url) {
        execute("GET", input, url);
    }

    public void POST(String input, String url) {
        execute("POST", input, url);
    }

    @Override
    protected String doInBackground(String... params) {
        Log.d(TAG, params[0] + " request to " + params[2].substring(params[2].indexOf(':') + 3));
        HttpURLConnection urlConnection = null;
        BufferedReader reader = null;
        StringBuilder reply = new StringBuilder();
        boolean error = false;

        try {
            URL url = new URL(params[2]);
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod(params[0]);

            if (!params[1].isEmpty()) {
                urlConnection.setDoOutput(true);
                urlConnection.setChunkedStreamingMode(0);
                OutputStream out = urlConnection.getOutputStream();
                out.write(params[1].getBytes());
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
            Log.e(TAG, "Error sending data to server: " + e.getMessage());
            if (replyCallback != null) {
                replyCallback.onError("HTTP server communication error");
            }
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
        }
        if (!error)
            return reply.toString();
        return null;
    }

    @Override
    protected void onPostExecute(String result) {
        super.onPostExecute(result);
        // Pass the result back to the fragment
        replyCallback.onReplyFromServer(result);
    }
}
