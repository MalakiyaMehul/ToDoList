package com.example.todolist.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class DonutChartView extends View {

    public static class Slice {
        public String name;
        public int color;
        public float value;

        public Slice(String name, int color, float value) {
            this.name = name;
            this.color = color;
            this.value = value;
        }

        public Slice(int color, float value) {
            this("Category", color, value);
        }
    }

    private static final int[] PALETTE = new int[]{
            Color.parseColor("#10B981"),
            Color.parseColor("#3B82F6"),
            Color.parseColor("#F59E0B"),
            Color.parseColor("#EC4899"),
            Color.parseColor("#8B5CF6"),
            Color.parseColor("#06B6D4"),
            Color.parseColor("#F43F5E"),
            Color.parseColor("#14B8A6"),
            Color.parseColor("#A855F7"),
            Color.parseColor("#EAB308"),
            Color.parseColor("#6366F1")
    };

    public static int getCategoryColor(String categoryName) {
        if (categoryName == null || categoryName.trim().isEmpty()) {
            return PALETTE[0];
        }
        String cat = categoryName.trim().toLowerCase(Locale.ROOT);
        switch (cat) {
            case "work": return Color.parseColor("#10B981");
            case "personal": return Color.parseColor("#06B6D4");
            case "design": return Color.parseColor("#A855F7");
            case "health": return Color.parseColor("#F43F5E");
            case "education": return Color.parseColor("#F59E0B");
            case "finance": return Color.parseColor("#3B82F6");
            case "shopping": return Color.parseColor("#EC4899");
            case "fitness": return Color.parseColor("#EF4444");
            case "home": return Color.parseColor("#14B8A6");
            case "music": return Color.parseColor("#8B5CF6");
            case "camera": return Color.parseColor("#EAB308");
            default:
                int hash = Math.abs(cat.hashCode());
                return PALETTE[hash % PALETTE.length];
        }
    }

    private final List<Slice> slices = new ArrayList<>();
    private String centerCount = "0";
    private String centerLabel = "ALL TASKS";

    private Paint arcPaint;
    private Paint countPaint;
    private Paint labelPaint;
    private RectF arcBounds;
    private float strokeWidthPx;

    public DonutChartView(Context context) {
        super(context);
        init();
    }

    public DonutChartView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public DonutChartView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        strokeWidthPx = 20f * getResources().getDisplayMetrics().density;

        arcPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        arcPaint.setStyle(Paint.Style.STROKE);
        arcPaint.setStrokeWidth(strokeWidthPx);
        arcPaint.setStrokeCap(Paint.Cap.BUTT);

        countPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        countPaint.setColor(Color.WHITE);
        countPaint.setTextSize(24f * getResources().getDisplayMetrics().scaledDensity);
        countPaint.setTextAlign(Paint.Align.CENTER);
        countPaint.setFakeBoldText(true);

        labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        labelPaint.setColor(Color.parseColor("#94A3B8"));
        labelPaint.setTextSize(10f * getResources().getDisplayMetrics().scaledDensity);
        labelPaint.setTextAlign(Paint.Align.CENTER);
        labelPaint.setFakeBoldText(true);
        labelPaint.setLetterSpacing(0.08f);

        arcBounds = new RectF();
        centerCount = "0";
        centerLabel = "ALL TASKS";
    }

    public void loadDefaultSample() {
        slices.clear();
        centerCount = "0";
        centerLabel = "ALL TASKS";
        invalidate();
    }

    public void setData(List<Slice> newSlices, int totalCount) {
        slices.clear();
        if (newSlices != null) {
            slices.addAll(newSlices);
        }
        centerCount = String.valueOf(totalCount);
        centerLabel = "ALL TASKS";
        invalidate();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        float pad = strokeWidthPx / 2f + 4f * getResources().getDisplayMetrics().density;
        float size = Math.min(w, h);
        float left = (w - size) / 2f + pad;
        float top = (h - size) / 2f + pad;
        float right = (w + size) / 2f - pad;
        float bottom = (h + size) / 2f - pad;
        arcBounds.set(left, top, right, bottom);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float total = 0f;
        for (Slice s : slices) {
            total += s.value;
        }

        float currentAngle = -90f;
        float gapAngle = slices.size() > 1 ? 3f : 0f;

        if (total <= 0f) {
            arcPaint.setColor(Color.parseColor("#334155"));
            canvas.drawArc(arcBounds, 0, 360, false, arcPaint);
        } else {
            for (Slice slice : slices) {
                float sweep = (slice.value / total) * 360f;
                float drawSweep = Math.max(sweep - gapAngle, 1f);

                arcPaint.setColor(slice.color);
                canvas.drawArc(arcBounds, currentAngle + (gapAngle / 2f), drawSweep, false, arcPaint);
                currentAngle += sweep;
            }
        }

        // Draw center count and label
        float centerX = arcBounds.centerX();
        float centerY = arcBounds.centerY();

        Rect countBounds = new Rect();
        countPaint.getTextBounds(centerCount, 0, centerCount.length(), countBounds);

        Rect labelBounds = new Rect();
        labelPaint.getTextBounds(centerLabel, 0, centerLabel.length(), labelBounds);

        float spacing = 4f * getResources().getDisplayMetrics().density;
        float totalTextHeight = countBounds.height() + labelBounds.height() + spacing;

        float countY = centerY - (totalTextHeight / 2f) + countBounds.height();
        float labelY = countY + spacing + labelBounds.height();

        canvas.drawText(centerCount, centerX, countY, countPaint);
        canvas.drawText(centerLabel, centerX, labelY, labelPaint);
    }
}
