package com.example.reverseclock;

import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.util.Locale;

/**
 * 计时工具页（合并秒表和计时器）
 */
public class TimeToolFragment extends Fragment {

    // === 秒表相关 ===
    private TextView tvStopwatchTime;
    private Button btnStopwatchStartPause;
    private Button btnStopwatchReset;
    private final Handler stopwatchHandler = new Handler(Looper.getMainLooper());
    private boolean stopwatchRunning = false;
    private long stopwatchBaseMs = 0;
    private long stopwatchLastStartMs = 0;

    private final Runnable stopwatchTick = new Runnable() {
        @Override
        public void run() {
            if (stopwatchRunning) {
                updateStopwatchDisplay();
                stopwatchHandler.postDelayed(this, 50);
            }
        }
    };

    // === 计时器相关 ===
    private EditText etTimerMin;
    private EditText etTimerSec;
    private TextView tvTimerRemain;
    private Button btnTimerStartPause;
    private Button btnTimerReset;
    private CountDownTimer timer;
    private boolean timerRunning = false;
    private long timerRemainMs = 0;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_timetool, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 初始化秒表控件
        tvStopwatchTime = view.findViewById(R.id.tvStopwatchTime);
        btnStopwatchStartPause = view.findViewById(R.id.btnStopwatchStartPause);
        btnStopwatchReset = view.findViewById(R.id.btnStopwatchReset);

        btnStopwatchStartPause.setOnClickListener(v -> toggleStopwatch());
        btnStopwatchReset.setOnClickListener(v -> resetStopwatch());

        // 初始化计时器控件
        etTimerMin = view.findViewById(R.id.etTimerMin);
        etTimerSec = view.findViewById(R.id.etTimerSec);
        tvTimerRemain = view.findViewById(R.id.tvTimerRemain);
        btnTimerStartPause = view.findViewById(R.id.btnTimerStartPause);
        btnTimerReset = view.findViewById(R.id.btnTimerReset);

        btnTimerStartPause.setOnClickListener(v -> toggleTimer());
        btnTimerReset.setOnClickListener(v -> resetTimer());

        updateStopwatchDisplay();
        updateTimerText(0);
    }

    @Override
    public void onPause() {
        super.onPause();
        stopwatchHandler.removeCallbacks(stopwatchTick);
        cancelTimer();
    }

    // ============== 秒表逻辑 ==============
    private void toggleStopwatch() {
        if (!stopwatchRunning) {
            stopwatchRunning = true;
            stopwatchLastStartMs = System.currentTimeMillis();
            btnStopwatchStartPause.setText("暂停");
            stopwatchHandler.post(stopwatchTick);
        } else {
            stopwatchRunning = false;
            stopwatchBaseMs += System.currentTimeMillis() - stopwatchLastStartMs;
            btnStopwatchStartPause.setText("开始");
            stopwatchHandler.removeCallbacks(stopwatchTick);
            updateStopwatchDisplay();
        }
    }

    private void resetStopwatch() {
        stopwatchRunning = false;
        stopwatchBaseMs = 0;
        stopwatchLastStartMs = 0;
        btnStopwatchStartPause.setText("开始");
        stopwatchHandler.removeCallbacks(stopwatchTick);
        updateStopwatchDisplay();
    }

    private void updateStopwatchDisplay() {
        long elapsed = stopwatchBaseMs;
        if (stopwatchRunning) {
            elapsed += System.currentTimeMillis() - stopwatchLastStartMs;
        }
        long minutes = elapsed / 60000;
        long seconds = (elapsed / 1000) % 60;
        long centis = (elapsed / 10) % 100;
        tvStopwatchTime.setText(String.format(Locale.getDefault(), "%02d:%02d.%02d", minutes, seconds, centis));
    }

    // ============== 计时器逻辑 ==============
    private void toggleTimer() {
        if (!timerRunning) {
            if (timerRemainMs <= 0) {
                timerRemainMs = parseTimerInputMs();
            }
            if (timerRemainMs <= 0) {
                Toast.makeText(requireContext(), "请输入有效的时间", Toast.LENGTH_SHORT).show();
                return;
            }
            startTimer(timerRemainMs);
        } else {
            pauseTimer();
        }
    }

    private long parseTimerInputMs() {
        int min = 0;
        int sec = 0;
        try {
            String m = etTimerMin.getText().toString().trim();
            String s = etTimerSec.getText().toString().trim();
            if (!m.isEmpty()) min = Integer.parseInt(m);
            if (!s.isEmpty()) sec = Integer.parseInt(s);
        } catch (Exception e) {
            return 0;
        }
        if (min < 0 || sec < 0) return 0;
        return (min * 60L + sec) * 1000L;
    }

    private void startTimer(long startMs) {
        cancelTimer();
        timerRunning = true;
        btnTimerStartPause.setText("暂停");
        timer = new CountDownTimer(startMs, 200) {
            @Override
            public void onTick(long millisUntilFinished) {
                timerRemainMs = millisUntilFinished;
                updateTimerText(timerRemainMs);
            }

            @Override
            public void onFinish() {
                timerRunning = false;
                timerRemainMs = 0;
                btnTimerStartPause.setText("开始");
                updateTimerText(0);
                Toast.makeText(requireContext(), "⏰ 计时结束！", Toast.LENGTH_SHORT).show();
            }
        };
        timer.start();
    }

    private void pauseTimer() {
        cancelTimer();
        timerRunning = false;
        btnTimerStartPause.setText("开始");
        updateTimerText(timerRemainMs);
    }

    private void resetTimer() {
        cancelTimer();
        timerRunning = false;
        timerRemainMs = 0;
        btnTimerStartPause.setText("开始");
        updateTimerText(0);
    }

    private void cancelTimer() {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }

    private void updateTimerText(long ms) {
        long totalSec = ms / 1000;
        long min = totalSec / 60;
        long sec = totalSec % 60;
        tvTimerRemain.setText(String.format(Locale.getDefault(), "%02d:%02d", min, sec));
    }
}

