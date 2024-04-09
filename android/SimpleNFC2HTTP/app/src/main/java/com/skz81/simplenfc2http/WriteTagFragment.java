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
import android.widget.EditText;
import android.widget.DatePicker;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import java.io.IOException;
import java.util.UUID;
import java.util.Calendar;

import android.nfc.tech.Ndef;
import android.nfc.NdefRecord;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;

import com.skz81.simplenfc2http.MainActivity;


public class WriteTagFragment extends Fragment {
    protected class NdefWriteListener implements NdefTagCallback {
        private String appName = null;
        private WriteTagFragment parent;

        public NdefWriteListener(WriteTagFragment fragment, String appName) {
            this.parent = fragment;
            this.appName = appName;
        }
        @Override
        public void onNDEFDiscovered(Ndef ndef) {
            Log.i(TAG, "in Writer:onNDEFDiscovered, write tag info");
            Log.i(TAG, "UUID: " + parent.getNewUUID());
            Log.i(TAG, "AppName: " + appName);
            NdefMessage message = new NdefMessage(
                    NdefRecord.createTextRecord("", parent.getNewUUID()),
                    NdefRecord.createTextRecord("", appName)
            );
            try {
                ndef.connect();
                if (ndef.isConnected()) {
                    ndef.writeNdefMessage(message);
                }
                // check we're still connected
                if (!ndef.isConnected()) {
                    throw new IOException("NFC connection lost");
                }
            } catch (Exception e) {
                // Log error message
                Log.e(TAG, "Error writing NDEF messages: " + e.getMessage(), e);
                Toast.makeText(mainActivity, "Error writing tag...", Toast.LENGTH_LONG).show();
            } finally {
                newUUID = null;
                try {
                    ndef.close();
                } catch (Exception e) {
                    Log.w(TAG, "Error closing NDEF connection: " + e.getMessage(), e);
                }
            }
            if (scanTagDialog != null) {
                scanTagDialog.dismiss();
                scanTagDialog = null;
            }
            parent.stopScan();
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

    private static final String TAG = "AutoWatS-NFC-write";

    private EditText plantId;
    private Spinner varietySpinner;
    private EditText germinationDateEdit;
    private EditText bloomingDateEdit;
    private EditText yieldingDateEdit;
    private Button updateInfoButton;
    private Button scanTagButton;
    private Button newPlantButton;

    private MainActivity mainActivity;
    private Calendar calendar;
    private AlertDialog scanTagDialog = null;

    private String newUUID = null;

    private NdefReadListener ndefReader;
    private NdefWriteListener ndefWriter;

    public WriteTagFragment() {
    }

    private void addDateFieldListener(EditText textview) {
        textview.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDatePickerDialog(textview);
            }
        });
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_write_tag, container, false);

        plantId = view.findViewById(R.id.plantId);
        varietySpinner = view.findViewById(R.id.varietySpinner);
        germinationDateEdit = view.findViewById(R.id.germinationDateEdit);
        bloomingDateEdit = view.findViewById(R.id.bloomingDateEdit);
        yieldingDateEdit = view.findViewById(R.id.yieldingDateEdit);
        updateInfoButton = view.findViewById(R.id.updateInfoButton);
        scanTagButton = view.findViewById(R.id.scanTagButton);
        newPlantButton = view.findViewById(R.id.newPlantButton);

        calendar = Calendar.getInstance();
        mainActivity = (MainActivity) getActivity();

        addDateFieldListener(germinationDateEdit);
        addDateFieldListener(bloomingDateEdit);
        addDateFieldListener(yieldingDateEdit);

        ndefReader = new NdefReadListener(this);
        ndefWriter = new NdefWriteListener(this, mainActivity.appName());

        // Populate variety spinner with dummy values
        // TODO : use the HTTP to get the list with images (and info URL)
        String[] varieties = {
            "Rose Bouquet",
            "Lily Garden",
            "Sunflower Fields",
            "Orchid Paradise",
            "Daisy Meadow"};

        ArrayAdapter<CharSequence> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, varieties);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        varietySpinner.setAdapter(adapter);

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
                buttoUpdateInfoClicked();
            }
        });

        return view;
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

    void buttoUpdateInfoClicked() {
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
