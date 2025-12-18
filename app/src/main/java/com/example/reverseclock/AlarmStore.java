package com.example.reverseclock;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

// 轻量本地存储，使用SharedPreferences保存多个闹钟
public class AlarmStore {
    private static final String SP_NAME = "multi_alarm_store";
    private static final String KEY_ALARMS = "alarms";

    public static List<AlarmItem> load(Context context) {
        SharedPreferences sp = context.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE);
        String json = sp.getString(KEY_ALARMS, "[]");
        List<AlarmItem> list = new ArrayList<>();
        try {
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.getJSONObject(i);
                list.add(AlarmItem.fromJson(obj));
            }
        } catch (JSONException e) {
            // ignore and return empty
        }
        return list;
    }

    public static void save(Context context, List<AlarmItem> list) {
        JSONArray arr = new JSONArray();
        for (AlarmItem item : list) {
            try {
                arr.put(item.toJson());
            } catch (JSONException e) {
                // skip broken item
            }
        }
        SharedPreferences sp = context.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE);
        sp.edit().putString(KEY_ALARMS, arr.toString()).apply();
    }
}




