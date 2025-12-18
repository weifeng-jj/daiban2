package com.example.reverseclock;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

import java.util.Calendar;

// 简易模拟钟表，包含秒针，每秒重绘
public class TickingClockView extends View {
    private final Paint circlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint hourPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint minutePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint secondPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint centerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint tickPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint numberPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final Runnable ticker = new Runnable() {
        @Override
        public void run() {
            invalidate();
            postDelayed(this, 1000);
        }
    };

    public TickingClockView(Context context) {
        super(context);
        init();
    }

    public TickingClockView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        circlePaint.setStyle(Paint.Style.STROKE);
        circlePaint.setStrokeWidth(6f);
        circlePaint.setColor(Color.parseColor("#6200EE"));

        tickPaint.setStyle(Paint.Style.STROKE);
        tickPaint.setStrokeWidth(3f);
        tickPaint.setColor(Color.parseColor("#6200EE"));

        hourPaint.setColor(Color.parseColor("#333333"));
        hourPaint.setStrokeWidth(10f);
        hourPaint.setStrokeCap(Paint.Cap.ROUND);

        minutePaint.setColor(Color.parseColor("#444444"));
        minutePaint.setStrokeWidth(7f);
        minutePaint.setStrokeCap(Paint.Cap.ROUND);

        secondPaint.setColor(Color.parseColor("#FF7A59"));
        secondPaint.setStrokeWidth(4f);
        secondPaint.setStrokeCap(Paint.Cap.ROUND);

        centerPaint.setStyle(Paint.Style.FILL);
        centerPaint.setColor(Color.parseColor("#FF7A59"));

        numberPaint.setColor(Color.parseColor("#333333"));
        numberPaint.setTextSize(40f);
        numberPaint.setTextAlign(Paint.Align.CENTER);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        removeCallbacks(ticker);
        post(ticker);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        removeCallbacks(ticker);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int w = getWidth();
        int h = getHeight();
        int size = Math.min(w, h);
        float radius = size / 2f * 0.9f;
        float cx = w / 2f;
        float cy = h / 2f;

        // 外圈
        canvas.drawCircle(cx, cy, radius, circlePaint);

        // 刻度
        for (int i = 0; i < 60; i++) {
            double angle = Math.toRadians(i * 6 - 90);
            float startR = radius * (i % 5 == 0 ? 0.82f : 0.88f);
            float sx = cx + (float) Math.cos(angle) * startR;
            float sy = cy + (float) Math.sin(angle) * startR;
            float ex = cx + (float) Math.cos(angle) * radius;
            float ey = cy + (float) Math.sin(angle) * radius;
            canvas.drawLine(sx, sy, ex, ey, tickPaint);
        }

        // 数字 1~12
        float numberRadius = radius * 0.7f;
        for (int i = 1; i <= 12; i++) {
            double angle = Math.toRadians(i * 30 - 90);
            float nx = cx + (float) Math.cos(angle) * numberRadius;
            float ny = cy + (float) Math.sin(angle) * numberRadius + (numberPaint.getTextSize() / 3f);
            canvas.drawText(String.valueOf(i), nx, ny, numberPaint);
        }

        Calendar c = Calendar.getInstance();
        int hour = c.get(Calendar.HOUR);
        int minute = c.get(Calendar.MINUTE);
        int second = c.get(Calendar.SECOND);

        // 角度计算
        float hourAngle = (hour + minute / 60f + second / 3600f) * 30f - 90f;
        float minuteAngle = (minute + second / 60f) * 6f - 90f;
        float secondAngle = second * 6f - 90f;

        // 时针
        drawHand(canvas, cx, cy, radius * 0.5f, hourAngle, hourPaint);
        // 分针
        drawHand(canvas, cx, cy, radius * 0.72f, minuteAngle, minutePaint);
        // 秒针
        drawHand(canvas, cx, cy, radius * 0.82f, secondAngle, secondPaint);

        // 中心点
        canvas.drawCircle(cx, cy, 10f, centerPaint);
    }

    private void drawHand(Canvas canvas, float cx, float cy, float length, float angleDeg, Paint paint) {
        double angle = Math.toRadians(angleDeg);
        float x = cx + (float) Math.cos(angle) * length;
        float y = cy + (float) Math.sin(angle) * length;
        canvas.drawLine(cx, cy, x, y, paint);
    }
}

