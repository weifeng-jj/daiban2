package com.example.reverseclock;

import android.os.Bundle;
import android.os.CountDownTimer;
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

// 导航栏-计时器页（倒计时）
public class TimerFragment extends Fragment {
    private EditText etMin;
    private EditText etSec;
    private TextView tvRemain;
    private Button btnStartPause;
    private Button btnReset;

    private CountDownTimer timer;
    private boolean running = false;
    private long remainMs = 0;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_timer, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        etMin = view.findViewById(R.id.etTimerMin);
        etSec = view.findViewById(R.id.etTimerSec);
        tvRemain = view.findViewById(R.id.tvTimerRemain);
        btnStartPause = view.findViewById(R.id.btnTimerStartPause);
        btnReset = view.findViewById(R.id.btnTimerReset);

        btnStartPause.setOnClickListener(v -> toggle());
        btnReset.setOnClickListener(v -> reset());

        updateRemainText(0);
    }

    @Override
    public void onPause() {
        super.onPause();
        cancelTimer();
    }

    private void toggle() {
        if (!running) {
            if (remainMs <= 0) {
                remainMs = parseInputMs();
            }
            if (remainMs <= 0) {
                Toast.makeText(requireContext(), "请输入有效的时间", Toast.LENGTH_SHORT).show();
                return;
            }
            startTimer(remainMs);
        } else {
            pauseTimer();
        }
    }

    private long parseInputMs() {
        int min = 0;
        int sec = 0;
        try {
            String m = etMin.getText().toString().trim();
            String s = etSec.getText().toString().trim();
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
        running = true;
        btnStartPause.setText("暂停");
        timer = new CountDownTimer(startMs, 200) {
            @Override
            public void onTick(long millisUntilFinished) {
                remainMs = millisUntilFinished;
                updateRemainText(remainMs);
            }

            @Override
            public void onFinish() {
                running = false;
                remainMs = 0;
                btnStartPause.setText("开始");
                updateRemainText(0);
                Toast.makeText(requireContext(), "计时结束！", Toast.LENGTH_SHORT).show();
            }
        };
        timer.start();
    }

    private void pauseTimer() {
        cancelTimer();
        running = false;
        btnStartPause.setText("开始");
        updateRemainText(remainMs);
    }

    private void reset() {
        cancelTimer();
        running = false;
        remainMs = 0;
        btnStartPause.setText("开始");
        updateRemainText(0);
    }

    private void cancelTimer() {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }

    private void updateRemainText(long ms) {
        long totalSec = ms / 1000;
        long min = totalSec / 60;
        long sec = totalSec % 60;
        tvRemain.setText(String.format(Locale.getDefault(), "%02d:%02d", min, sec));
    }
}





