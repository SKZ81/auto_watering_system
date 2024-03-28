package com.skz81.simplenfc2http;

import android.app.Activity;
import android.nfc.tech.Ndef;
import android.nfc.NfcAdapter;
import android.nfc.Tag;

import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.FragmentActivity;
import androidx.viewpager.widget.ViewPager;
import com.google.android.material.tabs.TabLayout;;

import com.skz81.simplenfc2http.R;
import com.skz81.simplenfc2http.ScanTagsFragment;
import com.skz81.simplenfc2http.WriteTagFragment;
import com.skz81.simplenfc2http.TabFragment;



public class MainActivity extends FragmentActivity implements NfcAdapter.ReaderCallback {
    private static final String TAG = "AutoWatS-NFC";

    private NfcAdapter mNfcAdapter;
    private TextView mTextView;
    private NdefTagCallback tagCB;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tagCB = null;

        ViewPager viewPager = findViewById(R.id.viewPager);
        setupViewPager(viewPager);

        TabLayout tabLayout = findViewById(R.id.tabLayout);
        tabLayout.setupWithViewPager(viewPager);

        mNfcAdapter = NfcAdapter.getDefaultAdapter(this);
        if (mNfcAdapter == null) {
            Toast.makeText(this, "NFC is not available", Toast.LENGTH_LONG).show();
            finish();
            Log.e(TAG, "NFC not available.");
            return;
        }

        if (!mNfcAdapter.isEnabled()) {
            Toast.makeText(this, "Please enable NFC", Toast.LENGTH_LONG).show();
            Log.w(TAG, "NFC Disabled !..");
        }

    }

    private void setupViewPager(ViewPager viewPager) {
        ViewPagerAdapter adapter = new ViewPagerAdapter(getSupportFragmentManager());
        adapter.addFragment(new ScanTagsFragment(), "Scan...");
        adapter.addFragment(new WriteTagFragment(), "Write Tag");
        adapter.addFragment(new TabFragment("Config"), "Tab 3");
        viewPager.setAdapter(adapter);
    }

    public void startNFCScan(NdefTagCallback callback) {
        if (mNfcAdapter.isEnabled()) {
            Log.d(TAG, "Start NFC Scan");
            tagCB = callback;
            mNfcAdapter.enableReaderMode(this, this,
                                         NfcAdapter.FLAG_READER_NFC_A |
                                         NfcAdapter.FLAG_READER_NFC_B |
                                         NfcAdapter.FLAG_READER_NFC_BARCODE |
                                         NfcAdapter.FLAG_READER_NFC_F |
                                         NfcAdapter.FLAG_READER_NFC_V,
                                         null);
        } else {
            Log.w(TAG, "NFC disabled, can't start scan !");
        }
    }

    public void stopNFCScan() {
        if (tagCB != null) {
            Log.d(TAG, "Stop NFC scan.");
            tagCB = null;
            mNfcAdapter.disableReaderMode(this);
        }
    }

    @Override
    public void onTagDiscovered (Tag tag) {
        Log.d(TAG, "Tag Discovered: " + tag.toString());
        Ndef ndef = Ndef.get(tag);

        if (ndef == null) {
            Toast.makeText(this, "Tag is not NDEF compatible", Toast.LENGTH_LONG).show();
        } else if (tagCB != null) {
            tagCB.onNDEFDiscovered(ndef);
        }
    }
}
