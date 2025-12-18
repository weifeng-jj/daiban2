package com.example.reverseclock;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * 倒数日本地存储工具类
 */
public class CountdownStore {
    private static final String PREFS_NAME = "countdown_prefs";
    private static final String KEY_DATA = "countdown_list";

    public static void save(Context context, List<CountdownItem> list) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        try {
            JSONArray arr = new JSONArray();
            for (CountdownItem item : list) {
                JSONObject obj = new JSONObject();
                obj.put("id", item.id);
                obj.put("name", item.name);
                obj.put("year", item.year);
                obj.put("month", item.month);
                obj.put("day", item.day);
                obj.put("createTime", item.createTime);
                arr.put(obj);
            }
            prefs.edit().putString(KEY_DATA, arr.toString()).apply();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static List<CountdownItem> load(Context context) {
        List<CountdownItem> list = new ArrayList<>();
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String json = prefs.getString(KEY_DATA, "[]");
        try {
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.getJSONObject(i);
                CountdownItem item = new CountdownItem();
                item.id = obj.optInt("id");
                item.name = obj.optString("name", "");
                item.year = obj.optInt("year");
                item.month = obj.optInt("month");
                item.day = obj.optInt("day");
                item.createTime = obj.optLong("createTime");
                list.add(item);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }
}

