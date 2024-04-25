package com.skz81.simplenfc2http;

import android.app.AlertDialog;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

public class MainActivity extends FragmentActivity
                          implements NfcAdapter.ReaderCallback,
                                     SendToServerTask.ConnectionStateWatcher {

    private static final String TAG = "AutoWatS-NFC";

    private String appName;
    private NfcAdapter mNfcAdapter;
    private NdefTagCallback tagCB;
    private Varieties varieties;
    private ScanTagsFragment scanTab;
    private WriteTagFragment updateTab;
    private AppConfFragment configTab;
    private ViewPagerAdapter viewPagerAdapter;
    private AlertDialog progressDialog;
    private ViewPager2 viewPager;

    public String appName() {return appName;}
    public Varieties varieties() {return varieties;}

    public void dumpError(String tag, String message) {
        toastDisplay(tag, message, true);
    }

    public void toastDisplay(String tag, String message, boolean longDuration) {
        MainActivity mainActivity = this;
        runOnUiThread(() -> {
            Log.e(tag, message);
            Toast.makeText(mainActivity, message,
                           longDuration ?
                                Toast.LENGTH_SHORT :
                                Toast.LENGTH_LONG
                          ).show();
        });
    }

    @Override
    public void onServerConnectionOk() {
        hideLoadingDialog();
        viewPagerAdapter.showPage(scanTab);
        viewPagerAdapter.showPage(updateTab);
        viewPagerAdapter.updateViewPager();
    }

    @Override
    public void onServerConnectionError(String error) {
        hideLoadingDialog();
        toastDisplay(TAG, error + "\nPlease check config.", true);
        runOnUiThread(() -> configTab.activateConnectButton());
    }

    private void showLoadingDialog() {
        if(progressDialog == null){
            int llPadding = 30;
            LinearLayout ll = new LinearLayout(this);
            ll.setOrientation(LinearLayout.HORIZONTAL);
            ll.setPadding(llPadding, llPadding, llPadding, llPadding);
            ll.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams llParam = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            llParam.gravity = Gravity.CENTER;
            ll.setLayoutParams(llParam);

            ProgressBar progressBar = new ProgressBar(this);
            progressBar.setIndeterminate(true);
            progressBar.setPadding(0, 0, llPadding, 0);
            progressBar.setLayoutParams(llParam);

            llParam = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            llParam.gravity = Gravity.CENTER;
            TextView tvText = new TextView(this);
            tvText.setText("Connecting to server...");
            tvText.setTextColor(Color.parseColor("#D0D0D0"));
            tvText.setTextSize(20);
            tvText.setLayoutParams(llParam);

            ll.addView(progressBar);
            ll.addView(tvText);

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setCancelable(true);
            builder.setView(ll);

            progressDialog = builder.create();
            progressDialog.show();
            Window window = progressDialog.getWindow();
            if (window != null) {
                WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams();
                layoutParams.copyFrom(progressDialog.getWindow().getAttributes());
                layoutParams.width = LinearLayout.LayoutParams.WRAP_CONTENT;
                layoutParams.height = LinearLayout.LayoutParams.WRAP_CONTENT;
                progressDialog.getWindow().setAttributes(layoutParams);
            }
        }
    }

    public boolean isDialogVisible(){
        if(progressDialog != null){
            return progressDialog.isShowing();
        }else {
            return false;
        }
    }

    private void hideLoadingDialog() {
        Log.i(TAG, "hide dialog.");
        if(progressDialog != null){
            progressDialog.dismiss();
            progressDialog = null;
        } else {
            Log.i(TAG, "Dialog was null!");
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "is starting...");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        AppConfiguration.static_init(this);
        tagCB = null;

        viewPager = findViewById(R.id.viewPager);
        TabLayout tabLayout = findViewById(R.id.tabLayout);

        setupViewPager(viewPager);

        new TabLayoutMediator(tabLayout, viewPager,
                (tab, position) -> {
                    // Log.i(TAG, "onConfigureTab, tab:" + tab.toString() + ", pos:" + position);
                    tab.setText(viewPagerAdapter.getPageTitle(position));
                }
        ).attach();


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
            Log.e(TAG, "NFC not available.");
            if (BuildConfig.ABORT_NO_NFC) {
                finish();
                return;
            }
        }

        if (mNfcAdapter != null && !mNfcAdapter.isEnabled()) {
            Toast.makeText(this, "Please enable NFC", Toast.LENGTH_LONG).show();
            Log.w(TAG, "NFC Disabled !..");
        }
        connectServer();
    }

    public void connectServer() {
        showLoadingDialog();
        varieties = Varieties.instance(this);
    }

    private void setupViewPager(ViewPager2 viewPager) {
        // Log.d(TAG, "this=")
        viewPagerAdapter = new ViewPagerAdapter(this);
        scanTab = new ScanTagsFragment();
        updateTab = new WriteTagFragment();
        configTab = new AppConfFragment();

        viewPagerAdapter.addPage(scanTab, "Scan", true);
        viewPagerAdapter.addPage(updateTab, "Update", true);
        viewPagerAdapter.addPage(configTab, "Config", true);
        viewPagerAdapter.hidePage(scanTab);
        viewPagerAdapter.hidePage(updateTab);
        viewPager.setAdapter(viewPagerAdapter);
        viewPagerAdapter.updateViewPager();
    }

    public void startNFCScan(NdefTagCallback callback) {
        if (mNfcAdapter != null && mNfcAdapter.isEnabled()) {
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
