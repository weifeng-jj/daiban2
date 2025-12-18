package com.example.reverseclock;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

// 首页：显示当前时间的时钟+已设置闹钟列表+创建按钮
public class HomeActivity extends AppCompatActivity {
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
    private TextView tvNow;
    private LinearLayout llAlarmList;
    private List<AlarmItem> alarmList = new ArrayList<>();

    private final Runnable tickRunnable = new Runnable() {
        @Override
        public void run() {
            updateNow();
            handler.postDelayed(this, 1000);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        tvNow = findViewById(R.id.tvNow);
        llAlarmList = findViewById(R.id.llAlarmListHome);
        Button btnCreate = findViewById(R.id.btnCreateAlarm);

        btnCreate.setOnClickListener(v -> {
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadAlarms();
        renderAlarmList();
        handler.removeCallbacks(tickRunnable);
        handler.post(tickRunnable);
    }

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacks(tickRunnable);
    }

    private void updateNow() {
        Calendar c = Calendar.getInstance();
        tvNow.setText(timeFormat.format(c.getTime()));
    }

    private void loadAlarms() {
        alarmList = AlarmStore.load(this);
    }

    private void renderAlarmList() {
        if (llAlarmList == null) return;
        llAlarmList.removeAllViews();
        if (alarmList == null || alarmList.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText("暂无闹钟，点击下方按钮创建");
            empty.setTextColor(ContextCompat.getColor(this, R.color.gray));
            llAlarmList.addView(empty);
            return;
        }
        for (AlarmItem item : alarmList) {
            android.view.View row = getLayoutInflater().inflate(R.layout.item_alarm_row, llAlarmList, false);
            TextView tvInfo = row.findViewById(R.id.tvAlarmInfo);
            TextView tvTask = row.findViewById(R.id.tvAlarmTask);

            String time = String.format("%02d:%02d", item.hour, item.minute);
            tvInfo.setText(time);
            tvTask.setText(getTaskDesc(item));

            // 点击整行：跳转到 MainActivity 编辑该闹钟
            row.setOnClickListener(v -> {
                Intent intent = new Intent(this, MainActivity.class);
                intent.putExtra("edit_id", item.id);
                startActivity(intent);
            });

            llAlarmList.addView(row);
        }
    }

    private String getTaskDesc(AlarmItem item) {
        switch (item.taskType) {
            case MainActivity.TASK_MATH:
                return "数学题：连续答对 " + item.mathTarget + " 道";
            case MainActivity.TASK_SHAKE:
                return "摇晃手机：" + item.shakeCount + " 次";
            case MainActivity.TASK_PUZZLE:
                return "滑动拼图：" + (item.puzzleMode == 3 ? "3×3" : "4×4");
            default:
                return "完成任务后才能关闭闹钟";
        }
    }

    private void deleteAlarm(AlarmItem item) {
        try {
            AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
            if (alarmManager != null) {
                Intent intent = new Intent(this, AlarmReceiver.class);
                PendingIntent pi = PendingIntent.getBroadcast(
                        this,
                        item.id,
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT
                );
                alarmManager.cancel(pi);
            }
            alarmList.remove(item);
            AlarmStore.save(this, alarmList);
            renderAlarmList();
            Toast.makeText(this, "已删除闹钟", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "删除失败：" + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}

