package com.example.reverseclock;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.app.AlertDialog;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

// 导航栏-闹钟页：复用原启动页内容
public class AlarmFragment extends Fragment {
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
    private TextView tvNow;
    private TextView tvWeatherLocation;
    private LinearLayout llAlarmList;
    private List<AlarmItem> alarmList = new ArrayList<>();

    private final Runnable tickRunnable = new Runnable() {
        @Override
        public void run() {
            updateNow();
            handler.postDelayed(this, 1000);
        }
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_alarm, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        tvNow = view.findViewById(R.id.tvNow);
        llAlarmList = view.findViewById(R.id.llAlarmListHome);
        tvWeatherLocation = view.findViewById(R.id.tvWeatherLocation);
        
        Button btnCreate = view.findViewById(R.id.btnCreateAlarm);
        btnCreate.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), MainActivity.class);
            startActivity(intent);
        });
        
        // 点击地区名称，弹出选择器
        tvWeatherLocation.setOnClickListener(v -> showCityPicker());
        
        // 加载已保存的城市名称
        loadSavedCity();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadAlarms();
        renderAlarmList();
        handler.removeCallbacks(tickRunnable);
        handler.post(tickRunnable);
    }

    @Override
    public void onPause() {
        super.onPause();
        handler.removeCallbacks(tickRunnable);
    }

    private void updateNow() {
        if (tvNow == null) return;
        Calendar c = Calendar.getInstance();
        tvNow.setText(timeFormat.format(c.getTime()));
    }

    private void loadAlarms() {
        alarmList = AlarmStore.load(requireContext());
    }

    private void renderAlarmList() {
        if (llAlarmList == null) return;
        llAlarmList.removeAllViews();
        if (alarmList == null || alarmList.isEmpty()) {
            TextView empty = new TextView(requireContext());
            empty.setText("暂无闹钟，点击下方按钮创建");
            empty.setTextColor(ContextCompat.getColor(requireContext(), R.color.gray));
            llAlarmList.addView(empty);
            return;
        }
        for (AlarmItem item : alarmList) {
            View row = getLayoutInflater().inflate(R.layout.item_alarm_row, llAlarmList, false);
            TextView tvInfo = row.findViewById(R.id.tvAlarmInfo);
            TextView tvTask = row.findViewById(R.id.tvAlarmTask);

            String time = String.format("%02d:%02d", item.hour, item.minute);
            tvInfo.setText(time);
            tvTask.setText(getTaskDesc(item));

            // 点击整行：跳转到 MainActivity 编辑该闹钟
            row.setOnClickListener(v -> {
                Intent intent = new Intent(requireContext(), MainActivity.class);
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
            AlarmManager alarmManager = (AlarmManager) requireContext().getSystemService(android.content.Context.ALARM_SERVICE);
            if (alarmManager != null) {
                Intent intent = new Intent(requireContext(), AlarmReceiver.class);
                PendingIntent pi = PendingIntent.getBroadcast(
                        requireContext(),
                        item.id,
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT
                );
                alarmManager.cancel(pi);
            }
            alarmList.remove(item);
            AlarmStore.save(requireContext(), alarmList);
            renderAlarmList();
            Toast.makeText(requireContext(), "已删除闹钟", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(requireContext(), "删除失败：" + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    // 显示城市选择器
    private void showCityPicker() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_city_picker, null);
        builder.setView(dialogView);
        
        EditText etSearch = dialogView.findViewById(R.id.etSearchCity);
        Button btnSearch = dialogView.findViewById(R.id.btnSearchCity);
        ListView listView = dialogView.findViewById(R.id.listCities);
        TextView tvHint = dialogView.findViewById(R.id.tvCityHint);
        
        // 初始化空列表
        List<String> cityList = new ArrayList<>();
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), 
                android.R.layout.simple_list_item_1, cityList);
        listView.setAdapter(adapter);
        
        AlertDialog dialog = builder.create();
        
        // 点击城市项
        listView.setOnItemClickListener((parent, view, position, id) -> {
            String selectedCity = cityList.get(position);
            selectCity(selectedCity);
            dialog.dismiss();
        });
        
        // 搜索按钮点击
        btnSearch.setOnClickListener(v -> {
            String query = etSearch.getText().toString().trim();
            if (!query.isEmpty()) {
                tvHint.setText("搜索中...");
                searchCity(query, cityList, adapter, tvHint);
            } else {
                Toast.makeText(requireContext(), "请输入城市名称", Toast.LENGTH_SHORT).show();
            }
        });
        
        // 键盘搜索按钮
        etSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                btnSearch.performClick();
                return true;
            }
            return false;
        });
        
        dialog.show();
    }
    
    // 搜索城市（调用API）
    private void searchCity(String query, List<String> cityList, ArrayAdapter<String> adapter, TextView tvHint) {
        new Thread(() -> {
            try {
                String cityUrl = "https://nx7bygjncn.re.qweatherapi.com/geo/v2/city/lookup?location=" 
                        + java.net.URLEncoder.encode(query, "UTF-8") + "&key=850ffe6f1c094b9d9ea5d082fe0be2c4";
                
                String resp = WeatherHelper.doGet(cityUrl);
                if (resp == null) {
                    requireActivity().runOnUiThread(() -> {
                        tvHint.setText("搜索失败，请检查网络");
                        Toast.makeText(requireContext(), "搜索失败，请检查网络", Toast.LENGTH_SHORT).show();
                    });
                    return;
                }
                
                org.json.JSONObject json = new org.json.JSONObject(resp);
                String code = json.optString("code", "");
                if (!"200".equals(code)) {
                    requireActivity().runOnUiThread(() -> {
                        tvHint.setText("搜索失败");
                        Toast.makeText(requireContext(), "搜索失败", Toast.LENGTH_SHORT).show();
                    });
                    return;
                }
                
                org.json.JSONArray locationArray = json.optJSONArray("location");
                if (locationArray == null || locationArray.length() == 0) {
                    requireActivity().runOnUiThread(() -> {
                        tvHint.setText("未找到该城市，请尝试其他名称");
                        cityList.clear();
                        adapter.notifyDataSetChanged();
                    });
                    return;
                }
                
                // 更新列表
                requireActivity().runOnUiThread(() -> {
                    cityList.clear();
                    try {
                        for (int i = 0; i < locationArray.length(); i++) {
                            org.json.JSONObject loc = locationArray.getJSONObject(i);
                            String name = loc.optString("name", "");
                            String adm2 = loc.optString("adm2", "");
                            String adm1 = loc.optString("adm1", "");
                            // 格式：区县 · 市 · 省
                            String displayName = name;
                            if (!adm2.isEmpty() && !adm2.equals(name)) {
                                displayName += " · " + adm2;
                            }
                            if (!adm1.isEmpty()) {
                                displayName += " · " + adm1;
                            }
                            cityList.add(displayName);
                        }
                        adapter.notifyDataSetChanged();
                        tvHint.setText("找到 " + cityList.size() + " 个结果，点击选择");
                    } catch (Exception e) {
                        tvHint.setText("解析结果失败");
                        e.printStackTrace();
                    }
                });
            } catch (Exception e) {
                requireActivity().runOnUiThread(() -> {
                    tvHint.setText("搜索失败：" + e.getMessage());
                    Toast.makeText(requireContext(), "搜索失败：" + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }
    
    // 选择城市
    private void selectCity(String cityDisplayName) {
        // 提取城市名称（去掉省市后缀）
        String cityName = cityDisplayName.split(" · ")[0];
        
        // 保存并更新UI
        saveCity(cityName);
        tvWeatherLocation.setText(cityName);
        Toast.makeText(requireContext(), "已设置：" + cityName, Toast.LENGTH_SHORT).show();
    }

    // 保存城市名称到SharedPreferences
    private void saveCity(String cityName) {
        android.content.SharedPreferences sp = requireContext().getSharedPreferences("weather_prefs", android.content.Context.MODE_PRIVATE);
        sp.edit().putString("city_name", cityName).apply();
    }

    // 加载已保存的城市名称
    private void loadSavedCity() {
        android.content.SharedPreferences sp = requireContext().getSharedPreferences("weather_prefs", android.content.Context.MODE_PRIVATE);
        String savedCity = sp.getString("city_name", "");
        if (!savedCity.isEmpty()) {
            tvWeatherLocation.setText(savedCity);
        } else {
            tvWeatherLocation.setText("天气");
        }
    }
}





