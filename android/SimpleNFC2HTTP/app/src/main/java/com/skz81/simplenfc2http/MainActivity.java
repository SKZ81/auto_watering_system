package com.skz81.simplenfc2http;

import java.io.IOException;

import android.app.AlertDialog;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.DialogInterface;
import android.graphics.Color;
import android.nfc.tech.Ndef;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.FormatException;
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
    private Varieties varieties;
    private ScanTagsFragment scanTab;
    private UpdateFragment updateTab;
    private AppConfFragment configTab;
    private ViewPagerAdapter viewPagerAdapter;
    private AlertDialog progressDialog;
    private ViewPager2 viewPager;

    private NfcAdapter mNfcAdapter;
    public interface NdefTagListener {
        void onNDEFDiscovered(NdefAdapter ndef) throws IOException;
    }
    // default listener
    private FetchPlantInfo scanTagListener;
    // current actual listener, can be "overriden"
    private NdefTagListener ndefListener;
    // for debug purpose, tag injection needs to know if scanning is enabled
    private boolean scanning = false;

    public String appName() {return appName;}
    public Varieties varieties() {return varieties;}

    public void displayError(String tag, String message) {
        MainActivity mainActivity = this;
        Log.e(tag, message);
        runOnUiThread(() -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(mainActivity);
            builder.setTitle(tag +" Error")
            .setMessage(message)
            .setNegativeButton("Dismiss", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id)
                    { dialog.dismiss(); }
            });
            AlertDialog dialog = builder.create();
            dialog.show();
        });
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
        ndefListener = scanTagListener;
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
            builder.setCancelable(false);
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
        scanTagListener = new FetchPlantInfo(this);

        viewPager = findViewById(R.id.viewPager);
        TabLayout tabLayout = findViewById(R.id.tabLayout);

        setupViewPager(viewPager);

        new TabLayoutMediator(tabLayout, viewPager,
                (tab, position) -> {
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
        viewPagerAdapter = new ViewPagerAdapter(this);
        scanTab = new ScanTagsFragment();
        updateTab = new UpdateFragment();
        configTab = new AppConfFragment();

        viewPagerAdapter.addPage(scanTab, "Scan", true);
        viewPagerAdapter.addPage(updateTab, "Update", true);
        viewPagerAdapter.addPage(configTab, "Config", true);
        viewPagerAdapter.hidePage(scanTab);
        viewPagerAdapter.hidePage(updateTab);
        viewPager.setAdapter(viewPagerAdapter);
        viewPagerAdapter.updateViewPager();
    }

    public void setCustomNdefListener(NdefTagListener listener) {
        this.ndefListener = listener;
    }

    public void resetCustomNdefListener() {
        this.ndefListener = scanTagListener;
    }

    public void startNFCScan() {
        if (mNfcAdapter != null && mNfcAdapter.isEnabled()) {
            Log.d(TAG, "Start NFC Scan");
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
        scanning = true;
    }

    public void stopNFCScan() {
        if (mNfcAdapter != null) {
            Log.d(TAG, "Stop NFC scan.");
            mNfcAdapter.disableReaderMode(this);
        }
        scanning = false;
    }

    @Override
    public void onTagDiscovered (Tag tag) {
        Log.d(TAG, "Tag Discovered: " + tag.toString());
        Ndef ndef = Ndef.get(tag);

        if (ndef == null) {
            toastDisplay(TAG, "Tag is not NDEF compatible", true);
        } else if (ndefListener != null) {
            try{
                ndefListener.onNDEFDiscovered(new TrueNdefAdapter(ndef));
            } catch (IOException e) {
                toastDisplay(TAG, "Error processing tag: " + e.getMessage(), true);
            }
        }
        try {
            ndef.close();
        } catch (Exception e) {
            Log.w(TAG, "Error closing NDEF connection: " + e.getMessage());
        }
    }

    public void simulateTagScan(String uuid) {
        Log.i(TAG, "simulateTagScan: " + uuid +
                   " scanning: " + (scanning ? "true" : "false"));
        if (scanning) {
            try {
                ndefListener.onNDEFDiscovered(
                    new FakeNdefAdapter(
                        new NdefMessage(
                            NdefRecord.createTextRecord("", uuid),
                            NdefRecord.createTextRecord("", appName))));
            } catch (IOException e) {
                Log.w(TAG, "This exception should NOT happend ! " + e.getMessage());
            }
        }
    }

    public interface NdefAdapter {
        public void connect() throws java.io.IOException;
        public boolean isConnected();
        public NdefMessage getNdefMessage();
        public void writeNdefMessage(NdefMessage message) throws java.io.IOException,
                                                                 FormatException;
    }

    public static class TrueNdefAdapter implements NdefAdapter {
        private Ndef ndef;

        TrueNdefAdapter(Ndef ndef) {
            this.ndef = ndef;
        }

        @Override
        public void connect() throws IOException {
            ndef.connect();
        }
        @Override
        public boolean isConnected() {
            return ndef.isConnected();
        }
        @Override
        public NdefMessage getNdefMessage() {
            return ndef.getCachedNdefMessage();
        }
        @Override
        public void writeNdefMessage(NdefMessage message) throws IOException,
                                                                 FormatException {
            ndef.writeNdefMessage(message);
        }
    }

    public static class FakeNdefAdapter implements NdefAdapter {
        private NdefMessage message;
        public FakeNdefAdapter(NdefMessage message) {
            this.message = message;
        }
        @Override
        public void connect() {}
        @Override
        public boolean isConnected() {
            return true;
        }
        @Override
        public NdefMessage getNdefMessage() {
            return message;
        }
        @Override
        public void writeNdefMessage(NdefMessage message) throws IOException {
        }
    }
}
