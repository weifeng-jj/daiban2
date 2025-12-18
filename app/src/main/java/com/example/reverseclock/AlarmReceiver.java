package com.example.reverseclock;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Vibrator;
import android.util.Log;
import android.widget.Toast;

// 闹钟触发接收器：增加容错，避免闪退
public class AlarmReceiver extends BroadcastReceiver {
    private static Ringtone ringtone; // 闹钟铃声
    private static Vibrator vibrator; // 震动器

    @Override
    public void onReceive(Context context, Intent intent) {
        try {
            Log.d("AlarmDebug", "闹钟接收器触发！");
            int alertMode = intent.getIntExtra("alert_mode", 0);
            startAlert(context, alertMode);

            // 2. 获取任务类型（增加默认值+容错）
            int taskType = intent.getIntExtra(MainActivity.EXTRA_TASK, 0);
            int shakeCount = intent.getIntExtra(MainActivity.EXTRA_SHAKE_COUNT, 10);
            int mathTarget = intent.getIntExtra("math_target", 5);
            int puzzleMode = intent.getIntExtra("puzzle_mode", 4);
            int hour = intent.getIntExtra("hour", 0);
            int minute = intent.getIntExtra("minute", 0);
            int repeatType = intent.getIntExtra("repeat_type", 0);
            int repeatMask = intent.getIntExtra("repeat_mask", 0);
            int alarmId = intent.getIntExtra("alarm_id", -1);
            Log.d("AlarmDebug", "任务类型：" + taskType + "，摇晃次数：" + shakeCount);

            // 3. 跳转到对应页面（增加空指针容错）
            Intent targetIntent = null;
            switch (taskType) {
                case MainActivity.TASK_MATH:
                    targetIntent = new Intent(context, MathQuestionActivity.class);
                    targetIntent.putExtra("math_target", mathTarget);
                    break;
                case MainActivity.TASK_SHAKE:
                    targetIntent = new Intent(context, ShakeActivity.class);
                    targetIntent.putExtra(MainActivity.EXTRA_SHAKE_COUNT, shakeCount);
                    break;
                case MainActivity.TASK_PUZZLE:
                    targetIntent = new Intent(context, PuzzleActivity.class);
                    targetIntent.putExtra("puzzle_mode", puzzleMode);
                    break;
                default:
                    // 未知任务类型，默认跳数学题
                    targetIntent = new Intent(context, MathQuestionActivity.class);
            }

            if (targetIntent != null) {
                // 必须加FLAG_ACTIVITY_NEW_TASK + 容错
                targetIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                try {
                    context.startActivity(targetIntent);
                } catch (Exception e) {
                    Log.e("AlarmDebug", "启动页面失败：" + e.getMessage());
                    Toast.makeText(context, "启动关闹钟页面失败", Toast.LENGTH_LONG).show();
                }
            } else {
                Log.e("AlarmDebug", "目标页面Intent为空");
            }

            // 4. 根据重复规则安排下一次
            scheduleNext(context, alarmId, alertMode, taskType, shakeCount,
                    mathTarget, puzzleMode, hour, minute, repeatType, repeatMask);
        } catch (Exception e) {
            // 捕获所有异常，避免闪退
            Log.e("AlarmDebug", "闹钟触发崩溃：" + e.getMessage());
            e.printStackTrace();
            Toast.makeText(context, "闹钟触发失败：" + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void startAlert(Context context, int alertMode) {
        // 0:铃声 1:震动 2:铃声+震动
        if (alertMode == 0 || alertMode == 2) {
            playAlarmSound(context);
        }
        if (alertMode == 1 || alertMode == 2) {
            startVibrate(context);
        }
    }

    // 播放闹钟铃声（增加容错，避免铃声播放失败导致闪退）
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
            } else {
                Log.e("AlarmDebug", "找不到铃声文件");
            }
        } catch (Exception e) {
            Log.e("AlarmDebug", "播放铃声失败：" + e.getMessage());
            // 铃声播放失败不影响核心逻辑，不闪退
        }
    }

    private void startVibrate(Context context) {
        try {
            if (vibrator == null) {
                vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
            }
            if (vibrator != null) {
                long[] pattern = new long[]{0, 800, 400, 800};
                vibrator.vibrate(pattern, 0);
            }
        } catch (Exception e) {
            Log.e("AlarmDebug", "启动震动失败：" + e.getMessage());
        }
    }

    // 停止铃声（供其他页面调用）
    public static void stopAlarmSound() {
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

    // 根据重复规则安排下一次
    private void scheduleNext(Context context, int alarmId, int alertMode, int taskType,
                              int shakeCount, int mathTarget, int puzzleMode,
                              int hour, int minute, int repeatType, int repeatMask) {
        if (alarmId < 0) return;
        if (repeatType == 0) return; // 仅一次，不重复

        java.util.Calendar now = java.util.Calendar.getInstance();
        java.util.Calendar next = java.util.Calendar.getInstance();
        next.set(java.util.Calendar.HOUR_OF_DAY, hour);
        next.set(java.util.Calendar.MINUTE, minute);
        next.set(java.util.Calendar.SECOND, 0);
        next.set(java.util.Calendar.MILLISECOND, 0);

        if (!next.after(now)) {
            next.add(java.util.Calendar.DAY_OF_MONTH, 1);
        }

        // 按重复规则调整到下一次
        if (repeatType == 1) {
            // 每天：已经至少+0或+1天，直接用
        } else if (repeatType == 2) {
            // 工作日：循环找到下一个周一到周五
            while (true) {
                int dow = next.get(java.util.Calendar.DAY_OF_WEEK); // 1-7, 周日=1
                if (dow >= java.util.Calendar.MONDAY && dow <= java.util.Calendar.FRIDAY) {
                    break;
                }
                next.add(java.util.Calendar.DAY_OF_MONTH, 1);
            }
        } else if (repeatType == 3) {
            // 自定义：使用mask
            if (repeatMask == 0) return;
            while (true) {
                int dow = next.get(java.util.Calendar.DAY_OF_WEEK); // 1-7, 周日=1
                int idx = dow - 1; // 0-6
                if ((repeatMask & (1 << idx)) != 0 && next.after(now)) {
                    break;
                }
                next.add(java.util.Calendar.DAY_OF_MONTH, 1);
            }
        }

        android.app.AlarmManager alarmManager =
                (android.app.AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;

        Intent intent = new Intent(context, AlarmReceiver.class);
        intent.putExtra(MainActivity.EXTRA_TASK, taskType);
        intent.putExtra(MainActivity.EXTRA_SHAKE_COUNT, shakeCount);
        intent.putExtra("alarm_id", alarmId);
        intent.putExtra("alert_mode", alertMode);
        intent.putExtra("repeat_type", repeatType);
        intent.putExtra("repeat_mask", repeatMask);
        intent.putExtra("math_target", mathTarget);
        intent.putExtra("puzzle_mode", puzzleMode);
        intent.putExtra("hour", hour);
        intent.putExtra("minute", minute);

        int pendingFlags = android.app.PendingIntent.FLAG_UPDATE_CURRENT;
        android.app.PendingIntent pi = android.app.PendingIntent.getBroadcast(
                context, alarmId, intent, pendingFlags);

        long triggerAt = next.getTimeInMillis();
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            alarmManager.setExact(
                    android.app.AlarmManager.RTC_WAKEUP,
                    triggerAt,
                    pi
            );
        } else {
            alarmManager.set(
                    android.app.AlarmManager.RTC_WAKEUP,
                    triggerAt,
                    pi
            );
        }
    }
}