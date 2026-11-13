
package com.skz81.simplenfc2http.cultureProgram;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.Locale;

public class CultureProgramEditor extends LinearLayout {

    private static final int DEFAULT_PHASE_DURATION = 1;

    private static final int COLOR_BACKGROUND = Color.rgb(248, 248, 248);
    private static final int COLOR_BORDER = Color.rgb(210, 210, 210);
    private static final int COLOR_TEXT_SECONDARY = Color.rgb(100, 100, 100);

    private static final int MARGIN_DP = 8;
    private static final int PADDING_DP = 12;
    private static final int FIELD_SPACING_DP = 6;

    private CultureProgram program;
    private int fertilizerCount = -1;

    private CultureProgramTimeline timeline;
    private LinearLayout phasesContainer;
    private Button addPhaseButton;

    private OnProgramChangedListener listener;

    public interface OnProgramChangedListener {
        void onProgramChanged(CultureProgram program);
    }

    public CultureProgramEditor(Context context) {
        super(context);
        init();
    }

    public CultureProgramEditor(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public CultureProgramEditor(
            Context context,
            AttributeSet attrs,
            int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        setOrientation(VERTICAL);

        timeline = new CultureProgramTimeline(getContext());
        timeline.setCurrentDay(-1);

        addView(timeline, new LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT
        ));

        phasesContainer = new LinearLayout(getContext());
        phasesContainer.setOrientation(VERTICAL);

        addView(phasesContainer, new LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT
        ));

        addPhaseButton = new Button(getContext());
        addPhaseButton.setText("Add phase");
        addPhaseButton.setOnClickListener(v -> addPhase());

