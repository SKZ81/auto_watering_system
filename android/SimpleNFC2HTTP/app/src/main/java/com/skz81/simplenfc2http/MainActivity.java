package com.skz81.simplenfc2http;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.nfc.tech.Ndef;
import android.nfc.NdefRecord;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;

import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Parcelable;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.FragmentActivity;
import androidx.viewpager.widget.ViewPager;
import com.google.android.material.tabs.TabLayout;

import java.util.List;
import java.util.ArrayList;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import com.skz81.simplenfc2http.R;
import com.skz81.simplenfc2http.ScanTagsFragment;
import com.skz81.simplenfc2http.WriteTagFragment;
import com.skz81.simplenfc2http.TabFragment;

public class MainActivity extends FragmentActivity implements NfcAdapter.ReaderCallback {

    private static final String TAG = "NFC Demo";
    private static final String SERVER_URL = "http://yourserveraddress.com";
    private static final int SERVER_PORT = 8080;

    private NfcAdapter mNfcAdapter;
    private TextView mTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ViewPager viewPager = findViewById(R.id.viewPager);
        setupViewPager(viewPager);

        TabLayout tabLayout = findViewById(R.id.tabLayout);
        tabLayout.setupWithViewPager(viewPager);

        mNfcAdapter = NfcAdapter.getDefaultAdapter(this);
        if (mNfcAdapter == null) {
            Toast.makeText(this, "NFC is not available", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        if (!mNfcAdapter.isEnabled()) {
            Toast.makeText(this, "Please enable NFC", Toast.LENGTH_LONG).show();
        }
    }

    private void setupViewPager(ViewPager viewPager) {
        ViewPagerAdapter adapter = new ViewPagerAdapter(getSupportFragmentManager());
        adapter.addFragment(new ScanTagsFragment(), "Scan...");
        adapter.addFragment(new WriteTagFragment(), "Write Tag");
        adapter.addFragment(new TabFragment("Config"), "Tab 3");
        viewPager.setAdapter(adapter);
    }

    @Override
    protected void onResume() {
        Log.d(TAG, "onResume");
        super.onResume();
        if (mNfcAdapter.isEnabled()) {
            mNfcAdapter.enableReaderMode(this, this,
                                         NfcAdapter.FLAG_READER_NFC_A |
                                         NfcAdapter.FLAG_READER_NFC_B |
                                         NfcAdapter.FLAG_READER_NFC_BARCODE |
                                         NfcAdapter.FLAG_READER_NFC_F |
                                         NfcAdapter.FLAG_READER_NFC_V,
                                         null);
        }
    }

    @Override
    protected void onPause() {
        Log.d(TAG, "onPause");
        super.onPause();
        mNfcAdapter.disableReaderMode(this);
    }

    @Override
    public void onTagDiscovered (Tag tag) {
        Log.d(TAG, "Tag Discovered: " + tag.toString());
        Ndef ndef = Ndef.get(tag);
        if (ndef == null) {
            Toast.makeText(this, "Incompatible tag (not NDEF)", Toast.LENGTH_LONG).show();
        }

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
            Toast.makeText(this, "Error reading tag, please retry", Toast.LENGTH_LONG).show();
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

    private void sendToServer(String data) {
        new SendToServerTask().execute(data);
    }

    private class SendToServerTask extends AsyncTask<String, Void, Void> {

        @Override
        protected Void doInBackground(String... params) {
            HttpURLConnection urlConnection = null;
            try {
                URL url = new URL(SERVER_URL + ":" + SERVER_PORT);
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("POST");
                urlConnection.setDoOutput(true);
                urlConnection.setChunkedStreamingMode(0);

                OutputStream out = urlConnection.getOutputStream();
                out.write(params[0].getBytes());
                out.flush();
                out.close();

                int responseCode = urlConnection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    Log.d(TAG, "Data sent successfully to server");
                } else {
                    Log.e(TAG, "Failed to send data to server. Response code: " + responseCode);
                }
            } catch (IOException e) {
                Log.e(TAG, "Error sending data to server: " + e.getMessage());
            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
            }
            return null;
        }
    }
}
