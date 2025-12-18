package com.example.reverseclock;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

// 待办事项持久化存储
public class TodoStore {
    private static final String PREF_NAME = "todo_prefs";
    private static final String KEY_TODO_LIST = "todo_list";

    // 保存待办列表
    public static void save(Context context, List<TodoItem> list) {
        try {
            JSONArray arr = new JSONArray();
            for (TodoItem item : list) {
                arr.put(item.toJson());
            }
            SharedPreferences sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            sp.edit().putString(KEY_TODO_LIST, arr.toString()).apply();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 加载待办列表
    public static List<TodoItem> load(Context context) {
        List<TodoItem> list = new ArrayList<>();
        try {
            SharedPreferences sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            String json = sp.getString(KEY_TODO_LIST, "[]");
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.getJSONObject(i);
                list.add(TodoItem.fromJson(obj));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    // 清除今天之前的待办（可选，每天自动清理）
    public static void clearOldTodos(Context context) {
        // 获取今天0点的时间戳
        java.util.Calendar cal = java.util.Calendar.getInstance();
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0);
        cal.set(java.util.Calendar.MINUTE, 0);
        cal.set(java.util.Calendar.SECOND, 0);
        cal.set(java.util.Calendar.MILLISECOND, 0);
        long todayStart = cal.getTimeInMillis();

        List<TodoItem> list = load(context);
        List<TodoItem> todayList = new ArrayList<>();
        for (TodoItem item : list) {
            if (item.createTime >= todayStart) {
                todayList.add(item);
            }
        }
        save(context, todayList);
    }
}
