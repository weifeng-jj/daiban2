package com.example.reverseclock;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.location.LocationListener;
import android.os.Build;
import android.os.Bundle;
import android.util.Log; // 补充Log的import
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    // 控件
    private TextView tvSelectedTime;
    private Spinner spinnerTask;
    private TextView tvTaskDesc;
    private LinearLayout llShakeCount;
    private LinearLayout llMathConfig;
    private LinearLayout llPuzzleConfig;
    private EditText etShakeCount;
    private EditText etMathCount;
    private Spinner spinnerPuzzleMode;
    private Spinner spinnerAlertMode;
    private Spinner spinnerRepeat;
    private LinearLayout llCustomRepeat;
    private CheckBox cbSun, cbMon, cbTue, cbWed, cbThu, cbFri, cbSat;
    private Button btnSetAlarm;
    private Button btnDeleteCurrentAlarm;
    private LinearLayout llAlarmList; // 动态展示已设置闹钟

    // 编辑模式相关
    private boolean isEditing = false;
    private int editingAlarmId = -1;

    // 数据
    private int selectedHour = -1;
    private int selectedMinute = -1;
    private int selectedTask = 0; // 0=数学题，1=摇晃，2=拼图
    private int shakeCount = 10; // 默认摇晃次数
    private int mathTarget = 5;  // 默认答对题数
    private int puzzleMode = 4;  // 3:3x3 4:4x4
    private int alertMode = 0;   // 0:铃声 1:震动 2:铃声+震动
    private int repeatType = 0;  // 0:仅一次 1:每天 2:工作日 3:自定义
    private int repeatMask = 0;  // 自定义周几 bit0=周日 ... bit6=周六
    private List<AlarmItem> alarmList = new ArrayList<>();


    // 常量（供Receiver使用）
    public static final String EXTRA_TASK = "task_type";
    public static final String EXTRA_SHAKE_COUNT = "shake_count";
    public static final int TASK_MATH = 0;
    public static final int TASK_SHAKE = 1;
    public static final int TASK_PUZZLE = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 初始化控件
        Button btnSelectTime = findViewById(R.id.btnSelectTime);
        tvSelectedTime = findViewById(R.id.tvSelectedTime);
        spinnerTask = findViewById(R.id.spinnerTask);
        tvTaskDesc = findViewById(R.id.tvTaskDesc);
        llShakeCount = findViewById(R.id.llShakeCount);
        llMathConfig = findViewById(R.id.llMathConfig);
        llPuzzleConfig = findViewById(R.id.llPuzzleConfig);
        etShakeCount = findViewById(R.id.etShakeCount);
        etMathCount = findViewById(R.id.etMathCount);
        spinnerPuzzleMode = findViewById(R.id.spinnerPuzzleMode);
        spinnerAlertMode = findViewById(R.id.spinnerAlertMode);
        spinnerRepeat = findViewById(R.id.spinnerRepeat);
        llCustomRepeat = findViewById(R.id.llCustomRepeat);
        cbSun = findViewById(R.id.cbSun);
        cbMon = findViewById(R.id.cbMon);
        cbTue = findViewById(R.id.cbTue);
        cbWed = findViewById(R.id.cbWed);
        cbThu = findViewById(R.id.cbThu);
        cbFri = findViewById(R.id.cbFri);
        cbSat = findViewById(R.id.cbSat);
        btnSetAlarm = findViewById(R.id.btnSetAlarm);
        btnDeleteCurrentAlarm = findViewById(R.id.btnDeleteCurrentAlarm);
        llAlarmList = findViewById(R.id.llAlarmList);

        // 初始化任务下拉框
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this, R.array.task_list, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerTask.setAdapter(adapter);

        // 提醒方式
        ArrayAdapter<CharSequence> alertAdapter = ArrayAdapter.createFromResource(
                this, R.array.alert_mode_list, android.R.layout.simple_spinner_item);
        alertAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerAlertMode.setAdapter(alertAdapter);
        spinnerAlertMode.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                alertMode = position; // 0/1/2
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) { }
        });

        // 重复规则
        ArrayAdapter<CharSequence> repeatAdapter = ArrayAdapter.createFromResource(
                this, R.array.repeat_mode_list, android.R.layout.simple_spinner_item);
        repeatAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerRepeat.setAdapter(repeatAdapter);
        spinnerRepeat.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                repeatType = position;
                llCustomRepeat.setVisibility(position == 3 ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) { }
        });

        // 拼图规模
        ArrayAdapter<CharSequence> puzzleAdapter = ArrayAdapter.createFromResource(
                this, R.array.puzzle_mode_list, android.R.layout.simple_spinner_item);
        puzzleAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerPuzzleMode.setAdapter(puzzleAdapter);
        spinnerPuzzleMode.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                // 0:3x3 1:4x4
                puzzleMode = (position == 0) ? 3 : 4;
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) { }
        });

        // 监听任务选择（仅选择摇晃时显示次数输入框）
        spinnerTask.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedTask = position;
                llShakeCount.setVisibility(position == TASK_SHAKE ? View.VISIBLE : View.GONE);
                llMathConfig.setVisibility(position == TASK_MATH ? View.VISIBLE : View.GONE);
                llPuzzleConfig.setVisibility(position == TASK_PUZZLE ? View.VISIBLE : View.GONE);
                updateTaskDesc(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // 选择时间
        btnSelectTime.setOnClickListener(v -> showTimePicker());

        // 设置闹钟
        btnSetAlarm.setOnClickListener(v -> setAlarm());

        // 删除当前正在编辑的闹钟（仅编辑模式下可见）
        btnDeleteCurrentAlarm.setOnClickListener(v -> deleteCurrentEditingAlarm());

        // 载入本地已存闹钟
        alarmList = AlarmStore.load(this);
        renderAlarmList();

        // 如果是从“编辑闹钟”入口进来，则根据ID填充表单
        handleEditIntent(getIntent());
    }

    // 弹出时间选择器
    private void showTimePicker() {
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);

        TimePickerDialog timeDialog = new TimePickerDialog(
                this,
                (view, hourOfDay, minute1) -> {
                    selectedHour = hourOfDay;
                    selectedMinute = minute1;
                    String time = String.format("%02d:%02d", hourOfDay, minute1);
                    tvSelectedTime.setText("已选时间：" + time);
                    tvSelectedTime.setTextColor(ContextCompat.getColor(this, R.color.black));
                },
                hour, minute, true
        );
        timeDialog.show();
    }

    // 设置系统闹钟（修复后 + 支持编辑）
    private void setAlarm() {
        // 校验
        if (selectedHour == -1 || selectedMinute == -1) {
            tvSelectedTime.setText("❌ 请先选择时间！");
            tvSelectedTime.setTextColor(ContextCompat.getColor(this, R.color.red));
            return;
        }
        // 校验摇晃次数
        if (selectedTask == TASK_SHAKE) {
            try {
                shakeCount = Integer.parseInt(etShakeCount.getText().toString());
                if (shakeCount <= 0) {
                    Toast.makeText(this, "摇晃次数必须大于0", Toast.LENGTH_SHORT).show();
                    return;
                }
            } catch (Exception e) {
                Toast.makeText(this, "请输入有效的摇晃次数", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        // 校验数学题数量
        if (selectedTask == TASK_MATH) {
            try {
                String mathText = etMathCount.getText().toString().trim();
                Log.d("AlarmDebug", "数学题数量输入框内容：" + mathText);
                if (mathText.isEmpty()) {
                    mathTarget = 5; // 如果为空，使用默认值
                } else {
                    mathTarget = Integer.parseInt(mathText);
                }
                Log.d("AlarmDebug", "解析后的mathTarget：" + mathTarget);
                if (mathTarget <= 0) {
                    Toast.makeText(this, "题目数量必须大于0", Toast.LENGTH_SHORT).show();
                    return;
                }
            } catch (Exception e) {
                Log.e("AlarmDebug", "解析数学题数量失败：" + e.getMessage());
                Toast.makeText(this, "请输入有效的题目数量", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        // 计算自定义重复mask
        if (repeatType == 3) {
            repeatMask = 0;
            if (cbSun.isChecked()) repeatMask |= 1 << 0;
            if (cbMon.isChecked()) repeatMask |= 1 << 1;
            if (cbTue.isChecked()) repeatMask |= 1 << 2;
            if (cbWed.isChecked()) repeatMask |= 1 << 3;
            if (cbThu.isChecked()) repeatMask |= 1 << 4;
            if (cbFri.isChecked()) repeatMask |= 1 << 5;
            if (cbSat.isChecked()) repeatMask |= 1 << 6;
            if (repeatMask == 0) {
                Toast.makeText(this, "请选择至少一天进行重复", Toast.LENGTH_SHORT).show();
                return;
            }
        } else {
            repeatMask = 0;
        }

        // 计算闹钟触发时间
        Calendar alarmCal = Calendar.getInstance();
        alarmCal.set(Calendar.HOUR_OF_DAY, selectedHour);
        alarmCal.set(Calendar.MINUTE, selectedMinute);
        alarmCal.set(Calendar.SECOND, 0);
        alarmCal.set(Calendar.MILLISECOND, 0);
        // 跨天处理
        if (alarmCal.getTimeInMillis() < System.currentTimeMillis()) {
            alarmCal.add(Calendar.DAY_OF_MONTH, 1);
        }
        Log.d("AlarmDebug", "闹钟触发时间：" + alarmCal.getTime().toString()); // 日志

        // 唯一ID（作为PendingIntent requestCode）
        int requestCode;
        if (isEditing && editingAlarmId != -1) {
            // 编辑已有闹钟：复用原ID
            requestCode = editingAlarmId;

            // 先取消原来的系统闹钟
            AlarmManager alarmManagerCancel = (AlarmManager) getSystemService(ALARM_SERVICE);
            if (alarmManagerCancel != null) {
                Intent cancelIntent = new Intent(this, AlarmReceiver.class);
                PendingIntent cancelPi = PendingIntent.getBroadcast(
                        this,
                        editingAlarmId,
                        cancelIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT
                );
                alarmManagerCancel.cancel(cancelPi);
            }

            // 从内存列表中移除旧记录
            for (int i = 0; i < alarmList.size(); i++) {
                if (alarmList.get(i).id == editingAlarmId) {
                    alarmList.remove(i);
                    break;
                }
            }
        } else {
            // 新建闹钟：生成新的ID
            requestCode = (int) (System.currentTimeMillis() & 0x7fffffff);
        }

        // 构建Intent
        Intent intent = new Intent(this, AlarmReceiver.class);
        intent.putExtra(EXTRA_TASK, selectedTask);
        intent.putExtra(EXTRA_SHAKE_COUNT, shakeCount);
        intent.putExtra("alarm_id", requestCode);
        intent.putExtra("alert_mode", alertMode);
        intent.putExtra("repeat_type", repeatType);
        intent.putExtra("repeat_mask", repeatMask);
        intent.putExtra("math_target", mathTarget);
        intent.putExtra("puzzle_mode", puzzleMode);
        intent.putExtra("hour", selectedHour);
        intent.putExtra("minute", selectedMinute);

        // 修复：将Build.VERSION_CODES.S改为具体版本号（28是Android 9，31是Android 12）
        int pendingFlags = PendingIntent.FLAG_UPDATE_CURRENT;
        // 如果你用的SDK版本<31，直接注释下面这行
        // if (Build.VERSION.SDK_INT >= 31) {
        //     pendingFlags |= PendingIntent.FLAG_IMMUTABLE;
        // }
        PendingIntent pendingIntent = null;
        try {
            pendingIntent = PendingIntent.getBroadcast(
                    this, requestCode, intent, pendingFlags
            );
        } catch (Exception e) {
            Log.e("AlarmDebug", "创建PendingIntent失败：" + e.getMessage());
            Toast.makeText(this, "创建闹钟失败：" + e.getMessage(), Toast.LENGTH_LONG).show();
            return;
        }

        // 设置闹钟
        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        if (alarmManager != null) {
            try {
                        scheduleAlarm(alarmManager, alarmCal, pendingIntent);

                // 提示成功
                String tip = String.format("✅ 闹钟设置成功！%02d:%02d 触发", selectedHour, selectedMinute);
                tvSelectedTime.setText(tip);
                tvSelectedTime.setTextColor(ContextCompat.getColor(this, R.color.green));
                tvTaskDesc.setText("已选择：" + getTaskDesc(selectedTask));

                        // 保存列表
                        alarmList.add(new AlarmItem(
                                requestCode,
                                selectedHour,
                                selectedMinute,
                                selectedTask,
                                shakeCount,
                                alertMode,
                                repeatType,
                                repeatMask,
                                mathTarget,
                                puzzleMode
                        ));
                        AlarmStore.save(this, alarmList);
                        renderAlarmList();

                        // 保存成功后，退出编辑模式，恢复按钮文字
                        if (isEditing) {
                            isEditing = false;
                            editingAlarmId = -1;
                            btnSetAlarm.setText("设置闹钟");
                        }
            } catch (Exception e) {
                Log.e("AlarmDebug", "设置闹钟失败：" + e.getMessage());
                Toast.makeText(this, "设置闹钟失败：" + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        } else {
            Toast.makeText(this, "获取闹钟服务失败", Toast.LENGTH_LONG).show();
        }
    }

    private void scheduleAlarm(AlarmManager alarmManager, Calendar alarmCal, PendingIntent pendingIntent) {
        // 适配低版本：仅用setExact
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    alarmCal.getTimeInMillis(),
                    pendingIntent
            );
        } else {
            alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    alarmCal.getTimeInMillis(),
                    pendingIntent
            );
        }
    }

    // 更新任务描述
    private void updateTaskDesc(int position) {
        if (tvTaskDesc == null) return;
        tvTaskDesc.setText(getTaskDesc(position));
    }

    // 根据任务类型返回说明文案
    private String getTaskDesc(int position) {
        switch (position) {
            case TASK_MATH:
                return "数学题：连续答对5道才能关闭铃声";
            case TASK_SHAKE:
                return "摇晃手机：自定义次数，摇够才停止闹钟";
            case TASK_PUZZLE:
                return "滑动拼图：还原图片即可关闭闹钟";
            default:
                return "完成任务后才能关闭闹钟";
        }
    }

    // 渲染闹钟列表
    private void renderAlarmList() {
        if (llAlarmList == null) return;
        llAlarmList.removeAllViews();
        if (alarmList == null || alarmList.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText("暂无闹钟，快去添加吧");
            empty.setTextColor(ContextCompat.getColor(this, R.color.gray));
            llAlarmList.addView(empty);
            return;
        }
        for (AlarmItem item : alarmList) {
            View row = getLayoutInflater().inflate(R.layout.item_alarm_row, llAlarmList, false);
            TextView tvInfo = row.findViewById(R.id.tvAlarmInfo);
            TextView tvTask = row.findViewById(R.id.tvAlarmTask);

            String time = String.format("%02d:%02d", item.hour, item.minute);
            tvInfo.setText(time);
            tvTask.setText(getTaskDesc(item.taskType));

            // 点击整行，进入编辑界面（在当前页直接填充表单）
            row.setOnClickListener(v -> startEditForItem(item));

            llAlarmList.addView(row);
        }
    }

    // 删除闹钟（取消系统闹钟 + 移除存储）
    private void deleteAlarm(AlarmItem item) {
        try {
            AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
            if (alarmManager != null) {
                Intent intent = new Intent(this, AlarmReceiver.class);
                PendingIntent pi = PendingIntent.getBroadcast(
                        this,
                        item.id,
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT
                );
                alarmManager.cancel(pi);
            }
            alarmList.remove(item);
            AlarmStore.save(this, alarmList);
            renderAlarmList();
            Toast.makeText(this, "已删除闹钟", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.e("AlarmDebug", "删除闹钟失败：" + e.getMessage());
        }
    }

    // 删除当前正在编辑的闹钟（从编辑界面底部按钮触发）
    private void deleteCurrentEditingAlarm() {
        if (!isEditing || editingAlarmId == -1) {
            Toast.makeText(this, "当前没有正在编辑的闹钟", Toast.LENGTH_SHORT).show();
            return;
        }

        AlarmItem target = null;
        for (AlarmItem item : alarmList) {
            if (item.id == editingAlarmId) {
                target = item;
                break;
            }
        }
        if (target == null) {
            Toast.makeText(this, "要删除的闹钟不存在", Toast.LENGTH_SHORT).show();
            return;
        }

        deleteAlarm(target);

        // 重置编辑状态 & 表单
        isEditing = false;
        editingAlarmId = -1;
        btnSetAlarm.setText("设置闹钟");
        btnDeleteCurrentAlarm.setVisibility(View.GONE);
        tvSelectedTime.setText("未选择时间");
        tvSelectedTime.setTextColor(ContextCompat.getColor(this, R.color.gray));
    }

    // 进入编辑模式：填充表单 + 记录当前正在编辑的闹钟ID
    private void startEditForItem(AlarmItem item) {
        isEditing = true;
        editingAlarmId = item.id;

        // 时间
        selectedHour = item.hour;
        selectedMinute = item.minute;
        String time = String.format("%02d:%02d", item.hour, item.minute);
        tvSelectedTime.setText("已选时间：" + time);
        tvSelectedTime.setTextColor(ContextCompat.getColor(this, R.color.black));

        // 任务类型
        selectedTask = item.taskType;
        spinnerTask.setSelection(item.taskType);

        // 摇晃次数 / 数学题数量 / 拼图模式
        shakeCount = item.shakeCount;
        mathTarget = item.mathTarget;
        puzzleMode = item.puzzleMode;
        etShakeCount.setText(String.valueOf(shakeCount));
        etMathCount.setText(String.valueOf(mathTarget));
        spinnerPuzzleMode.setSelection(item.puzzleMode == 3 ? 0 : 1);

        // 提醒方式
        alertMode = item.alertMode;
        spinnerAlertMode.setSelection(alertMode);

        // 重复规则
        repeatType = item.repeatType;
        repeatMask = item.repeatMask;
        spinnerRepeat.setSelection(repeatType);
        llCustomRepeat.setVisibility(repeatType == 3 ? View.VISIBLE : View.GONE);
        if (repeatType == 3) {
            cbSun.setChecked((repeatMask & (1 << 0)) != 0);
            cbMon.setChecked((repeatMask & (1 << 1)) != 0);
            cbTue.setChecked((repeatMask & (1 << 2)) != 0);
            cbWed.setChecked((repeatMask & (1 << 3)) != 0);
            cbThu.setChecked((repeatMask & (1 << 4)) != 0);
            cbFri.setChecked((repeatMask & (1 << 5)) != 0);
            cbSat.setChecked((repeatMask & (1 << 6)) != 0);
        } else {
            cbSun.setChecked(false);
            cbMon.setChecked(false);
            cbTue.setChecked(false);
            cbWed.setChecked(false);
            cbThu.setChecked(false);
            cbFri.setChecked(false);
            cbSat.setChecked(false);
        }

        // 更新任务描述 + 按钮文字
        updateTaskDesc(selectedTask);
        btnSetAlarm.setText("保存修改");
        btnDeleteCurrentAlarm.setVisibility(View.VISIBLE);

        Toast.makeText(this, "正在编辑闹钟 " + time, Toast.LENGTH_SHORT).show();
    }

    // 处理从外部（首页/闹钟Fragment）传入的“编辑某个闹钟”的意图
    private void handleEditIntent(Intent intent) {
        if (intent == null) return;
        int editId = intent.getIntExtra("edit_id", -1);
        if (editId == -1) return;

        for (AlarmItem item : alarmList) {
            if (item.id == editId) {
                startEditForItem(item);
                break;
            }
        }
    }

}