        addView(addPhaseButton, new LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT
        ));
    }

    // ---------------------------------------------------------------------
    // Public API
    // ---------------------------------------------------------------------

    public void setProgram(CultureProgram program) {
        if (program == null) {
            throw new IllegalArgumentException("Program cannot be null");
        }

        this.program = program;

        if (fertilizerCount < 0 && !program.getPhases().isEmpty()) {
            fertilizerCount =
                    program.getPhases().get(0).getFertilizerDosages().length;
        }

        validateProgram();

        timeline.setProgram(program);
        rebuildPhaseEditors();
    }

    public CultureProgram getProgram() {
        return program;
    }

    /**
     * Must be called before adding phases to an empty program.
     * If the program already contains phases, their dosage arrays
     * must have exactly this size.
     */
    public void setFertilizerCount(int count) {
        if (count < 0) {
            throw new IllegalArgumentException(
                    "Fertilizer count cannot be negative"
            );
        }

        fertilizerCount = count;

        if (program != null) {
            validateProgram();
            rebuildPhaseEditors();
        }
    }

    public int getFertilizerCount() {
        return fertilizerCount;
    }

    public void setOnProgramChangedListener(
            OnProgramChangedListener listener) {
        this.listener = listener;
    }

    public void setDaysPerRow(int daysPerRow) {
        timeline.setDaysPerRow(daysPerRow);
    }

    // ---------------------------------------------------------------------
    // Program operations
    // ---------------------------------------------------------------------

    public void addPhase() {
        ensureProgram();

        program.addPhase(new CulturePhase(
                "",
                DEFAULT_PHASE_DURATION,
                new double[fertilizerCount]
        ));

        onProgramChanged();
        rebuildPhaseEditors();
    }

    private void removePhase(int index) {
        program.removePhase(index);

        onProgramChanged();
        rebuildPhaseEditors();
    }

    private void movePhase(int fromIndex, int toIndex) {
        program.movePhase(fromIndex, toIndex);

        onProgramChanged();
        rebuildPhaseEditors();
    }

    private void ensureProgram() {
        if (program == null) {
            program = new CultureProgram();
            timeline.setProgram(program);
        }

        if (fertilizerCount < 0) {
            throw new IllegalStateException(
                    "Set fertilizerCount before adding phases"
            );
        }
    }

    private void validateProgram() {
        if (fertilizerCount < 0) {
            return;
        }

        for (CulturePhase phase : program.getPhases()) {
            if (phase.getFertilizerDosages().length != fertilizerCount) {
                throw new IllegalArgumentException(
                        "Every phase must contain exactly "
                                + fertilizerCount + " fertilizer dosages"
                );
            }
        }
    }

    private void onProgramChanged() {
        timeline.setProgram(program);

        if (listener != null) {
            listener.onProgramChanged(program);
        }
    }

    // ---------------------------------------------------------------------
    // Phase editor construction
    // ---------------------------------------------------------------------

    private void rebuildPhaseEditors() {
        phasesContainer.removeAllViews();

        if (program == null) {
            return;
        }

        for (int i = 0; i < program.getPhases().size(); i++) {
            phasesContainer.addView(createPhaseEditor(i));
        }
    }

    private View createPhaseEditor(int phaseIndex) {
        CulturePhase phase = program.getPhases().get(phaseIndex);

        LinearLayout card = new LinearLayout(getContext());
        card.setOrientation(VERTICAL);
        card.setPadding(
                dp(PADDING_DP),
                dp(PADDING_DP),
                dp(PADDING_DP),
                dp(PADDING_DP)
        );

        // GradientDrawable background = new GradientDrawable();
        // background.setColor(COLOR_BACKGROUND);
        // background.setCornerRadius(dp(8));
        // background.setStroke(dp(1), COLOR_BORDER);
        // card.setBackground(background);

        LayoutParams cardParams = new LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT
        );
        cardParams.setMargins(
                dp(MARGIN_DP),
                dp(MARGIN_DP),
                dp(MARGIN_DP),
                dp(MARGIN_DP)
        );
        card.setLayoutParams(cardParams);

        // Phase title and day range
        TextView summary = new TextView(getContext());
        summary.setText(getPhaseSummary(phaseIndex));
        summary.setTextSize(16);
        summary.setTypeface(null, Typeface.BOLD);
        card.addView(summary);

        // Phase name
        card.addView(createFieldLabel("Name"));

        EditText nameInput = createTextInput(
                phase.getName(),
                InputType.TYPE_CLASS_TEXT
        );
        nameInput.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start,
                                      int before, int count) {
                phase.setName(s.toString());
                onProgramChanged();
            }
        });
        card.addView(nameInput);

        // Duration
        card.addView(createFieldLabel("Duration (days)"));

        EditText durationInput = createTextInput(
                String.valueOf(phase.getDurationDays()),
                InputType.TYPE_CLASS_NUMBER
        );

        durationInput.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start,
                                      int before, int count) {
                try {
                    int duration = Integer.parseInt(s.toString());

                    if (duration <= 0) {
                        return;
                    }

                    phase.setDurationDays(duration);
                    updatePhaseSummaries();
                    onProgramChanged();

                } catch (NumberFormatException ignored) {
                    // The field may temporarily be empty while editing.
                }
            }
        });
        card.addView(durationInput);

        // Fertilizer dosages
        for (int fertilizerIndex = 0;
             fertilizerIndex < fertilizerCount;
             fertilizerIndex++) {

            final int index = fertilizerIndex;

            card.addView(createFieldLabel(
                    String.format(
                            Locale.getDefault(),
                            "Fertilizer %d (ml/L)",
                            index + 1
                    )
            ));

            EditText dosageInput = createTextInput(
                    formatDosage(phase.getFertilizerDosage(index)),
                    InputType.TYPE_CLASS_NUMBER
                            | InputType.TYPE_NUMBER_FLAG_DECIMAL
            );

            dosageInput.addTextChangedListener(new SimpleTextWatcher() {
                @Override
                public void onTextChanged(CharSequence s, int start,
                                          int before, int count) {
                    try {
                        double dosage = Double.parseDouble(
                                s.toString().replace(',', '.')
                        );

                        if (Double.isNaN(dosage)
                            || Double.isInfinite(dosage)
                            || dosage < 0) {
                            return;
                        }

                        phase.setFertilizerDosage(index, dosage);
                        onProgramChanged();

                    } catch (NumberFormatException ignored) {
                        // The field may temporarily be empty while editing.
                    }
                }
            });

            card.addView(dosageInput);
        }

        // Phase actions
        LinearLayout actions = new LinearLayout(getContext());
        actions.setOrientation(HORIZONTAL);
        actions.setGravity(Gravity.END);

        Button upButton = createActionButton("↑");
        upButton.setEnabled(phaseIndex > 0);
        upButton.setOnClickListener(v ->
                movePhase(phaseIndex, phaseIndex - 1)
        );
        actions.addView(upButton);

        Button downButton = createActionButton("↓");
        downButton.setEnabled(
                phaseIndex < program.getPhases().size() - 1
        );
        downButton.setOnClickListener(v ->
                movePhase(phaseIndex, phaseIndex + 1)
        );
        actions.addView(downButton);

        Button deleteButton = createActionButton("Remove");
        deleteButton.setOnClickListener(v -> removePhase(phaseIndex));
        actions.addView(deleteButton);

        card.addView(actions);

        return card;
    }

    // ---------------------------------------------------------------------
    // Phase summaries
    // ---------------------------------------------------------------------

    private String getPhaseSummary(int phaseIndex) {
        CulturePhase phase = program.getPhases().get(phaseIndex);

        int startDay = program.getPhaseStartDay(phaseIndex);
        int endDay = program.getPhaseEndDay(phaseIndex);

        return String.format(
                Locale.getDefault(),
                "Phase %d — %s (Day %d to %d — %d days)",
                phaseIndex + 1,
                phase.getName(),
                startDay,
                endDay,
                phase.getDurationDays()
        );
    }

    private void updatePhaseSummaries() {
        for (int i = 0; i < phasesContainer.getChildCount(); i++) {
            View card = phasesContainer.getChildAt(i);

            if (card instanceof LinearLayout) {
                LinearLayout layout = (LinearLayout) card;

                if (layout.getChildCount() > 0
                        && layout.getChildAt(0) instanceof TextView) {
                    ((TextView) layout.getChildAt(0))
                            .setText(getPhaseSummary(i));
                }
            }
        }
    }

    // ---------------------------------------------------------------------
    // UI helpers
    // ---------------------------------------------------------------------

    private TextView createFieldLabel(String text) {
        TextView label = new TextView(getContext());
        label.setText(text);
        label.setTextSize(13);
        label.setTextColor(COLOR_TEXT_SECONDARY);

        LayoutParams params = new LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT
        );
        params.topMargin = dp(FIELD_SPACING_DP);
        label.setLayoutParams(params);

        return label;
    }

    private EditText createTextInput(String value, int inputType) {
        EditText input = new EditText(getContext());
        input.setSingleLine(true);
        input.setText(value);
        input.setInputType(inputType);
        input.setTextSize(15);
        input.setPadding(
                dp(8), dp(4), dp(8), dp(4)
        );

        input.setLayoutParams(new LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT
        ));

        return input;
    }

    private Button createActionButton(String text) {
        Button button = new Button(getContext());
        button.setText(text);
        button.setTextSize(12);
        button.setMinWidth(0);
        button.setMinimumWidth(0);
        return button;
    }

    private String formatDosage(double dosage) {
        return String.format(Locale.getDefault(), "%s",
                dosage == (long) dosage
                        ? String.valueOf((long) dosage)
                        : String.valueOf(dosage)
        );
    }

    private int dp(float value) {
        return (int) (value * getResources()
                .getDisplayMetrics().density + 0.5f);
    }

    private abstract static class SimpleTextWatcher
            implements TextWatcher {

        @Override
        public void beforeTextChanged(
                CharSequence s, int start, int count, int after) {
        }

        @Override
        public void afterTextChanged(Editable s) {
        }
    }
}
