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

    public static int getCategoryColor(String categoryName) {
        if (categoryName == null) return Color.parseColor("#8B5CF6");
        switch (categoryName.trim()) {
            case "Work": return Color.parseColor("#8B5CF6");
            case "Design": return Color.parseColor("#C084FC");
            case "Personal": return Color.parseColor("#06B6D4");
            case "Health": return Color.parseColor("#10B981");
            case "Grocery": return Color.parseColor("#CCFF80");
            case "Sport": return Color.parseColor("#80FFFF");
            case "University": return Color.parseColor("#809CFF");
            case "Social": return Color.parseColor("#FF80EB");
            case "Music": return Color.parseColor("#FC80FF");
            case "Heart": return Color.parseColor("#80FFA3");
            case "Movie": return Color.parseColor("#80D1FF");
            case "Home": return Color.parseColor("#FFCC80");
            default:
                return Color.parseColor("#8B5CF6");
        }
    }

    private final List<Slice> slices = new ArrayList<>();
    private String centerCount = "48";
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

        loadDefaultSample();
    }

    public void loadDefaultSample() {
        slices.clear();
        slices.add(new Slice("Work", Color.parseColor("#8B5CF6"), 22));
        slices.add(new Slice("Design", Color.parseColor("#C084FC"), 12));
        slices.add(new Slice("Personal", Color.parseColor("#06B6D4"), 9));
        slices.add(new Slice("Health", Color.parseColor("#10B981"), 5));
        centerCount = "48";
        centerLabel = "ALL TASKS";
        invalidate();
    }

    public void setData(List<Slice> newSlices, int totalCount) {
        slices.clear();
        if (newSlices != null && !newSlices.isEmpty() && totalCount > 0) {
            slices.addAll(newSlices);
            centerCount = String.valueOf(totalCount);
        } else {
            loadDefaultSample();
            return;
        }
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

        if (total <= 0f) {
            total = 1f;
        }

        float currentAngle = -90f;
        float gapAngle = slices.size() > 1 ? 3f : 0f;

        for (Slice slice : slices) {
            float sweep = (slice.value / total) * 360f;
            float drawSweep = Math.max(sweep - gapAngle, 1f);

            arcPaint.setColor(slice.color);
            canvas.drawArc(arcBounds, currentAngle + (gapAngle / 2f), drawSweep, false, arcPaint);
            currentAngle += sweep;
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
