package com.example.reverseclock;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.util.Locale;

// 导航栏-秒表页
public class StopwatchFragment extends Fragment {
    private TextView tvTime;
    private Button btnStartPause;
    private Button btnReset;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean running = false;
    private long baseElapsedMs = 0; // 已累计
    private long lastStartMs = 0;   // 本次开始时间

    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            if (running) {
                updateDisplay();
                handler.postDelayed(this, 50);
            }
        }
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_stopwatch, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        tvTime = view.findViewById(R.id.tvStopwatchTime);
        btnStartPause = view.findViewById(R.id.btnStopwatchStartPause);
        btnReset = view.findViewById(R.id.btnStopwatchReset);

        btnStartPause.setOnClickListener(v -> toggle());
        btnReset.setOnClickListener(v -> reset());

        updateDisplay();
    }

    @Override
    public void onPause() {
        super.onPause();
        handler.removeCallbacks(tick);
    }

    private void toggle() {
        if (!running) {
            running = true;
            lastStartMs = System.currentTimeMillis();
            btnStartPause.setText("暂停");
            handler.post(tick);
        } else {
            running = false;
            baseElapsedMs += System.currentTimeMillis() - lastStartMs;
            btnStartPause.setText("开始");
            handler.removeCallbacks(tick);
            updateDisplay();
        }
    }

    private void reset() {
        running = false;
        baseElapsedMs = 0;
        lastStartMs = 0;
        btnStartPause.setText("开始");
        handler.removeCallbacks(tick);
        updateDisplay();
    }

    private void updateDisplay() {
        long elapsed = baseElapsedMs;
        if (running) {
            elapsed += System.currentTimeMillis() - lastStartMs;
        }
        long minutes = elapsed / 60000;
        long seconds = (elapsed / 1000) % 60;
        long centis = (elapsed / 10) % 100;
        tvTime.setText(String.format(Locale.getDefault(), "%02d:%02d.%02d", minutes, seconds, centis));
    }
}





