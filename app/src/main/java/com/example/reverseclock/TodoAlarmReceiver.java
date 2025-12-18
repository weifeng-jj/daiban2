package com.example.reverseclock;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Vibrator;
import android.util.Log;

// 待办事项提醒接收器 - 像真正的闹钟一样响铃
public class TodoAlarmReceiver extends BroadcastReceiver {
    private static Ringtone ringtone;
    private static Vibrator vibrator;

    @Override
    public void onReceive(Context context, Intent intent) {
        try {
            Log.d("TodoAlarm", "待办提醒触发！");
            
            int todoId = intent.getIntExtra("todo_id", -1);
            String content = intent.getStringExtra("todo_content");
            String time = intent.getStringExtra("todo_time");
            
            if (content == null || content.isEmpty()) {
                content = "待办事项提醒";
            }
            
            // 播放闹钟铃声（持续响）
            playAlarmSound(context);
            
            // 震动（持续）
            startVibrate(context);
            
            // 跳转到待办闹钟页面，显示待办内容
            Intent alarmIntent = new Intent(context, TodoAlarmActivity.class);
            alarmIntent.putExtra("todo_id", todoId);
            alarmIntent.putExtra("todo_content", content);
            alarmIntent.putExtra("todo_time", time);
            alarmIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            context.startActivity(alarmIntent);
            
        } catch (Exception e) {
            Log.e("TodoAlarm", "待办提醒触发失败：" + e.getMessage());
            e.printStackTrace();
        }
    }

    // 播放闹钟铃声
    private void playAlarmSound(Context context) {
        try {
            Uri alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
            if (alarmUri == null) {
                alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            }
            if (alarmUri != null) {
                ringtone = RingtoneManager.getRingtone(context, alarmUri);
                if (ringtone != null && !ringtone.isPlaying()) {
                    ringtone.play();
                }
            }
        } catch (Exception e) {
            Log.e("TodoAlarm", "播放铃声失败：" + e.getMessage());
        }
    }

    // 开始震动
    private void startVibrate(Context context) {
        try {
            if (vibrator == null) {
                vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
            }
            if (vibrator != null) {
                long[] pattern = new long[]{0, 800, 400, 800};
                vibrator.vibrate(pattern, 0); // 0表示循环震动
            }
        } catch (Exception e) {
            Log.e("TodoAlarm", "启动震动失败：" + e.getMessage());
        }
    }

    // 停止铃声和震动（供其他页面调用）
    public static void stopAlarm() {
        try {
            if (ringtone != null && ringtone.isPlaying()) {
                ringtone.stop();
            }
            if (vibrator != null) {
                vibrator.cancel();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

