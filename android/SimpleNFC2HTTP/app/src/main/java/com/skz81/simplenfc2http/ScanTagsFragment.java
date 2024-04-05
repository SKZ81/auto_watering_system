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
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.nfc.tech.Ndef;
import android.nfc.NdefRecord;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;

import java.util.List;
import java.util.ArrayList;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import com.skz81.simplenfc2http.MainActivity;
import com.skz81.simplenfc2http.NdefTagCallback;

public class ScanTagsFragment extends Fragment
                              implements NdefTagCallback,
                                         SendToServerTask.ReplyCB {
    private static final String TAG = "AutoWatS-NFC-scan";

    private MainActivity mainActivity;

    private ImageView varietyIcon;
    private TextView varietyName;
    private TextView plantId;
    private TextView germinationDate;
    private TextView bloomingDate;
    private TextView yieldingDate;

    public ScanTagsFragment() {
        // Required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_scan_tags, container, false);

        varietyIcon = view.findViewById(R.id.varietyIcon);
        varietyName = view.findViewById(R.id.varietyName);
        plantId = view.findViewById(R.id.plantId);
        germinationDate = view.findViewById(R.id.germinationDate);
        bloomingDate = view.findViewById(R.id.bloomingDate);
        yieldingDate = view.findViewById(R.id.yieldingDate);

        // Set content for the labels
        varietyName.setText("Variety");
        plantId.setText("Tag ID : uuid");
        germinationDate.setText("Germination : date");
        bloomingDate.setText("Blooming : date");
        yieldingDate.setText("Yielding : date");

        mainActivity = (MainActivity) getActivity();
        if (mainActivity != null) {
            mainActivity.startNFCScan(this);
        }

        // Set image resource
        // varietyIcon.setImageResource(R.drawable.your_image); // Replace 'your_image' with your image resource

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
        super.onDestroyView();
    }

    @Override
    public void onNDEFDiscovered(Ndef ndef) {
        List<NdefMessage> messages = new ArrayList<>();
        try {
            ndef.connect();
            if (ndef.isConnected()) {
                // Read all NDEF messages
                NdefMessage message;
                while ((message = ndef.getNdefMessage()) != null) {
                    messages.add(message);
                }
            }
            // check we're still connected
            if (!ndef.isConnected()) {
                throw new IOException("NFC connection lost while reading tag");
            }
        } catch (Exception e) {
            // Log error message
            Log.e(TAG, "Error reading NDEF messages: " + e.getMessage(), e);
            messages.clear();
            Toast.makeText(mainActivity, "Error reading tag, please retry", Toast.LENGTH_LONG).show();
        } finally {
            try {
                ndef.close();
            } catch (Exception e) {
                Log.w(TAG, "Error closing NDEF connection: " + e.getMessage(), e);
            }
        }
        for (int i = 0; i < messages.size(); i++) {
            NdefMessage message = messages.get(i);
            Log.i(TAG, "Message " + (i + 1) + ":");

            // Iterate through each NDEF record within the message
            NdefRecord[] records = message.getRecords();
            for (int j = 0; j < records.length; j++) {
                NdefRecord record = records[j];
                Log.i(TAG, "  Record " + (j + 1) + ":");
                Log.i(TAG, "    TNF (Type Name Format): " + record.getTnf());
                Log.i(TAG, "    Type: " + new String(record.getType()));
                Log.i(TAG, "    Payload: " + new String(record.getPayload()));
            }
        }
    }

    private void sendToServer(String url, String data) {
        new SendToServerTask(this).POST(url, data);
    }

    @Override
    public void onReplyFromServer(String data) {
        Log.i(TAG, "Got answer from server:\n" + data);
    }

    @Override
    public void onError(String error) {
        Toast.makeText(mainActivity, "Can't get TAG data from server:\n" + error,
                       Toast.LENGTH_LONG).show();
    }

    private class LoadImageTask extends AsyncTask<String, Void, Bitmap> {
        private ImageView view = null;

        public LoadImageTask(ImageView view) {
            this.view = view;
        }

        @Override
        protected Bitmap doInBackground(String... params) {
            // Decode base64 string to byte array
            byte[] decodedBytes = Base64.decode(params[0], Base64.DEFAULT);
            // Convert byte array to Bitmap
            InputStream inputStream = new ByteArrayInputStream(decodedBytes);
            return BitmapFactory.decodeStream(inputStream);
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            super.onPostExecute(bitmap);
            // Set the bitmap to the ImageView
            if (bitmap != null && view != null) {
                view.setImageBitmap(bitmap);
            }
        }
    }

    private class LoadImageTask extends AsyncTask<String, Void, Bitmap> {
        private ImageView view = null;

        public LoadImageTask(ImageView view) {
            this.view = view
        }
        @Override
        protected Bitmap doInBackground(String... params) {
            // Decode base64 string to byte array
            byte[] decodedBytes = Base64.decode(params[0], Base64.DEFAULT);
            // Convert byte array to Bitmap
            InputStream inputStream = new ByteArrayInputStream(decodedBytes);
            return BitmapFactory.decodeStream(inputStream);
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            super.onPostExecute(bitmap);
            // Set the bitmap to the ImageView
            if (bitmap != null && view != null) {
                view.setImageBitmap(bitmap);
            }
        }
    }
}
