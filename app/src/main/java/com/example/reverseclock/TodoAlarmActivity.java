package com.example.reverseclock;

import android.os.Bundle;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

// 待办事项闹钟页面 - 显示待办内容，点击关闭闹钟
public class TodoAlarmActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // 让页面在锁屏上也能显示
        getWindow().addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD |
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON |
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
        );
        
        setContentView(R.layout.activity_todo_alarm);
        
        // 获取待办信息
        String content = getIntent().getStringExtra("todo_content");
        String time = getIntent().getStringExtra("todo_time");
        int todoId = getIntent().getIntExtra("todo_id", -1);
        
        if (content == null) content = "待办事项";
        if (time == null) time = "";
        
        // 显示待办内容
        TextView tvTodoContent = findViewById(R.id.tvTodoContent);
        TextView tvTodoTime = findViewById(R.id.tvTodoTime);
        Button btnDismiss = findViewById(R.id.btnDismiss);
        
        tvTodoContent.setText(content);
        tvTodoTime.setText("计划时间：" + time);
        
        // 关闭闹钟按钮
        btnDismiss.setOnClickListener(v -> {
            // 停止铃声和震动
            TodoAlarmReceiver.stopAlarm();
            
            // 不自动勾选完成，让用户自己决定
            // 关闭页面
            finish();
        });
    }

    @Override
    public void onBackPressed() {
        // 禁止返回键关闭页面，必须点击按钮
    }
}

