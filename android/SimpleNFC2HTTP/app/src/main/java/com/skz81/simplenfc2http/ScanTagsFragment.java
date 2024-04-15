package com.skz81.simplenfc2http;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.nfc.tech.Ndef;
import android.nfc.NdefRecord;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.skz81.simplenfc2http.AppConfiguration;
import com.skz81.simplenfc2http.MainActivity;
import com.skz81.simplenfc2http.NdefTagCallback;
import com.skz81.simplenfc2http.SendToServerTask;

public class ScanTagsFragment extends Fragment
                              implements NdefTagCallback {
    private static final String TAG = "AutoWatS-NFC-scan";

    private MainActivity mainActivity;
    private SendToServerTask serverTask = null;

    private ImageView varietyIcon;
    private TextView varietyName;
    private TextView plantId;
    private RadioGroup genderRadioGroup;
    private TextView germinationDate;
    private TextView bloomingDate;
    private TextView yieldingDate;

    public ScanTagsFragment() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_scan_tags, container, false);

        varietyIcon = view.findViewById(R.id.varietyIcon);
        varietyName = view.findViewById(R.id.varietyName);
        plantId = view.findViewById(R.id.plantId);
        genderRadioGroup = view.findViewById(R.id.genderRadioGroup);
        germinationDate = view.findViewById(R.id.germinationDate);
        bloomingDate = view.findViewById(R.id.bloomingDate);
        yieldingDate = view.findViewById(R.id.yieldingDate);

        // Set content for the labels
        varietyName.setText("Variety");
        plantId.setText("Tag ID : uuid");
        germinationDate.setText("Germination : date");
        bloomingDate.setText("Blooming : date");
        yieldingDate.setText("Yielding : date");
        // varietyIcon.setImageResource(R.drawable.your_image); // Replace 'your_image' with your image resource

        mainActivity = (MainActivity) getActivity();
        if (mainActivity != null) {
            mainActivity.startNFCScan(this);
        }

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mainActivity != null) {
            mainActivity.startNFCScan(this);
        }
    }

    @Override
    public void onPause() {
        if (mainActivity != null) {
            mainActivity.stopNFCScan();
        }
        super.onPause();
    }

    @Override
    public void onDestroyView() {
        if (mainActivity != null) {
            mainActivity.stopNFCScan();
        }
        if (serverTask != null) {
            serverTask.cancel(true);
        }
        super.onDestroyView();
    }

    @Override
    public void onNDEFDiscovered(Ndef ndef) {
        NdefMessage message = ndef.getCachedNdefMessage();

        try {
            ndef.close();
        } catch (Exception e) {
            Log.w(TAG, "Error closing NDEF connection: " + e.getMessage(), e);
        }


        try {
            AppConfiguration config = AppConfiguration.instance();
            Map<String, String> params = new HashMap<>();
            String uuid = null;

            NdefRecord[] records = message.getRecords();
            if (records == null || records.length < 2) {
                throw new IOException("Bad TAG format: no message or not enough records");
            }
            if (records[0].getType().equals("T") ||
                records[1].getPayload().equals(mainActivity.appName())) {
                throw new IOException("Bad TAG format: bad record type/content");
            }
            if (serverTask != null) {
                serverTask.cancel(true);
            }
            uuid = records[0].getPayload().toString();
            params.put("uuid", uuid);
            serverTask = new SendToServerTask(new SendToServerTask.ReplyCB() {
                @Override
                public void onRequestFailure(int errorCode) {
                    if (errorCode == 404 && mainActivity != null) {
                        Log.e(TAG, "Unknown Tag...");
                        // Toast.makeText(mainActivity, "Unknown Tag...", Toast.LENGTH_LONG).show();
                    }
                }
                @Override
                public void onReplyFromServer(String data) {
                    setTabFieldsFromJSON(data);
                }
                @Override
                public void onError(String error) {
                    Log.e(TAG, "Error fetching data: " + error);
                }
            });
            serverTask.GET(config.getServerURL() + config.PLANT_SEARCH_ID, params);
        } catch (IOException e) {
            Log.e(TAG, "Error scanning tag:" + e.getMessage());
        }
    }

    private void setTabFieldsFromJSON(String json) {
        if (json == null) return; // filter error case (should not happend)
        try {
            Log.d(TAG, "Got JSON:\n" + json);
            JSONArray jsonArray = new JSONArray(json);
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject jsonObject = jsonArray.getJSONObject(i);
                int variety_id = jsonObject.getInt("variety");
                varietyName.setText(mainActivity.varieties().getNameById(variety_id, "not available"));
                plantId.setText(jsonObject.getString("UUID"));
                germinationDate.setText(jsonObject.getString("germination_date"));
                bloomingDate.setText(jsonObject.getString("blooming_date"));
                yieldingDate.setText(jsonObject.getString("yielding_date"));
                new LoadImageTask(varietyIcon).execute(
                    mainActivity.varieties().getImageById(variety_id));
            }
        } catch (JSONException e) {
            Log.e(TAG, "Error parsing JSON data: " + e.getMessage());
        }
    }

    private class LoadImageTask extends AsyncTask<String, Void, Bitmap> {
        private ImageView view = null;

        public LoadImageTask(ImageView view) {
            this.view = view;
        }

        @Override
        protected Bitmap doInBackground(String... params) {
            byte[] decodedBytes = Base64.decode(params[0], Base64.DEFAULT);
            InputStream inputStream = new ByteArrayInputStream(decodedBytes);
            return BitmapFactory.decodeStream(inputStream);
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            super.onPostExecute(bitmap);
            if (bitmap != null && view != null) {
                view.setImageBitmap(bitmap);
            }
        }
    }
}
