package com.skz81.simplenfc2http;


import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.skz81.simplenfc2http.cultureProgram.*;

import java.util.ArrayList;
import java.util.List;

public class TestTabFragment  extends Fragment {
    private static final int FERTILIZER_COUNT = 3;

    private MainActivity mainActivity;

    private SeekBar daysPerRow;
    private CultureProgramTimeline timeline;
    private CultureProgramEditor editor;

    public TestTabFragment() {}

   @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mainActivity = (MainActivity) context;
    }

    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_test, container, false);
        daysPerRow = view.findViewById(R.id.days_per_row);
        timeline = view.findViewById(R.id.culture_timeline);
        // TODO : configurable in XML
        timeline.setDaysPerRow(28);

        // TODO : remove that sample code
        List<CulturePhase> phases = new ArrayList<CulturePhase>();
        phases.add(new CulturePhase("Vegetative", 20, new double[]{5.0, 2.5, 1.0}));
        phases.add(new CulturePhase("Grow",       30, new double[]{3.5, 4.5, 2.0}));
        phases.add(new CulturePhase("Bloom",      25, new double[]{1.0, 3.0, 5.0}));

        CultureProgram tomates = new CultureProgram("Tomates", phases);
        timeline.setProgram(tomates);
        // Plante au jour 37
        timeline.setCurrentDay(37);

        daysPerRow.setMax(tomates.getDurationDays());
        daysPerRow.setProgress(timeline.getDaysPerRow());

        editor = view.findViewById(R.id.culture_program_editor);

        editor.setFertilizerCount(FERTILIZER_COUNT);
        editor.setProgram(tomates);

        editor.setOnProgramChangedListener(program -> {
            timeline.setProgram(program);
            daysPerRow.setMax(program.getDurationDays());
        });

        daysPerRow.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(android.widget.SeekBar seekBar,
                                   int value, boolean fromUser) {
                if (fromUser) timeline.setDaysPerRow(value);
            }
            @Override
            public void onStartTrackingTouch(android.widget.SeekBar seekBar) {}
            @Override
            public void onStopTrackingTouch(android.widget.SeekBar seekBar) {}
        });

        return view;
    }

}
