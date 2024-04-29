package com.skz81.simplenfc2http;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.LiveData;

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
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
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

public class ScanTagsFragment extends Fragment {

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
    private JSONInfoAdapter plantInfoAdapter;

    public ScanTagsFragment() {}

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mainActivity = (MainActivity) context;
    }

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
        // varietyIcon.setImageResource(R.drawable.your_image); // Replace 'your_image' with your image resource
        varietyName.setText("Variety Name");
        plantId.setText("<UUID>");
        germinationDate.setText("<date>");
        bloomingDate.setText("<date>");
        yieldingDate.setText("<date>");

        plantInfoAdapter = new JSONInfoAdapter(this, FetchPlantInfo.sharedJSONView());
        plantInfoAdapter.addAttribute("UUID", String.class,
                                      value -> plantId.setText((String) value),
                                      () -> plantId.setText(""));
        plantInfoAdapter.addAttribute("variety", Integer.class,
                                       value -> setVarietyFields((Integer) value),
                                       () -> clearVarietyFields());
        plantInfoAdapter.addAttribute("sex", Integer.class,
                                      value -> setGenderField((Integer) value),
                                      () -> genderRadioGroup.clearCheck());
        plantInfoAdapter.addAttribute("germination_date", String.class,
                                      value -> germinationDate.setText((String) value),
                                      () -> germinationDate.setText(""));
        plantInfoAdapter.addAttribute("blooming_date", String.class,
                                      value -> bloomingDate.setText((String) value),
                                      () -> bloomingDate.setText(""));
        plantInfoAdapter.addAttribute("yielding_date", String.class,
                                      value -> yieldingDate.setText((String) value),
                                      () -> yieldingDate.setText(""));

        Button injectTagButton = new Button(getContext());

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        injectTagButton.setText("Inject Tag");
        params.setMargins(0, 16, 0, 0); // 16dp marginTop
        injectTagButton.setLayoutParams(params);
        ((LinearLayout) view.findViewById(R.id.scanTabLayout)).addView(injectTagButton);

        injectTagButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mainActivity.simulateTagScan("b8c9de57-669f-4dc4-bd9f-eb1e6e6e67d9");
            }
        });

        return view;
    }

    private void setVarietyFields(int varietyId) {
        Varieties.Variety variety = Varieties.instance().getById(varietyId);
        varietyName.setText(variety.name());
        new LoadImageTask(varietyIcon).execute(variety.imageBase64());
    }

    private void clearVarietyFields() {
        varietyIcon.setImageBitmap(null);
        varietyName.setText("");
    }
    private void setGenderField(int gender) {
        RadioButton radioBtn = (RadioButton) genderRadioGroup.getChildAt(gender);
        if (radioBtn != null) {
            radioBtn.setChecked(true);
        } else {
            genderRadioGroup.clearCheck();
        }
    }

    private void resetGenderField(int gender) {
        RadioButton radioBtn = (RadioButton) genderRadioGroup.getChildAt(gender);
        radioBtn.setChecked(true);
    }

    @Override
    public void onResume() {
        super.onResume();
        mainActivity.startNFCScan();
    }

    @Override
    public void onPause() {
        mainActivity.stopNFCScan();
        super.onPause();
    }

    public class LoadImageTask {
        private ImageView view;

        public LoadImageTask(ImageView view) {
            this.view = view;
        }

        public void execute(final String base64Image) {
            ExecutorService executor = Executors.newSingleThreadExecutor();
            Future<Bitmap> future = executor.submit(new Callable<Bitmap>() {
                @Override
                public Bitmap call() throws Exception {
                    byte[] decodedBytes = Base64.decode(base64Image, Base64.DEFAULT);
                    InputStream inputStream = new ByteArrayInputStream(decodedBytes);
                    return BitmapFactory.decodeStream(inputStream);
                }
            });

            executor.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        final Bitmap bitmap = future.get();
                        view.post(new Runnable() {
                            @Override
                            public void run() {
                                if (bitmap != null && view != null) {
                                    view.setImageBitmap(bitmap);
                                }
                            }
                        });
                    } catch (Exception e) {
                        Log.e(TAG, "Error setting varity image:" + e.getMessage());
                    }
                }
            });
        }
    }
}
