package com.example.reverseclock;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

// 待办事项Fragment
public class TodoFragment extends Fragment {
    private LinearLayout llTodoList;
    private TextView tvDate;
    private Button btnAddTodo;
    private List<TodoItem> todoList = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_todo, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        llTodoList = view.findViewById(R.id.llTodoList);
        tvDate = view.findViewById(R.id.tvDate);
        btnAddTodo = view.findViewById(R.id.btnAddTodo);
        
        // 显示今天日期
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy年MM月dd日 EEEE", Locale.CHINESE);
        tvDate.setText(sdf.format(Calendar.getInstance().getTime()));
        
        // 添加按钮点击
        btnAddTodo.setOnClickListener(v -> addNewTodoItem());
        
        // 加载待办列表
        loadTodos();
        
        // 如果列表为空，默认添加6行空白待办
        if (todoList.isEmpty()) {
            for (int i = 0; i < 6; i++) {
                int defaultHour = 8 + i * 2; // 默认时间：08:00, 10:00, 12:00, 14:00, 16:00, 18:00
                if (defaultHour > 23) defaultHour = 23;
                TodoItem item = new TodoItem(generateId(), defaultHour, 0, "");
                todoList.add(item);
            }
            saveTodos();
        }
        
        renderTodoList();
    }

    @Override
    public void onResume() {
        super.onResume();
        // 刷新列表
        loadTodos();
        renderTodoList();
    }

    // 加载待办
    private void loadTodos() {
        todoList = TodoStore.load(requireContext());
    }

    // 保存待办
    private void saveTodos() {
        TodoStore.save(requireContext(), todoList);
    }

    // 生成唯一ID
    private int generateId() {
        return (int) (System.currentTimeMillis() & 0x7fffffff);
    }

    // 添加新待办项
    private void addNewTodoItem() {
        Calendar now = Calendar.getInstance();
        TodoItem item = new TodoItem(generateId(), now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE), "");
        todoList.add(item);
        saveTodos();
        renderTodoList();
        Toast.makeText(requireContext(), "已添加新待办", Toast.LENGTH_SHORT).show();
    }

    // 渲染待办列表
    private void renderTodoList() {
        if (llTodoList == null) return;
        llTodoList.removeAllViews();
        
        for (int i = 0; i < todoList.size(); i++) {
            final int index = i;
            final TodoItem item = todoList.get(i);
            
            View row = getLayoutInflater().inflate(R.layout.item_todo_row, llTodoList, false);
            
            CheckBox cbCompleted = row.findViewById(R.id.cbCompleted);
            TextView tvTime = row.findViewById(R.id.tvTime);
            EditText etContent = row.findViewById(R.id.etContent);
            ImageButton btnDelete = row.findViewById(R.id.btnDelete);
            
            // 设置数据
            cbCompleted.setChecked(item.completed);
            tvTime.setText(item.getTimeString());
            etContent.setText(item.content);
            
            // 如果已完成，添加删除线效果
            if (item.completed) {
                etContent.setPaintFlags(etContent.getPaintFlags() | android.graphics.Paint.STRIKE_THRU_TEXT_FLAG);
                etContent.setAlpha(0.5f);
            } else {
                etContent.setPaintFlags(etContent.getPaintFlags() & ~android.graphics.Paint.STRIKE_THRU_TEXT_FLAG);
                etContent.setAlpha(1f);
            }
            
            // 完成状态改变
            cbCompleted.setOnCheckedChangeListener((buttonView, isChecked) -> {
                item.completed = isChecked;
                saveTodos();
                
                // 更新UI
                if (isChecked) {
                    etContent.setPaintFlags(etContent.getPaintFlags() | android.graphics.Paint.STRIKE_THRU_TEXT_FLAG);
                    etContent.setAlpha(0.5f);
                    // 取消闹钟
                    cancelTodoAlarm(item);
                } else {
                    etContent.setPaintFlags(etContent.getPaintFlags() & ~android.graphics.Paint.STRIKE_THRU_TEXT_FLAG);
                    etContent.setAlpha(1f);
                    // 如果内容不为空，重新设置闹钟
                    if (!item.content.isEmpty()) {
                        setTodoAlarm(item);
                    }
                }
            });
            
            // 点击时间，弹出时间选择器
            tvTime.setOnClickListener(v -> {
                TimePickerDialog dialog = new TimePickerDialog(requireContext(),
                        (view, hourOfDay, minute) -> {
                            item.hour = hourOfDay;
                            item.minute = minute;
                            tvTime.setText(item.getTimeString());
                            saveTodos();
                            
                            // 如果内容不为空且未完成，更新闹钟
                            if (!item.content.isEmpty() && !item.completed) {
                                setTodoAlarm(item);
                                Toast.makeText(requireContext(), "已更新提醒时间", Toast.LENGTH_SHORT).show();
                            }
                        },
                        item.hour, item.minute, true);
                dialog.show();
            });
            
            // 内容编辑完成
            etContent.setOnEditorActionListener((v, actionId, event) -> {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    String newContent = etContent.getText().toString().trim();
                    String oldContent = item.content;
                    item.content = newContent;
                    saveTodos();
                    
                    // 如果内容从空变为非空，设置闹钟
                    if (oldContent.isEmpty() && !newContent.isEmpty() && !item.completed) {
                        setTodoAlarm(item);
                        Toast.makeText(requireContext(), "已设置提醒：" + item.getTimeString(), Toast.LENGTH_SHORT).show();
                    } else if (!newContent.isEmpty() && !item.completed) {
                        // 更新闹钟
                        setTodoAlarm(item);
                    } else if (newContent.isEmpty()) {
                        // 取消闹钟
                        cancelTodoAlarm(item);
                    }
                    
                    // 隐藏键盘
                    etContent.clearFocus();
                    return true;
                }
                return false;
            });
            
            // 失去焦点时也保存
            etContent.setOnFocusChangeListener((v, hasFocus) -> {
                if (!hasFocus) {
                    String newContent = etContent.getText().toString().trim();
                    if (!newContent.equals(item.content)) {
                        String oldContent = item.content;
                        item.content = newContent;
                        saveTodos();
                        
                        if (!newContent.isEmpty() && !item.completed) {
                            setTodoAlarm(item);
                            if (oldContent.isEmpty()) {
                                Toast.makeText(requireContext(), "已设置提醒：" + item.getTimeString(), Toast.LENGTH_SHORT).show();
                            }
                        } else if (newContent.isEmpty()) {
                            cancelTodoAlarm(item);
                        }
                    }
                }
            });
            
            // 删除按钮
            btnDelete.setOnClickListener(v -> {
                cancelTodoAlarm(item);
                todoList.remove(index);
                saveTodos();
                renderTodoList();
                Toast.makeText(requireContext(), "已删除", Toast.LENGTH_SHORT).show();
            });
            
            llTodoList.addView(row);
        }
    }

    // 设置待办提醒闹钟
    private void setTodoAlarm(TodoItem item) {
        if (item.content.isEmpty()) return;
        
        try {
            AlarmManager alarmManager = (AlarmManager) requireContext().getSystemService(Context.ALARM_SERVICE);
            if (alarmManager == null) return;
            
            // 计算触发时间
            Calendar alarmCal = Calendar.getInstance();
            alarmCal.set(Calendar.HOUR_OF_DAY, item.hour);
            alarmCal.set(Calendar.MINUTE, item.minute);
            alarmCal.set(Calendar.SECOND, 0);
            alarmCal.set(Calendar.MILLISECOND, 0);
            
            // 如果时间已过，不设置闹钟
            if (alarmCal.getTimeInMillis() <= System.currentTimeMillis()) {
                return;
            }
            
            // 创建Intent，使用待办提醒专用的Receiver
            Intent intent = new Intent(requireContext(), TodoAlarmReceiver.class);
            intent.putExtra("todo_id", item.id);
            intent.putExtra("todo_content", item.content);
            intent.putExtra("todo_time", item.getTimeString());
            
            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    requireContext(),
                    item.id,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT
            );
            
            // 设置闹钟
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        alarmCal.getTimeInMillis(),
                        pendingIntent
                );
            } else {
                alarmManager.setExact(
                        AlarmManager.RTC_WAKEUP,
                        alarmCal.getTimeInMillis(),
                        pendingIntent
                );
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 取消待办提醒闹钟
    private void cancelTodoAlarm(TodoItem item) {
        try {
            AlarmManager alarmManager = (AlarmManager) requireContext().getSystemService(Context.ALARM_SERVICE);
            if (alarmManager == null) return;
            
            Intent intent = new Intent(requireContext(), TodoAlarmReceiver.class);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    requireContext(),
                    item.id,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT
            );
            
            alarmManager.cancel(pendingIntent);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

