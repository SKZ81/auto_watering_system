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


public class UpdateFragment extends Fragment
                              /*implements Varieties.UpdateListener*/ {

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
    private Button formatTagButton;
    private Button discardTagButton;

    private MainActivity mainActivity;
    private Calendar calendar;

    private LocalDatabase database;
    private JSONInfoAdapter plantInfoAdapter;

    private List<VarietyItem> varieties = null;
    // save selected varietyId. If varieties are updated, this info is used to retrigger
    // item selection in varietySpinner
    private int spinnerVarietyId = -1;

    public UpdateFragment() {}

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        config = AppConfiguration.instance();
        mainActivity = (MainActivity) context;
        Log.d(TAG, "UpdateFragment onAttach... mainActivity=" + (mainActivity!=null ? mainActivity.toString() : "null"));
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
        View view = inflater.inflate(R.layout.fragment_update, container, false);

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
        formatTagButton = view.findViewById(R.id.formatTagButton);
        discardTagButton = view.findViewById(R.id.discardTagButton);
        calendar = Calendar.getInstance();

        addDateFieldListener(germinationDateEdit);
        addDateFieldListener(bloomingDateEdit);
        addDateFieldListener(yieldingDateEdit);
        addDateDeleteButtonListener(germinationDateEdit, germinationDeleteButton);
        addDateDeleteButtonListener(bloomingDateEdit, bloomingDeleteButton);
        addDateDeleteButtonListener(yieldingDateEdit, yieldingDeleteButton);

        updateInfoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                buttonUpdateInfoClicked();
            }
        });
        formatTagButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                buttonNewPlantClicked();
            }
        });
        discardTagButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                buttonDiscardUUIDClicked();
            }
        });

        database = LocalDatabase.getInstance(mainActivity);
        database.varietiesDao().getVarietiesLive().observe(this, new Observer<List<Varieties.Variety>>() {
            @Override
            public void onChanged(List<Varieties.Variety> varieties) {
                List<VarietyItem> varietyItems = varieties.stream().map(
                    variety -> new VarietyItem(variety.getName(), variety.getId())
                ).collect(Collectors.toCollection(ArrayList::new));
                // Prepend a "placeholder item" with a special ID
                varietyItems.add(0, new VarietyItem("", -1));
                updateSpinnerItems(varietyItems);
                // redo spinner item selection
                setVarietySpinner(spinnerVarietyId);
            }
        });


        plantInfoAdapter = new JSONInfoAdapter(this, FetchPlantInfo.sharedJSONView());

        plantInfoAdapter.addAttribute("UUID", JSONInfoAdapter.AttributeAdapter.setterCleaner(
                                        String.class,
                                        value -> plantId.setText((String) value),
                                        () -> plantId.getText().clear()));
        plantInfoAdapter.addAttribute("variety", JSONInfoAdapter.AttributeAdapter.setterCleaner(
                                        Integer.class,
                                        value -> setVarietySpinner((Integer) value),
                                        () -> varietySpinner.setSelection(0)));
        plantInfoAdapter.addAttribute("sex", JSONInfoAdapter.AttributeAdapter.setterCleaner(
                                        Integer.class,
                                        value -> setGenderField((Integer) value),
                                        () -> genderRadioGroup.clearCheck()));
        plantInfoAdapter.addAttribute("germination_date", JSONInfoAdapter.AttributeAdapter.setterCleaner(
                                        String.class,
                                        value -> germinationDateEdit.setText((String) value),
                                        () -> germinationDateEdit.getText().clear()));
        plantInfoAdapter.addAttribute("blooming_date", JSONInfoAdapter.AttributeAdapter.setterCleaner(
                                        String.class,
                                        value -> bloomingDateEdit.setText((String) value),
                                        () -> bloomingDateEdit.getText().clear()));
        plantInfoAdapter.addAttribute("yielding_date",JSONInfoAdapter.AttributeAdapter.setterCleaner(
                                        String.class,
                                        value -> yieldingDateEdit.setText((String) value),
                                        () -> yieldingDateEdit.getText().clear()));

        return view;
    }

    private void setVarietySpinner(int varietyId) {
        spinnerVarietyId = varietyId;
        SpinnerAdapter spinnerAdapter = varietySpinner.getAdapter();
        if (spinnerAdapter == null) return;

        for (int i = 0; i < spinnerAdapter.getCount(); i++) {
            VarietyItem item = (VarietyItem)spinnerAdapter.getItem(i);
            if (item.id() == varietyId) {
                varietySpinner.setSelection(i);
                return; // Exit the loop once the item is found
            }
        }
        // if not found, select "placeholder" (first item)
        varietySpinner.setSelection(0);
    }

    private void setGenderField(int index) {
        RadioButton radioBtn = (RadioButton) genderRadioGroup.getChildAt(index);
        if (radioBtn != null) {
            radioBtn.setChecked(true);
        } else {
            genderRadioGroup.clearCheck();
        }
    }

    // @Override
    // public void onVarietiesUpdated(Varieties update) {
    //     if (update == null) {
    //         varieties = new ArrayList<>();
    //         Log.i(TAG, "onVarietiesUpdated: None");
    //     } else {
    //         varieties = update.getAll().stream().map(
    //             variety -> new VarietyItem(variety.name(), variety.id())
    //         ).collect(Collectors.toCollection(ArrayList::new));
    //         Log.i(TAG, "onVarietiesUpdated: " + varieties);
    //     }
    //     updateSpinnerItems();
    // }

    private void updateSpinnerItems(List<VarietyItem> varieties) {
        if (varieties == null || varietySpinner == null) return;

        ArrayAdapter<VarietyItem> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, varieties);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        varietySpinner.setAdapter(adapter);
    }

    private void buttonUpdateInfoClicked() {
        VarietyItem variety = (VarietyItem) varietySpinner.getSelectedItem();
        int gender = genderRadioGroup.indexOfChild(
                        getActivity().findViewById(
                            genderRadioGroup.getCheckedRadioButtonId()));
        JSONObject json_data = new JSONObject();
        try {
            json_data.put("UUID", plantId.getText());
            json_data.put("variety", variety.id());
            json_data.put("sex", gender);
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

    private void buttonNewPlantClicked() {
        // the listener is "one-shot" and resets default listener
        mainActivity.setCustomNdefListener(new FormatTagNdefListener(mainActivity));
    }

    private void buttonDiscardUUIDClicked() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Discard UUID")
                .setMessage("Please confirm you want to discard UUID: " +
                            plantId.getText() + " from database ?")
                .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.dismiss();
                        discardUUID(plantId.getText().toString());
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.dismiss();
                    }
                });
        AlertDialog formatTagDialog = builder.create();
        formatTagDialog.show();
    }

    private void discardUUID(String uuid) {
        AppConfiguration config = AppConfiguration.instance();
        new SendToServerTask(new SendToServerTask.ReplyListener() {
            @Override
            public void onReplyFromServer(String data) {
                if (data.equals("{\"result\": \"OK\"}")) {
                    mainActivity.toastDisplay(TAG, "UUID discarded.", true);
                }
            }
            @Override
            public void onRequestFailure(int errorCode, String data) {
                if (errorCode == 404) {
                    onError("UUID not found.");
                    return;
                }
                onError("Error " + errorCode + " from server" +
                        ((data != null) ? ", " + data : ""));
            }
            @Override
            public void onError(String data) {
                mainActivity.displayError(TAG, "Error discarding UUID: " + data);
            }
        }).POST(config.getServerURL() + config.DISCARD_UUID_URL,
                "{\"uuid\" : \"" + uuid + "\"}");
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

    private class FormatTagNdefListener implements MainActivity.NdefTagListener {
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

                String old_uuid = FetchPlantInfo.getUUIDFromNdefMessage(cached, mainActivity.appName());
                Log.i(TAG, "Found old tag UUID to discard:" + old_uuid);
                discardUUID(old_uuid);
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

}
