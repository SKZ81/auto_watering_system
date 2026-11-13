
package com.skz81.simplenfc2http.cultureProgram;

import com.skz81.simplenfc2http.R;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import java.util.List;

/**
 * Read-only timeline displaying fertilizer dosages
 * throughout a culture program.
 *
 * Dosages are expressed in ml/L.
 * Days are numbered starting at 1.
 */
public class CultureProgramTimeline extends View {

    // -----------------------------------------------------------------
    // Configuration
    // -----------------------------------------------------------------

    private static final int[] FERTILIZER_COLORS = {
        Color.rgb(40, 121, 200),
        Color.rgb(36, 160, 120),
        Color.rgb(229, 164, 59),
        Color.rgb(183, 92, 173),
        Color.rgb(220, 91, 91),
        Color.rgb(90, 160, 180),
        Color.rgb(130, 145, 75),
        Color.rgb(130, 110, 190)
    };

    private static final int GRID_COLOR = Color.rgb(220, 220, 220);
    private static final int AXIS_COLOR = Color.rgb(100, 100, 100);
    private static final int PHASE_COLOR = Color.rgb(150, 150, 150);
    private static final int CURRENT_DAY_COLOR = Color.rgb(210, 55, 55);
    private static final int TEXT_COLOR = Color.rgb(55, 55, 55);

    private static final float ROW_HEIGHT_DP = 190f;
    private static final float ROW_GAP_DP = 22f;
    private static final float HEADER_HEIGHT_DP = 32f;
    private static final float LEGEND_HEIGHT_DP = 42f;

    private static final float AXIS_WIDTH_DP = 44f;
    private static final float RIGHT_PADDING_DP = 12f;
    private static final float TOP_PADDING_DP = 8f;
    private static final float BOTTOM_PADDING_DP = 30f;

    private static final float TEXT_SIZE_DP = 11f;
    private static final float PHASE_TEXT_SIZE_DP = 12f;
    private static final float LINE_WIDTH_DP = 2f;
    private static final float STAIRCASE_OFFSET_DP = 10f;

    // -----------------------------------------------------------------
    // Data
    // -----------------------------------------------------------------

    private CultureProgram program;

    /**
     * Number of days displayed on each row.
     * 0 means no wrapping.
     */
    private int daysPerRow = 0;

    /**
     * Current day in the culture program.
     * -1 means no current day marker.
     */
    private int currentDay = -1;

    private boolean showPhaseNames = true;
    private boolean showLegend = true;

    // -----------------------------------------------------------------
    // Paints
    // -----------------------------------------------------------------

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    // -----------------------------------------------------------------
    // Geometry
    // -----------------------------------------------------------------

    private float density;

    private float axisWidth;
    private float rightPadding;
    private float topPadding;
    private float bottomPadding;

    private float headerHeight;
    private float rowHeight;
    private float rowGap;
    private float legendHeight;

    private float phase_text_size;
    private float text_size;
    private float line_width;
    private float staircase_offset;

    // -----------------------------------------------------------------
    // Colors
    // -----------------------------------------------------------------

    private int grid_color;
    private int axis_color;
    private int phase_color;
    private int current_day_color;
    private int text_color;
    private int[] fertilizer_colors;

    // -----------------------------------------------------------------
    // Constructors
    // -----------------------------------------------------------------

    public CultureProgramTimeline(Context context) {
        super(context);
        init(null, 0);
    }

