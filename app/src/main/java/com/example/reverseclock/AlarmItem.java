package com.example.reverseclock;

import org.json.JSONException;
import org.json.JSONObject;

// 简单的闹钟数据模型
public class AlarmItem {
    public int id; // 唯一requestCode
    public int hour;
    public int minute;
    public int taskType;
    public int shakeCount;
    public int alertMode;   // 0:铃声 1:震动 2:铃声+震动
    public int repeatType;  // 0:仅一次 1:每天 2:工作日 3:自定义
    public int repeatMask;  // 自定义周几 bit0=周日 ... bit6=周六
    public int mathTarget;  // 数学题目标数量
    public int puzzleMode;  // 3:3x3 4:4x4

    public AlarmItem(int id, int hour, int minute, int taskType, int shakeCount,
                     int alertMode, int repeatType, int repeatMask,
                     int mathTarget, int puzzleMode) {
        this.id = id;
        this.hour = hour;
        this.minute = minute;
        this.taskType = taskType;
        this.shakeCount = shakeCount;
        this.alertMode = alertMode;
        this.repeatType = repeatType;
        this.repeatMask = repeatMask;
        this.mathTarget = mathTarget;
        this.puzzleMode = puzzleMode;
    }

    // 序列化为JSON
    public JSONObject toJson() throws JSONException {
        JSONObject obj = new JSONObject();
        obj.put("id", id);
        obj.put("hour", hour);
        obj.put("minute", minute);
        obj.put("taskType", taskType);
        obj.put("shakeCount", shakeCount);
        obj.put("alertMode", alertMode);
        obj.put("repeatType", repeatType);
        obj.put("repeatMask", repeatMask);
        obj.put("mathTarget", mathTarget);
        obj.put("puzzleMode", puzzleMode);
        return obj;
    }

    // 反序列化
    public static AlarmItem fromJson(JSONObject obj) throws JSONException {
        return new AlarmItem(
                obj.getInt("id"),
                obj.getInt("hour"),
                obj.getInt("minute"),
                obj.getInt("taskType"),
                obj.optInt("shakeCount", 10),
                obj.optInt("alertMode", 0),
                obj.optInt("repeatType", 0),
                obj.optInt("repeatMask", 0),
                obj.optInt("mathTarget", 5),
                obj.optInt("puzzleMode", 4)
        );
    }
}

