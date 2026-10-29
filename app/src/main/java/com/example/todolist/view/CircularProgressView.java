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

public class CircularProgressView extends View {

    private int progress = 67; // default matching the design
    private Paint trackPaint;
    private Paint progressPaint;
    private Paint percentPaint;
    private Paint labelPaint;
    private RectF arcBounds;
    private float strokeWidthPx;

    public CircularProgressView(Context context) {
        super(context);
        init();
    }

    public CircularProgressView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public CircularProgressView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        strokeWidthPx = 7f * getResources().getDisplayMetrics().density;

        trackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        trackPaint.setStyle(Paint.Style.STROKE);
        trackPaint.setStrokeWidth(strokeWidthPx);
        trackPaint.setColor(Color.parseColor("#2C2C2E"));

        progressPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        progressPaint.setStyle(Paint.Style.STROKE);
        progressPaint.setStrokeWidth(strokeWidthPx);
        progressPaint.setColor(Color.parseColor("#00D2A0"));
        progressPaint.setStrokeCap(Paint.Cap.ROUND);

        percentPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        percentPaint.setColor(Color.WHITE);
        percentPaint.setTextSize(15f * getResources().getDisplayMetrics().scaledDensity);
        percentPaint.setTextAlign(Paint.Align.CENTER);
        percentPaint.setFakeBoldText(true);

        labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        labelPaint.setColor(Color.parseColor("#9E9E9E"));
        labelPaint.setTextSize(9f * getResources().getDisplayMetrics().scaledDensity);
        labelPaint.setTextAlign(Paint.Align.CENTER);

        arcBounds = new RectF();
    }

    public void setProgress(int newProgress) {
        this.progress = Math.max(0, Math.min(100, newProgress));
        invalidate();
    }

    public int getProgress() {
        return progress;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        float pad = strokeWidthPx / 2f + 2f * getResources().getDisplayMetrics().density;
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

        // Background track circle
        canvas.drawOval(arcBounds, trackPaint);

        // Progress arc
        if (progress > 0) {
            float sweepAngle = (progress / 100f) * 360f;
            canvas.drawArc(arcBounds, -90f, sweepAngle, false, progressPaint);
        }

        // Center text
        float centerX = arcBounds.centerX();
        float centerY = arcBounds.centerY();

        String percentText = progress + "%";
        String labelText = "Complete";

        Rect percentBounds = new Rect();
        percentPaint.getTextBounds(percentText, 0, percentText.length(), percentBounds);

        Rect labelBounds = new Rect();
        labelPaint.getTextBounds(labelText, 0, labelText.length(), labelBounds);

        float spacing = 2f * getResources().getDisplayMetrics().density;
        float totalHeight = percentBounds.height() + labelBounds.height() + spacing;

        float percentY = centerY - (totalHeight / 2f) + percentBounds.height();
        float labelY = percentY + spacing + labelBounds.height();

        canvas.drawText(percentText, centerX, percentY, percentPaint);
        canvas.drawText(labelText, centerX, labelY, labelPaint);
    }
}