    public CultureProgramTimeline(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs, 0);
    }

    public CultureProgramTimeline(
            Context context,
            AttributeSet attrs,
            int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(attrs, defStyleAttr);
    }

    private void init(AttributeSet attrs, int defStyleAttr) {

        density = getResources().getDisplayMetrics().density;

        TypedArray a = getContext().obtainStyledAttributes(
            attrs,
            R.styleable.CultureProgramTimeline,
            defStyleAttr,
            0
        );

        try {
            // Dimensions (stored in pixels)

            axisWidth = a.getDimension(
                R.styleable.CultureProgramTimeline_AxisWidth,
                dp(AXIS_WIDTH_DP)
            );

            rightPadding = a.getDimension(
                R.styleable.CultureProgramTimeline_RightPadding,
                dp(RIGHT_PADDING_DP)
            );

            topPadding = a.getDimension(
                R.styleable.CultureProgramTimeline_TopPadding,
                dp(TOP_PADDING_DP)
            );

            bottomPadding = a.getDimension(
                R.styleable.CultureProgramTimeline_BottomPadding,
                dp(BOTTOM_PADDING_DP)
            );

            headerHeight = a.getDimension(
                R.styleable.CultureProgramTimeline_HeaderHeight,
                dp(HEADER_HEIGHT_DP)
            );

            rowHeight = a.getDimension(
                R.styleable.CultureProgramTimeline_RowHeight,
                dp(ROW_HEIGHT_DP)
            );

            rowGap = a.getDimension(
                R.styleable.CultureProgramTimeline_RowGap,
                dp(ROW_GAP_DP)
            );

            legendHeight = a.getDimension(
                R.styleable.CultureProgramTimeline_LegendHeight,
                dp(LEGEND_HEIGHT_DP)
            );

            phase_text_size = a.getDimension(
                R.styleable.CultureProgramTimeline_PhaseTextSize,
                dp(PHASE_TEXT_SIZE_DP)
            );

            text_size = a.getDimension(
                R.styleable.CultureProgramTimeline_TextSize,
                dp(TEXT_SIZE_DP)
            );

            line_width = a.getDimension(
                R.styleable.CultureProgramTimeline_LineWidth,
                dp(LINE_WIDTH_DP)
            );

            staircase_offset = a.getDimension(
                R.styleable.CultureProgramTimeline_StaircaseOffset,
                dp(STAIRCASE_OFFSET_DP)
            );
            // Colors

            grid_color = a.getColor(
                R.styleable.CultureProgramTimeline_GridColor,
                GRID_COLOR
            );

            axis_color = a.getColor(
                R.styleable.CultureProgramTimeline_AxisColor,
                AXIS_COLOR
            );

            phase_color = a.getColor(
                R.styleable.CultureProgramTimeline_PhaseColor,
                PHASE_COLOR
            );

            current_day_color = a.getColor(
                R.styleable.CultureProgramTimeline_CurrentDayColor,
                CURRENT_DAY_COLOR
            );

            text_color = a.getColor(
                R.styleable.CultureProgramTimeline_TextColor,
                TEXT_COLOR
            );

            // Fertilizer colors

            fertilizer_colors = FERTILIZER_COLORS.clone();

            int colorsArrayId = a.getResourceId(
                R.styleable.CultureProgramTimeline_FertilizerColors,
                0
            );

            if (colorsArrayId != 0) {
                TypedArray colors = getResources().obtainTypedArray(
                    colorsArrayId
                );

                try {
                    int count = colors.length();

                    if (count > 0) {
                        fertilizer_colors = new int[count];

                        for (int i = 0; i < count; i++) {
                            fertilizer_colors[i] = colors.getColor(
                                i,
                                FERTILIZER_COLORS[
                                    i % FERTILIZER_COLORS.length
                                ]
                            );
                        }
                    }
                } finally {
                    colors.recycle();
                }
            }

        } finally {
            a.recycle();
        }

        // Paint initialization

        textPaint.setColor(text_color);
        textPaint.setTextSize(text_size);
        textPaint.setTextAlign(Paint.Align.RIGHT);
    }

    private float dp(float value) {
        return value * density;
    }

    // -----------------------------------------------------------------
    // Public API
    // -----------------------------------------------------------------

    public void setProgram(CultureProgram program) {
        this.program = program;

        if (program != null &&
                currentDay > program.getDurationDays()) {
            currentDay = -1;
        }

        requestLayout();
        invalidate();
    }

    public CultureProgram getProgram() {
        return program;
    }

    public void setDaysPerRow(int daysPerRow) {
        if (daysPerRow < 0) {
            throw new IllegalArgumentException(
                "daysPerRow cannot be negative"
            );
        }

        this.daysPerRow = daysPerRow;

        requestLayout();
        invalidate();
    }

    public int getDaysPerRow() {
        return daysPerRow;
    }

    public void setCurrentDay(int currentDay) {
        if (currentDay < -1) {
            throw new IllegalArgumentException(
                "currentDay must be -1 or positive"
            );
        }

        if (currentDay > 0 &&
                program != null &&
                currentDay > program.getDurationDays()) {
            throw new IllegalArgumentException(
                "currentDay exceeds program duration"
            );
        }

        this.currentDay = currentDay;
        invalidate();
    }

    public int getCurrentDay() {
        return currentDay;
    }

    public void setShowPhaseNames(boolean show) {
        showPhaseNames = show;
        invalidate();
    }

    public void setShowLegend(boolean show) {
        showLegend = show;
        requestLayout();
        invalidate();
    }

    // -----------------------------------------------------------------
    // Layout
    // -----------------------------------------------------------------

    private int getRowCount() {
        if (program == null ||
                program.getDurationDays() <= 0) {
            return 0;
        }

        int totalDays = program.getDurationDays();

        if (daysPerRow <= 0) {
            return 1;
        }

        return (totalDays + daysPerRow - 1) / daysPerRow;
    }

    private int getDesiredHeight() {
        int rows = getRowCount();

        if (rows == 0) {
            return (int) dp(80);
        }

        float height = rows * rowHeight
                + (rows - 1) * rowGap
                + rows * headerHeight
                + topPadding
                + dp(8);

        if (showLegend) {
            height += legendHeight;
        }

        return (int) Math.ceil(height);
    }

    @Override
    protected void onMeasure(
            int widthMeasureSpec,
            int heightMeasureSpec) {

        int desiredWidth = getSuggestedMinimumWidth();
        int desiredHeight = getDesiredHeight();

        int width = resolveSize(
            desiredWidth,
            widthMeasureSpec
        );

        int height = resolveSize(
            desiredHeight,
            heightMeasureSpec
        );

        setMeasuredDimension(width, height);
    }

    // -----------------------------------------------------------------
    // Drawing
    // -----------------------------------------------------------------

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (program == null ||
                program.getPhases().isEmpty()) {
            drawEmptyMessage(canvas);
            return;
        }

        int totalDays = program.getDurationDays();

        if (totalDays <= 0) {
            drawEmptyMessage(canvas);
            return;
        }

        int fertilizerCount = getFertilizerCount();

        if (fertilizerCount < 0) {
            drawEmptyMessage(canvas);
            return;
        }

        float plotLeft = axisWidth;
        float plotRight = getWidth() - rightPadding;

        if (plotRight <= plotLeft) {
            return;
        }

        float plotWidth = plotRight - plotLeft;

        float maxDosage = (float) getMaxDosage();

        if (maxDosage <= 0) {
            maxDosage = 1;
        }

        float maxScale = niceMax(maxDosage);

        int rows = getRowCount();

        float yOffset = topPadding;

        for (int row = 0; row < rows; row++) {

            int rowStart = daysPerRow <= 0
                    ? 1
                    : row * daysPerRow + 1;

            int rowEnd = daysPerRow <= 0
                    ? totalDays
                    : Math.min(
                        totalDays,
                        rowStart + daysPerRow - 1
                    );

            drawRow(
                canvas,
                row,
                rows,
                rowStart,
                rowEnd,
                plotLeft,
                plotWidth,
                maxScale,
                yOffset,
                fertilizerCount
            );

            yOffset += headerHeight + rowHeight + rowGap;
        }

        if (showLegend) {
            drawLegend(canvas, fertilizerCount, yOffset);
        }
    }

    // -----------------------------------------------------------------
    // Row rendering
    // -----------------------------------------------------------------

    private void drawRow(
            Canvas canvas,
            int rowIndex,
            int rowCount,
            int rowStart,
            int rowEnd,
            float plotLeft,
            float plotWidth,
            float maxScale,
            float yOffset,
            int fertilizerCount) {

        float headerTop = yOffset;

        float graphTop = headerTop + headerHeight;
        float graphBottom = graphTop + rowHeight
                - bottomPadding;

        float graphHeight = graphBottom - graphTop;

        float rowWidth = rowEnd - rowStart + 1;

        // Phase names
        if (showPhaseNames) {
            drawPhaseNames(
                canvas,
                rowStart,
                rowEnd,
                plotLeft,
                plotWidth,
                headerTop
            );
        }

        // Y axis and horizontal grid
        drawYAxis(
            canvas,
            graphTop,
            graphBottom,
            graphHeight,
            maxScale
        );

        // X axis and day labels
        drawXAxis(
            canvas,
            rowStart,
            rowEnd,
            plotLeft,
            plotWidth,
            graphBottom
        );

        // Phase boundaries
        drawPhaseBoundaries(
            canvas,
            rowStart,
            rowEnd,
            plotLeft,
            plotWidth,
            graphTop,
            graphBottom
        );

        // Fertilizer dosage curves
        for (int fertilizer = 0;
                fertilizer < fertilizerCount;
                fertilizer++) {

            drawFertilizerCurve(
                canvas,
                fertilizer,
                rowStart,
                rowEnd,
                plotLeft,
                plotWidth,
                graphTop,
                graphHeight,
                maxScale
            );
        }

        // Current day marker
        if (currentDay >= rowStart &&
                currentDay <= rowEnd) {

            float x = dayToX(
                currentDay,
                rowStart,
                rowWidth,
                plotLeft,
                plotWidth
            );

            paint.setColor(current_day_color);
            paint.setStrokeWidth(dp(1.5f));
            paint.setStyle(Paint.Style.STROKE);

            canvas.drawLine(
                x,
                graphTop,
                x,
                graphBottom,
                paint
            );

            paint.setStyle(Paint.Style.FILL);

            textPaint.setColor(current_day_color);
            textPaint.setTextAlign(Paint.Align.CENTER);

            canvas.drawText(
                "J" + currentDay,
                x,
                graphTop - dp(5),
                textPaint
            );

            textPaint.setColor(text_color);
            textPaint.setTextAlign(Paint.Align.RIGHT);
        }
    }

    // -----------------------------------------------------------------
    // Phase labels
    // -----------------------------------------------------------------

    private void drawPhaseNames(
            Canvas canvas,
            int rowStart,
            int rowEnd,
            float plotLeft,
            float plotWidth,
            float headerTop) {

        float rowWidth = rowEnd - rowStart + 1;

        List<CulturePhase> phases = program.getPhases();

        int phaseStart = 1;

        textPaint.setColor(text_color);
        textPaint.setTextSize(phase_text_size);
        textPaint.setTextAlign(Paint.Align.CENTER);

        for (CulturePhase phase : phases) {

            int phaseEnd = phaseStart
                    + phase.getDurationDays() - 1;

            int visibleStart = Math.max(
                phaseStart,
                rowStart
            );

            int visibleEnd = Math.min(
                phaseEnd,
                rowEnd
            );

            if (visibleStart <= visibleEnd) {

                float x1 = dayToX(
                    visibleStart,
                    rowStart,
                    rowWidth,
                    plotLeft,
                    plotWidth
                );

                float x2 = dayToX(
                    visibleEnd + 1,
                    rowStart,
                    rowWidth,
                    plotLeft,
                    plotWidth
                );

                float center = (x1 + x2) / 2f;

                float availableWidth = x2 - x1 - dp(6);

                if (availableWidth > dp(20)) {
                    String label = phase.getName();

                    label = ellipsize(
                        label,
                        availableWidth,
                        textPaint
                    );

                    canvas.drawText(
                        label,
                        center,
                        headerTop + dp(20),
                        textPaint
                    );
                }
            }

            phaseStart = phaseEnd + 1;
        }

        textPaint.setTextSize(text_size);
        textPaint.setTextAlign(Paint.Align.RIGHT);
    }

    private String ellipsize(
            String text,
            float availableWidth,
            Paint textPaint) {

        if (textPaint.measureText(text) <= availableWidth) {
            return text;
        }

        String ellipsis = "...";

        while (!text.isEmpty() &&
                textPaint.measureText(text + ellipsis)
                    > availableWidth) {
            text = text.substring(0, text.length() - 1);
        }

        return text + ellipsis;
    }

    // -----------------------------------------------------------------
    // Axes
    // -----------------------------------------------------------------

    private void drawYAxis(
            Canvas canvas,
            float graphTop,
            float graphBottom,
            float graphHeight,
            float maxScale) {

        paint.setColor(grid_color);
        paint.setStrokeWidth(dp(0.7f));
        paint.setStyle(Paint.Style.STROKE);

        int divisions = 4;

        for (int i = 0; i <= divisions; i++) {

            float value = maxScale * i / divisions;

            float y = graphBottom
                    - graphHeight * i / divisions;

            canvas.drawLine(
                axisWidth,
                y,
                getWidth() - rightPadding,
                y,
                paint
            );

            textPaint.setColor(axis_color);
            textPaint.setTextAlign(Paint.Align.RIGHT);

            canvas.drawText(
                formatValue(value),
                axisWidth - dp(5),
                y + dp(4),
                textPaint
            );
        }

        paint.setStyle(Paint.Style.FILL);
    }

    private void drawXAxis(
            Canvas canvas,
            int rowStart,
            int rowEnd,
            float plotLeft,
            float plotWidth,
            float graphBottom) {

        paint.setColor(axis_color);
        paint.setStrokeWidth(dp(1));
        paint.setStyle(Paint.Style.STROKE);

        canvas.drawLine(
            plotLeft,
            graphBottom,
            plotLeft + plotWidth,
            graphBottom,
            paint
        );

        paint.setStyle(Paint.Style.FILL);

        int span = rowEnd - rowStart + 1;
        int step = getDayTickStep(span);

        textPaint.setColor(axis_color);
        textPaint.setTextAlign(Paint.Align.CENTER);

        for (int day = rowStart; day <= rowEnd; day++) {

            if ((day - 1) % step != 0 && day != rowEnd) {
                continue;
            }

            float x = dayToX(
                day,
                rowStart,
                span,
                plotLeft,
                plotWidth
            );

            canvas.drawText(
                String.valueOf(day),
                x,
                graphBottom + dp(18),
                textPaint
            );
        }

        textPaint.setTextAlign(Paint.Align.RIGHT);
    }

    private int getDayTickStep(int span) {
        if (span <= 7) return 1;
        if (span <= 14) return 2;
        if (span <= 35) return 5;
        if (span <= 70) return 10;
        if (span <= 140) return 20;
        if (span <= 350) return 50;
        return 100;
    }

    // -----------------------------------------------------------------
    // Phase boundaries
    // -----------------------------------------------------------------

    private void drawPhaseBoundaries(
            Canvas canvas,
            int rowStart,
            int rowEnd,
            float plotLeft,
            float plotWidth,
            float graphTop,
            float graphBottom) {

        float rowWidth = rowEnd - rowStart + 1;

        paint.setColor(phase_color);
        paint.setStrokeWidth(dp(1));
        paint.setStyle(Paint.Style.STROKE);

        int day = 1;

        for (CulturePhase phase : program.getPhases()) {

            day += phase.getDurationDays();

            if (day > rowStart && day <= rowEnd) {

                float x = dayToX(
                    day,
                    rowStart,
                    rowWidth,
                    plotLeft,
                    plotWidth
                );

                canvas.drawLine(
                    x,
                    graphTop,
                    x,
                    graphBottom,
                    paint
                );
            }
        }

        paint.setStyle(Paint.Style.FILL);
    }

    // -----------------------------------------------------------------
    // Fertilizer curves
    // -----------------------------------------------------------------

    // private void drawFertilizerCurve(
    //         Canvas canvas,
    //         int fertilizerIndex,
    //         int rowStart,
    //         int rowEnd,
    //         float plotLeft,
    //         float plotWidth,
    //         float graphTop,
    //         float graphHeight,
    //         float maxScale) {
    //
    //     List<CulturePhase> phases = program.getPhases();
    //
    //     float rowWidth = rowEnd - rowStart + 1;
    //
    //     Path path = new Path();
    //
    //     boolean started = false;
    //     float previousY = 0;
    //
    //     int phaseStart = 1;
    //
    //     for (CulturePhase phase : phases) {
    //
    //         int phaseEnd = phaseStart
    //                 + phase.getDurationDays() - 1;
    //
    //         int visibleStart = Math.max(phaseStart, rowStart);
    //         int visibleEnd = Math.min(phaseEnd, rowEnd);
    //
    //         if (visibleStart <= visibleEnd) {
    //
    //             double dosage = phase.getFertilizerDosage(
    //                 fertilizerIndex
    //             );
    //
    //             float y = dosageToY(
    //                 dosage, graphTop, graphHeight, maxScale
    //             );
    //
    //             float x1 = dayToX(
    //                 visibleStart, rowStart, rowWidth,
    //                 plotLeft, plotWidth
    //             );
    //
    //             float x2 = dayToX(
    //                 visibleEnd + 1, rowStart, rowWidth,
    //                 plotLeft, plotWidth
    //             );
    //
    //             if (!started) {
    //                 path.moveTo(x1, y);
    //                 started = true;
    //             } else {
    //                 path.lineTo(x1, previousY);
    //                 path.lineTo(x1, y);
    //             }
    //
    //             path.lineTo(x2, y);
    //             previousY = y;
    //         }
    //
    //         phaseStart = phaseEnd + 1;
    //     }
    //
    //     paint.setColor(getFertilizerColor(fertilizerIndex));
    //     paint.setStrokeWidth(line_width);
    //     paint.setStrokeCap(Paint.Cap.ROUND);
    //     paint.setStrokeJoin(Paint.Join.ROUND);
    //     paint.setStyle(Paint.Style.STROKE);
    //
    //     canvas.drawPath(path, paint);
    //
    //     paint.setStyle(Paint.Style.FILL);
    // }

    /* First version generated, To Be Tested */

    private void drawFertilizerCurve(
            Canvas canvas,
            int fertilizerIndex,
            int rowStart,
            int rowEnd,
            float plotLeft,
            float plotWidth,
            float graphTop,
            float graphHeight,
            float maxScale) {

        List<CulturePhase> phases = program.getPhases();

        float rowWidth = rowEnd - rowStart + 1;

        Path path = new Path();

        boolean started = false;

        int phaseStart = 1;

        for (CulturePhase phase : phases) {

            int phaseEnd = phaseStart
                    + phase.getDurationDays() - 1;

            int visibleStart = Math.max(
                phaseStart,
                rowStart
            );

            int visibleEnd = Math.min(
                phaseEnd,
                rowEnd
            );

            if (visibleStart <= visibleEnd) {

                double dosage = phase.getFertilizerDosage(
                    fertilizerIndex
                );

                float y = dosageToY(
                    dosage,
                    graphTop,
                    graphHeight,
                    maxScale
                );

                float x1 = dayToX(
                    visibleStart,
                    rowStart,
                    rowWidth,
                    plotLeft,
                    plotWidth
                );

                float x2 = dayToX(
                    visibleEnd + 1,
                    rowStart,
                    rowWidth,
                    plotLeft,
                    plotWidth
                );

                if (!started) {
                    path.moveTo(x1, y);
                    started = true;
                } else {
                    path.lineTo(x1 + staircase_offset / 2, y);
                }

                path.lineTo(x2 - staircase_offset / 2, y);
            }

            phaseStart = phaseEnd + 1;
        }

        paint.setColor(getFertilizerColor(fertilizerIndex));
        paint.setStrokeWidth(line_width);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeJoin(Paint.Join.ROUND);
        paint.setStyle(Paint.Style.STROKE);

        canvas.drawPath(path, paint);

        paint.setStyle(Paint.Style.FILL);
    }

    // -----------------------------------------------------------------
    // Legend
    // -----------------------------------------------------------------

    private void drawLegend(
            Canvas canvas,
            int fertilizerCount,
            float yOffset) {

        float x = axisWidth;
        float y = yOffset + dp(12);

        textPaint.setTextAlign(Paint.Align.LEFT);
        textPaint.setColor(text_color);

        for (int i = 0; i < fertilizerCount; i++) {

            paint.setColor(getFertilizerColor(i));

            canvas.drawCircle(
                x + dp(5),
                y,
                dp(4),
                paint
            );

            String label = "Fertilizer " + (i + 1);

            float textX = x + dp(14);

            canvas.drawText(
                label,
                textX,
                y + dp(4),
                textPaint
            );

            x = textX + textPaint.measureText(label)
                    + dp(18);

            if (x > getWidth() - dp(60)) {
                // Keep the rendering simple for now.
                break;
            }
        }

        textPaint.setTextAlign(Paint.Align.RIGHT);
    }

    // -----------------------------------------------------------------
    // Empty state
    // -----------------------------------------------------------------

    private void drawEmptyMessage(Canvas canvas) {

        textPaint.setColor(axis_color);
        textPaint.setTextAlign(Paint.Align.CENTER);

        canvas.drawText(
            "No program to display",
            getWidth() / 2f,
            getHeight() / 2f,
            textPaint
        );

        textPaint.setTextAlign(Paint.Align.RIGHT);
    }

    // -----------------------------------------------------------------
    // Data helpers
    // -----------------------------------------------------------------

    private int getFertilizerCount() {

        List<CulturePhase> phases = program.getPhases();

        if (phases.isEmpty()) {
            return 0;
        }

        int count = phases.get(0).getFertilizerDosages().length;

        for (CulturePhase phase : phases) {
            if (phase.getFertilizerDosages().length != count) {
                return -1;
            }
        }

        return count;
    }

    private double getMaxDosage() {

        double max = 0;

        for (CulturePhase phase : program.getPhases()) {

            for (double dosage : phase.getFertilizerDosages()) {
                max = Math.max(max, dosage);
            }
        }

        return max;
    }

    private float niceMax(double value) {

        double magnitude = Math.pow(
            10,
            Math.floor(Math.log10(value))
        );

        double normalized = value / magnitude;

        double rounded;

        if (normalized <= 1) {
            rounded = 1;
        } else if (normalized <= 2) {
            rounded = 2;
        } else if (normalized <= 5) {
            rounded = 5;
        } else {
            rounded = 10;
        }

        return (float) (rounded * magnitude);
    }

    private float dosageToY(
            double dosage,
            float graphTop,
            float graphHeight,
            float maxScale) {

        return graphTop + graphHeight
                - (float) (dosage / maxScale) * graphHeight;
    }

    private float dayToX(
            float day,
            float rowStart,
            float rowWidth,
            float plotLeft,
            float plotWidth) {

        return plotLeft
                + ((day - rowStart) / rowWidth) * plotWidth;
    }

    private int getFertilizerColor(int index) {
        return fertilizer_colors[
            index % fertilizer_colors.length
        ];
    }

    private String formatValue(float value) {
        if (value == (int) value) {
            return String.valueOf((int) value);
        }

        return String.format(
            java.util.Locale.getDefault(),
            "%.1f",
            value
        );
    }
}
