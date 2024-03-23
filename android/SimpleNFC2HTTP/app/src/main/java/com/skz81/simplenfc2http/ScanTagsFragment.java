package com.skz81.simplenfc2http;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class ScanTagsFragment extends Fragment {

    private ImageView varietyIcon;
    private TextView varietyName;
    private TextView plantId;
    private TextView germinationDate;
    private TextView bloomingDate;
    private TextView yieldingDate;

    public ScanTagsFragment() {
        // Required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_scan_tags, container, false);

        varietyIcon = view.findViewById(R.id.varietyIcon);
        varietyName = view.findViewById(R.id.varietyName);
        plantId = view.findViewById(R.id.plantId);
        germinationDate = view.findViewById(R.id.germinationDate);
        bloomingDate = view.findViewById(R.id.bloomingDate);
        yieldingDate = view.findViewById(R.id.yieldingDate);

        // Set content for the labels
        varietyName.setText("Variety");
        plantId.setText("Tag ID : uuid");
        germinationDate.setText("Germination : date");
        bloomingDate.setText("Blooming : date");
        yieldingDate.setText("Yielding : date");

        // Set image resource
        // varietyIcon.setImageResource(R.drawable.your_image); // Replace 'your_image' with your image resource

        return view;
    }
}
