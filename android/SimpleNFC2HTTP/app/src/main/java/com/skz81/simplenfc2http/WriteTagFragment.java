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

    protected class NdefWriteListener implements NdefTagCallback {
        private String appName = null;
        private WriteTagFragment parent;
        private AppConfiguration config;

        public NdefWriteListener(WriteTagFragment fragment, String appName,
                                 AppConfiguration config) {
            this.parent = fragment;
            this.appName = appName;
            this.config = config;
        }

        @Override
        public void onNDEFDiscovered(Ndef ndef) {
            JSONObject json_data = new JSONObject();
            NdefMessage message = new NdefMessage(
                    NdefRecord.createTextRecord("", parent.getNewUUID()),
                    NdefRecord.createTextRecord("", appName)
            );
            Log.i(TAG, "in Writer:onNDEFDiscovered, write tag info");
            Log.i(TAG, "UUID: " + parent.getNewUUID());
            Log.i(TAG, "AppName: " + appName);
            try {
                json_data.put("uuid", parent.getNewUUID());
                NdefMessage current = ndef.getCachedNdefMessage();
                if (current == null) {throw new IOException("Tag is empty");}
                NdefRecord[] records = current.getRecords();
                if (records == null || records.length < 2) {
                    throw new IOException("Bad TAG format: no message or not enough records");
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
            } catch (IOException e) {
                // Log error message
                Log.e(TAG, "IO Error while writing NDEF messages: " + e.getMessage());
                // Toast.makeText(mainActivity, "Error writing tag...", Toast.LENGTH_LONG).show();
            } catch (FormatException e) {
                // Log error message
                Log.e(TAG, "Format Error while writing NDEF messages: " + e.getMessage());
                // Toast.makeText(mainActivity, "Error writing tag...", Toast.LENGTH_LONG).show();
            } finally {
                newUUID = null;
                try {
                    ndef.close();
                } catch (Exception e) {
                    Log.w(TAG, "Error closing NDEF connection: " + e.getMessage());
                }
            }
            if (scanTagDialog != null) {
                scanTagDialog.dismiss();
                scanTagDialog = null;
            }
            parent.stopScan();
            Log.d(TAG, "Send JSON to server for tag ID creation:" + json_data.toString());
            new SendToServerTask(new SendToServerTask.ReplyCB() {
                @Override public void onReplyFromServer(String data) {
                    if (data == null || data != "OK") {
                        parent.mainActivity.dumpError(TAG,
                                "not 'OK' reply from server (create tag ID)");
                    }
                    Log.d(TAG, "Tag ID=" + newUUID + " created on server.");
                }
                @Override public void onError(String error) {
                    parent.mainActivity.dumpError(TAG, "Can't create tag in server DB:" + error);
                }
            }).POST(config.getServerURL() + config.CREATE_TAG_URL, json_data.toString());
        }
    }

    protected class NdefReadListener implements NdefTagCallback {
        private WriteTagFragment parent;
        public NdefReadListener(WriteTagFragment fragment) {
            parent = fragment;
        }
        @Override
        public void onNDEFDiscovered(Ndef ndef) {
            NdefMessage message = ndef.getCachedNdefMessage();
            if (message == null) {return;}
            NdefRecord[] records = message.getRecords();
            if (records != null) {
                for (int j = 0; j < records.length; j++) {
                    NdefRecord record = records[j];
                    Log.i(TAG, "  Record " + (j + 1) + ":");
                    Log.i(TAG, "    TNF (Type Name Format): " + record.getTnf());
                    Log.i(TAG, "    Type: " + new String(record.getType()));
                    Log.i(TAG, "    Payload: " + new String(record.getPayload()));
                }
            }
            try {
                ndef.close();
            } catch (Exception e) {
                Log.w(TAG, "Error closing NDEF connection: " + e.getMessage(), e);
            }
            if (scanTagDialog != null) {
                scanTagDialog.dismiss();
                scanTagDialog = null;
            }
            parent.stopScan();
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
    private AlertDialog scanTagDialog = null;

    private JSONInfoAdapter plantInfoAdapter;
    private SharedJSONInfo plantInfo;

    private List<VarietyItem> varieties = null;
    private String newUUID = null;

    private NdefReadListener ndefReader;
    private NdefWriteListener ndefWriter;

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

        ndefReader = new NdefReadListener(this);
        ndefWriter = new NdefWriteListener(this, mainActivity.appName(), config);

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

        plantInfo = new ViewModelProvider(requireActivity())
                .get(SharedJSONInfo.class);
        plantInfoAdapter = new JSONInfoAdapter(this, plantInfo);

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
        } else {
            varieties = update.getAll().stream().map(
                variety -> new VarietyItem(variety.name(), variety.id())
            ).collect(Collectors.toCollection(ArrayList::new));
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
        newUUID = UUID.randomUUID().toString();
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Format Tag")
                .setMessage("This will format the tag, with\nnew UUID: " + newUUID
                            + ".\nPlease scan the tag to proceed...")
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        if (mainActivity != null) {
                            mainActivity.stopNFCScan();
                        }
                        dialog.dismiss();
                        scanTagDialog = null;
                        newUUID = null;
                    }
                });
        if (mainActivity != null) {
            mainActivity.startNFCScan(ndefWriter);
        }
        scanTagDialog = builder.create();
        scanTagDialog.show();
    }

    void buttonScanTagClicked() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Scan Tag")
                .setMessage("Please scan the tag to display data...")
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        if (mainActivity != null) {
                            mainActivity.stopNFCScan();
                        }
                        dialog.dismiss();
                        scanTagDialog = null;
                    }
                });
        if (mainActivity != null) {
            mainActivity.startNFCScan(ndefReader);
        }
        scanTagDialog = builder.create();
        scanTagDialog.show();
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
        new SendToServerTask(new SendToServerTask.ReplyCB() {
            @Override public void onReplyFromServer(String data) {
                if (data == "OK") {
                    Toast.makeText(mainActivity, "Tag data updated !", Toast.LENGTH_LONG).show();
                }
            }
            @Override public void onError(String error) {
                Log.e(TAG, "Error while updating tag: " + error);
                Toast.makeText(mainActivity, "Error while updating tag: " + error, Toast.LENGTH_LONG).show();
            }
        }).POST(config.getServerURL() + config.UPDATE_PLANT_URL, json_data.toString());
    }

    public void stopScan() {
        if (mainActivity != null) {
            mainActivity.stopNFCScan();
        }
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

    public String getNewUUID() {
        return newUUID;
    }
}
