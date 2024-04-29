package com.skz81.simplenfc2http;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.app.DatePickerDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.SpinnerAdapter ;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.LiveData;
import java.io.IOException;
import java.util.Calendar;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import android.nfc.tech.Ndef;
import android.nfc.FormatException;
import android.nfc.NdefRecord;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


public class WriteTagFragment extends Fragment
                              implements Varieties.UpdateListener {

    protected class FormatTagNdefListener implements MainActivity.NdefTagListener {
        private MainActivity mainActivity;
        private AppConfiguration config;
        private AlertDialog formatTagDialog = null;
        private String newUUID = null;

        public FormatTagNdefListener(MainActivity main) {
            mainActivity = main;
            this.config = AppConfiguration.instance();
            newUUID = UUID.randomUUID().toString();
            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
            builder.setTitle("Format Tag")
                    .setMessage("This will format the tag, with\nnew UUID: " + newUUID
                                + ".\nPlease scan the tag to proceed...")
                    .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            if (mainActivity != null) {
                                mainActivity.stopNFCScan();
                                mainActivity.resetCustomNdefListener();
                            }
                            dialog.dismiss();
                            formatTagDialog = null;
                            newUUID = null;
                        }
                    });
            mainActivity.stopNFCScan();
            mainActivity.setCustomNdefListener(this);
            formatTagDialog = builder.create();
            formatTagDialog.show();
            mainActivity.startNFCScan();
        }

        @Override
        public void onNDEFDiscovered(MainActivity.NdefAdapter ndef) {
            JSONObject json_data = new JSONObject();
            NdefMessage message = new NdefMessage(
                    NdefRecord.createTextRecord("", newUUID),
                    NdefRecord.createTextRecord("", mainActivity.appName())
            );
            Log.i(TAG, "in Writer:onNDEFDiscovered, write tag info");
            Log.i(TAG, "UUID: " + newUUID);
            Log.i(TAG, "AppName: " + mainActivity.appName());
            try {
                json_data.put("uuid", newUUID);
                NdefMessage cached = ndef.getNdefMessage();
                if (cached == null) {
                    throw new IOException("Tag is empty");
                }
                NdefRecord[] records = cached.getRecords();
                if (records == null || records.length < 2) {
                    throw new IOException("Bad TAG format: empty message or not enough records");
                }
                if (records[0].getType().equals("T") ||
                    records[1].getPayload().equals(mainActivity.appName())) {
                    throw new IOException("Bad TAG format: bad record type/content");
                }
                String old_uuid = new String(records[0].getPayload()).substring(2);
                Log.i(TAG, "Found old tag UUID to remove:" + old_uuid);
                json_data.put("old_uuid", old_uuid);
            } catch (JSONException e) {
                Log.e(TAG, "Error serializing JSON for create tag:" + e.getMessage());
                // TODO : O_o !!!!
            } catch (IOException e) {
                Log.d(TAG, "No old UUID to remove found on the tag.");
            }
            try {
                ndef.connect();
                if (ndef.isConnected()) {
                    ndef.writeNdefMessage(message);
                }
                // check we're still connected
                if (!ndef.isConnected()) {
                    throw new IOException("NFC connection lost");
                }
            // TODO : O_o !!!!
            } catch (IOException e) {
                Log.e(TAG, "IO Error while writing NDEF messages: " + e.getMessage());
            } catch (FormatException e) {
                Log.e(TAG, "Format Error while writing NDEF messages: " + e.getMessage());
            } finally {
                Log.d(TAG, "Send JSON to server for tag ID creation:" + json_data.toString());
                new SendToServerTask(new SendToServerTask.ReplyListener() {
                    @Override public void onReplyFromServer(String data) {
                        if (data == null || !data.strip().equals("{\"result\": \"OK\"}")) {
                            mainActivity.displayError(TAG,
                                "Received unexpected JSON result with 200 OK on tag creation:\n" +
                                ((data != null) ? data : "(null)"));
                        }
                        mainActivity.toastDisplay(TAG, "Tag formatted.", true);
                    }
                    @Override public void onError(String error) {
                        mainActivity.displayError(TAG, "Can't create tag UUID on server: " + error);
                    }
                }).POST(config.getServerURL() + config.CREATE_TAG_URL, json_data.toString());
            }
            if (formatTagDialog != null) {
                formatTagDialog.dismiss();
                formatTagDialog = null;
            }
            mainActivity.resetCustomNdefListener();
        }
    }

    private class VarietyItem {
        private String label;
        private int id;

        public VarietyItem(String label, int id) {
            this.label = label;
            this.id= id;
        }
        public String label() {return label;}
        public int id() {return id;}
        @Override public String toString() {return label;}
    }

    private static final String TAG = "AutoWatS-NFC-update";

    private AppConfiguration config;

    private EditText plantId;
    private Spinner varietySpinner;
    private RadioGroup genderRadioGroup;
    private EditText germinationDateEdit;
    private ImageButton germinationDeleteButton;
    private EditText bloomingDateEdit;
    private ImageButton bloomingDeleteButton;
    private EditText yieldingDateEdit;
    private ImageButton yieldingDeleteButton;
    private Button updateInfoButton;
    private Button scanTagButton;
    private Button newPlantButton;

    private MainActivity mainActivity;
    private Calendar calendar;

    private JSONInfoAdapter plantInfoAdapter;

    private List<VarietyItem> varieties = null;

    public WriteTagFragment() {}

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        config = AppConfiguration.instance();
        mainActivity = (MainActivity) context;
        Log.d(TAG, "WriteTagFragment onAttach... mainActivity=" + (mainActivity!=null ? mainActivity.toString() : "null"));
    }

    private void addDateFieldListener(EditText textview) {
        textview.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDatePickerDialog(textview);
            }
        });
    }
    private void addDateDeleteButtonListener(EditText textview, ImageButton btn) {
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                textview.getText().clear();
            }
        });
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_write_tag, container, false);

        plantId = view.findViewById(R.id.plantId);
        varietySpinner = view.findViewById(R.id.varietySpinner);
        genderRadioGroup = view.findViewById(R.id.genderRadioGroup);
        germinationDateEdit = view.findViewById(R.id.germinationDateEdit);
        germinationDeleteButton = view.findViewById(R.id.germDateDelBtn);
        bloomingDateEdit = view.findViewById(R.id.bloomingDateEdit);
        bloomingDeleteButton = view.findViewById(R.id.bloomDateDelBtn);
        yieldingDateEdit = view.findViewById(R.id.yieldingDateEdit);
        yieldingDeleteButton = view.findViewById(R.id.yieldDateDelBtn);
        updateInfoButton = view.findViewById(R.id.updateInfoButton);
        scanTagButton = view.findViewById(R.id.scanTagButton);
        newPlantButton = view.findViewById(R.id.newPlantButton);
        calendar = Calendar.getInstance();

        addDateFieldListener(germinationDateEdit);
        addDateFieldListener(bloomingDateEdit);
        addDateFieldListener(yieldingDateEdit);
        addDateDeleteButtonListener(germinationDateEdit, germinationDeleteButton);
        addDateDeleteButtonListener(bloomingDateEdit, bloomingDeleteButton);
        addDateDeleteButtonListener(yieldingDateEdit, yieldingDeleteButton);

        // Generate ID button click listener
        newPlantButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                buttonNewPlantClicked();
            }
        });

        // Write Tag button click listener
        scanTagButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                buttonScanTagClicked();
            }
        });

        // Write Tag button click listener
        updateInfoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                buttonUpdateInfoClicked();
            }
        });

        Varieties varieties = Varieties.instance();
        varieties.observe(this);


        plantInfoAdapter = new JSONInfoAdapter(this, FetchPlantInfo.sharedJSONView());

        plantInfoAdapter.addAttribute("UUID", String.class,
                                      value -> plantId.setText((String) value),
                                      () -> plantId.getText().clear());
        plantInfoAdapter.addAttribute("variety", Integer.class,
                                      value -> setVarietySpinner((Integer) value),
                                      () -> varietySpinner.setSelection(0));
        plantInfoAdapter.addAttribute("sex", Integer.class,
                                      value -> setGenderField((Integer) value),
                                      () -> genderRadioGroup.clearCheck());
        plantInfoAdapter.addAttribute("germination_date", String.class,
                                      value -> germinationDateEdit.setText((String) value),
                                      () -> germinationDateEdit.getText().clear());
        plantInfoAdapter.addAttribute("blooming_date", String.class,
                                      value -> bloomingDateEdit.setText((String) value),
                                      () -> bloomingDateEdit.getText().clear());
        plantInfoAdapter.addAttribute("yielding_date", String.class,
                                      value -> yieldingDateEdit.setText((String) value),
                                      () -> yieldingDateEdit.getText().clear());

        return view;
    }

    private void setVarietySpinner(int varietyId) {
        Varieties.Variety variety = Varieties.instance().getById(varietyId);
        SpinnerAdapter spinnerAdapter = varietySpinner.getAdapter();
        for (int i = 0; i < spinnerAdapter.getCount(); i++) {
            VarietyItem item = (VarietyItem)spinnerAdapter.getItem(i);
            if (item.id() == variety.id()) {
                varietySpinner.setSelection(i);
                break; // Exit the loop once the item is found
            }
        }
    }

    private void setGenderField(int index) {
        RadioButton radioBtn = (RadioButton) genderRadioGroup.getChildAt(index);
        if (radioBtn != null) {
            radioBtn.setChecked(true);
        } else {
            genderRadioGroup.clearCheck();
        }
    }

    @Override
    public void onVarietiesUpdated(Varieties update) {
        if (update == null) {
            varieties = new ArrayList<>();
            Log.i(TAG, "onVarietiesUpdated: None");
        } else {
            varieties = update.getAll().stream().map(
                variety -> new VarietyItem(variety.name(), variety.id())
            ).collect(Collectors.toCollection(ArrayList::new));
            Log.i(TAG, "onVarietiesUpdated: " + varieties);
        }
        updateSpinnor();
    }

    private void updateSpinnor() {
        if (varieties == null || varietySpinner == null) return;

        ArrayAdapter<VarietyItem> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, varieties);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        varietySpinner.setAdapter(adapter);
    }

    void buttonNewPlantClicked() {
        // the listener is "one-shot" and resets default listener
        mainActivity.setCustomNdefListener(new FormatTagNdefListener(mainActivity));
    }

    void buttonScanTagClicked() {
    }

    void buttonUpdateInfoClicked() {
        VarietyItem variety = (VarietyItem) varietySpinner.getSelectedItem();

        JSONObject json_data = new JSONObject();
        try {
            json_data.put("UUID", plantId.getText());
            json_data.put("variety", variety.id());
            json_data.put("sex", genderRadioGroup.getCheckedRadioButtonId());
            json_data.put("germination_date", germinationDateEdit.getText());
            json_data.put("blooming_date", bloomingDateEdit.getText());
            json_data.put("yielding_date", yieldingDateEdit.getText());
        } catch (JSONException e) {
            Log.e(TAG, "Error while serializing UPDATE req JSON param: " + e.getMessage());
        }
        new SendToServerTask(new SendToServerTask.ReplyListener() {
            @Override public void onReplyFromServer(String data) {
                Log.i(TAG, "uopdate, replyListener.decodeServerResponse(" + data + ")");
                if (data.strip().equals("{\"result\": \"OK\"}")) {
                    mainActivity.toastDisplay(TAG, "Tag data updated !", true);
                }
            }
            @Override public void onError(String error) {
                mainActivity.displayError(TAG, "Error while updating tag: " + error);
            }
        }).POST(config.getServerURL() + config.UPDATE_PLANT_URL, json_data.toString());
    }

    public void showDatePickerDialog(EditText v) {
        // int fieldId = v.getId(); // Get the id of the clicked field
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);
        // Process the selected date based on the field id
        // switch (fieldId) {
        //     case R.id.yieldingDateEdit:
        //         break;
        //     // Add cases for other fields if needed
        // }

        DatePickerDialog datePickerDialog = new DatePickerDialog(requireContext(),
                new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                        // Set the selected date to the EditText
                        String selectedDate = dayOfMonth + "/" + (monthOfYear + 1) + "/" + year;
                        v.setText(selectedDate);
                    }
                }, year, month, day);
        datePickerDialog.show();
    }

}
