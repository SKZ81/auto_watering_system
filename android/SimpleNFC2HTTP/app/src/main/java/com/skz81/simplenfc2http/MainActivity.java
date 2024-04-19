package com.skz81.simplenfc2http;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.nfc.tech.Ndef;
import android.nfc.NfcAdapter;
import android.nfc.Tag;

import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.widget.ViewPager2;
import com.google.android.material.tabs.TabLayout;;
import com.google.android.material.tabs.TabLayoutMediator;

import com.skz81.simplenfc2http.R;
import com.skz81.simplenfc2http.ScanTagsFragment;
import com.skz81.simplenfc2http.WriteTagFragment;
import com.skz81.simplenfc2http.AppConfFragment;
import com.skz81.simplenfc2http.AppConfiguration;
import com.skz81.simplenfc2http.Varieties;

public class MainActivity extends FragmentActivity implements NfcAdapter.ReaderCallback {
    private static final String TAG = "AutoWatS-NFC";

    private String appName;
    private NfcAdapter mNfcAdapter;
    private TextView mTextView;
    private NdefTagCallback tagCB;
    private AppConfiguration config;
    private Varieties varieties;
    private ScanTagsFragment scanTab;
    private WriteTagFragment updateTab;
    private AppConfFragment configTab;
    private ViewPagerAdapter viewPagerAdapter;

    public String appName() {return appName;}
    public Varieties varieties() {return varieties;}

    public void dumpError(String tag, String message) {
        toastDisplay(tag, message, true);
    }

    public void toastDisplay(String tag, String message, boolean longDuration) {
        MainActivity mainActivity = this;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Log.e(tag, message);
                Toast.makeText(mainActivity, message,
                               longDuration ?
                                    Toast.LENGTH_SHORT :
                                    Toast.LENGTH_LONG
                              ).show();
            }
        });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "is starting...");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        AppConfiguration.static_init(this);
        config = AppConfiguration.instance();
        tagCB = null;

        ViewPager2 viewPager = findViewById(R.id.viewPager);
        setupViewPager(viewPager);

        TabLayout tabLayout = findViewById(R.id.tabLayout);
        new TabLayoutMediator(tabLayout, viewPager,
                (tab, position) -> {
                    // Log.i(TAG, "onConfigureTab, tab:" + tab.toString() + ", pos:" + position);
                    tab.setText(viewPagerAdapter.getPageTitle(position));
                }
        ).attach();

        if (varieties == null) {
            varieties = new Varieties(this, config.getServerURL(),
                                      config.VARIETIES_URL, config.VARIETIES_IMG_URL);
        }
        try {
            PackageManager packageManager = this.getPackageManager();
            ApplicationInfo applicationInfo = packageManager.getApplicationInfo(this.getPackageName(), 0);
            appName = (String) packageManager.getApplicationLabel(applicationInfo);
        } catch (PackageManager.NameNotFoundException e) {
            appName = "APPNAME_NOT_FOUND";
        }

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

    private void setupViewPager(ViewPager2 viewPager) {
        viewPagerAdapter = new ViewPagerAdapter(this);
        scanTab = new ScanTagsFragment();
        updateTab = new WriteTagFragment();
        configTab = new AppConfFragment();

        viewPagerAdapter.addFragment(scanTab, "Scan");
        viewPagerAdapter.addFragment(updateTab, "Update");
        viewPagerAdapter.addFragment(configTab, "Config");
        viewPager.setAdapter(viewPagerAdapter);
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
