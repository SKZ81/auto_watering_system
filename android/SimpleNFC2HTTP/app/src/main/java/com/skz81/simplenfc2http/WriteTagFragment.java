package com.skz81.simplenfc2http;

import android.os.Bundle;
import android.app.DatePickerDialog;
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

import java.util.UUID;
import java.util.Calendar;

public class WriteTagFragment extends Fragment {
    private static final String TAG = "AutoWatS-NFC-write";

    private EditText plantId;
    private Spinner varietySpinner;
    private EditText germinationDateEdit;
    private EditText bloomingDateEdit;
    private EditText yieldingDateEdit;
    private Button generateIdButton;
    private Button writeTagButton;

    private Calendar calendar;

    public WriteTagFragment() {
        // Required empty public constructor
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
        generateIdButton = view.findViewById(R.id.generateIdButton);
        writeTagButton = view.findViewById(R.id.writeTagButton);

        calendar = Calendar.getInstance();

        addDateFieldListener(germinationDateEdit);
        addDateFieldListener(bloomingDateEdit);
        addDateFieldListener(yieldingDateEdit);

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
        generateIdButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Generate and set new UUID
                String newUuid = UUID.randomUUID().toString();
                plantId.setText(newUuid);
            }
        });

        // Write Tag button click listener
        writeTagButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Implement functionality to write tag here
                Toast.makeText(requireContext(), "Write Tag button clicked", Toast.LENGTH_SHORT).show();
            }
        });

        return view;
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
