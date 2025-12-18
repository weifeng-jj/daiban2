package com.example.reverseclock; // 包名与你的项目一致

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
import java.util.Calendar;

public class AlarmService extends Service {
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // 获取传递的参数
        int targetHour = intent.getIntExtra("hour", 0);
        int targetMinute = intent.getIntExtra("minute", 0);
        int targetTask = intent.getIntExtra("task", 0);

        // 设置闹钟时间
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, targetHour);
        calendar.set(Calendar.MINUTE, targetMinute);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        // 若时间已过，设为明天
        if (calendar.getTimeInMillis() < System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1);
        }

        // 初始化AlarmManager
        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        Intent broadcastIntent = new Intent(this, AlarmReceiver.class);
        broadcastIntent.putExtra("task", targetTask);

        // 创建PendingIntent
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                this,
                0,
                broadcastIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // 设置精确闹钟
        alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                calendar.getTimeInMillis(),
                pendingIntent
        );

        Log.d("AlarmService", "闹钟设置成功：" + targetHour + ":" + targetMinute + "，任务：" + targetTask);
        stopSelf();
        return super.onStartCommand(intent, flags, startId);
    }
}