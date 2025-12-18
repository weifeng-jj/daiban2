package com.example.reverseclock;

import org.json.JSONException;
import org.json.JSONObject;

// 待办事项数据模型
public class TodoItem {
    public int id;           // 唯一ID（也用作闹钟的requestCode）
    public int hour;         // 开始时间：小时
    public int minute;       // 开始时间：分钟
    public String content;   // 待办内容
    public boolean completed; // 是否已完成
    public long createTime;  // 创建时间

    public TodoItem(int id, int hour, int minute, String content) {
        this.id = id;
        this.hour = hour;
        this.minute = minute;
        this.content = content;
        this.completed = false;
        this.createTime = System.currentTimeMillis();
    }

    public TodoItem(int id, int hour, int minute, String content, boolean completed, long createTime) {
        this.id = id;
        this.hour = hour;
        this.minute = minute;
        this.content = content;
        this.completed = completed;
        this.createTime = createTime;
    }

    // 序列化为JSON
    public JSONObject toJson() throws JSONException {
        JSONObject obj = new JSONObject();
        obj.put("id", id);
        obj.put("hour", hour);
        obj.put("minute", minute);
        obj.put("content", content);
        obj.put("completed", completed);
        obj.put("createTime", createTime);
        return obj;
    }

    // 反序列化
    public static TodoItem fromJson(JSONObject obj) throws JSONException {
        return new TodoItem(
                obj.getInt("id"),
                obj.getInt("hour"),
                obj.getInt("minute"),
                obj.getString("content"),
                obj.optBoolean("completed", false),
                obj.optLong("createTime", System.currentTimeMillis())
        );
    }

    // 获取格式化的时间字符串
    public String getTimeString() {
        return String.format("%02d:%02d", hour, minute);
    }
}
