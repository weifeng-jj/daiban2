package com.example.reverseclock;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 倒数日功能Fragment
 * 可以添加多个倒数日事件，显示距离目标日期还有多少天
 */
public class CountdownFragment extends Fragment {

    private LinearLayout llCountdownList;
    private Button btnAddCountdown;
    private List<CountdownItem> countdownList = new ArrayList<>();

    // 添加对话框中的临时变量
    private int tempYear = -1;
    private int tempMonth = -1;
    private int tempDay = -1;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_countdown, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        llCountdownList = view.findViewById(R.id.llCountdownList);
        btnAddCountdown = view.findViewById(R.id.btnAddCountdown);

        btnAddCountdown.setOnClickListener(v -> showAddCountdownDialog());

        // 加载已保存的倒数日
        countdownList = CountdownStore.load(requireContext());
        renderCountdownList();
    }

    @Override
    public void onResume() {
        super.onResume();
        // 每次回到页面时刷新列表（更新剩余天数）
        countdownList = CountdownStore.load(requireContext());
        renderCountdownList();
    }

    /**
     * 显示添加倒数日对话框
     */
    private void showAddCountdownDialog() {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_countdown, null);
        
        EditText etName = dialogView.findViewById(R.id.etCountdownName);
        Button btnSelectDate = dialogView.findViewById(R.id.btnSelectDate);
        TextView tvSelectedDate = dialogView.findViewById(R.id.tvSelectedDate);

        // 重置临时变量
        tempYear = -1;
        tempMonth = -1;
        tempDay = -1;

        btnSelectDate.setOnClickListener(v -> {
            Calendar cal = Calendar.getInstance();
            DatePickerDialog datePickerDialog = new DatePickerDialog(
                    requireContext(),
                    (datePicker, year, month, dayOfMonth) -> {
                        tempYear = year;
                        tempMonth = month + 1; // DatePicker的month是0-11
                        tempDay = dayOfMonth;
                        String dateStr = String.format("%d年%d月%d日", tempYear, tempMonth, tempDay);
                        tvSelectedDate.setText(dateStr);
                        tvSelectedDate.setVisibility(View.VISIBLE);
                        btnSelectDate.setText("已选择: " + dateStr);
                    },
                    cal.get(Calendar.YEAR),
                    cal.get(Calendar.MONTH),
                    cal.get(Calendar.DAY_OF_MONTH)
            );
            datePickerDialog.show();
        });

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .setPositiveButton("添加", null) // 先设为null，后面手动处理
                .setNegativeButton("取消", null)
                .create();

        dialog.show();

        // 手动处理确认按钮，防止对话框自动关闭
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            
            if (name.isEmpty()) {
                Toast.makeText(requireContext(), "请输入事件名称", Toast.LENGTH_SHORT).show();
                return;
            }
            
            if (tempYear == -1 || tempMonth == -1 || tempDay == -1) {
                Toast.makeText(requireContext(), "请选择目标日期", Toast.LENGTH_SHORT).show();
                return;
            }

            // 创建新的倒数日
            int id = (int) (System.currentTimeMillis() & 0x7fffffff);
            CountdownItem item = new CountdownItem(
                    id,
                    name,
                    tempYear,
                    tempMonth,
                    tempDay,
                    System.currentTimeMillis()
            );
            
            countdownList.add(item);
            CountdownStore.save(requireContext(), countdownList);
            renderCountdownList();
            
            Toast.makeText(requireContext(), "倒数日添加成功！", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });
    }

    /**
     * 渲染倒数日列表
     */
    private void renderCountdownList() {
        if (llCountdownList == null) return;
        llCountdownList.removeAllViews();

        if (countdownList == null || countdownList.isEmpty()) {
            TextView emptyTv = new TextView(requireContext());
            emptyTv.setText("📅 还没有倒数日，点击右上角添加吧！");
            emptyTv.setTextColor(ContextCompat.getColor(requireContext(), R.color.gray));
            emptyTv.setTextSize(16);
            emptyTv.setPadding(0, 48, 0, 0);
            llCountdownList.addView(emptyTv);
            return;
        }

        for (CountdownItem item : countdownList) {
            View row = LayoutInflater.from(requireContext()).inflate(R.layout.item_countdown_row, llCountdownList, false);
            
            TextView tvName = row.findViewById(R.id.tvCountdownName);
            TextView tvDate = row.findViewById(R.id.tvCountdownDate);
            TextView tvDays = row.findViewById(R.id.tvCountdownDays);
            TextView tvStatus = row.findViewById(R.id.tvCountdownStatus);
            Button btnDelete = row.findViewById(R.id.btnDeleteCountdown);

            // 设置事件名称
            tvName.setText(item.name);
            
            // 设置目标日期
            String dateStr = String.format("%d年%d月%d日", item.year, item.month, item.day);
            tvDate.setText(dateStr);

            // 计算剩余天数
            long daysRemaining = calculateDaysRemaining(item.year, item.month, item.day);
            
            if (daysRemaining > 0) {
                tvStatus.setText("还有");
                tvDays.setText(String.valueOf(daysRemaining));
                tvDays.setTextColor(ContextCompat.getColor(requireContext(), R.color.primary));
            } else if (daysRemaining == 0) {
                tvStatus.setText("就是今天！");
                tvDays.setText("");
                tvDays.setTextColor(ContextCompat.getColor(requireContext(), R.color.secondary));
            } else {
                tvStatus.setText("已过");
                tvDays.setText(String.valueOf(Math.abs(daysRemaining)));
                tvDays.setTextColor(ContextCompat.getColor(requireContext(), R.color.gray));
            }

            // 删除按钮
            btnDelete.setOnClickListener(v -> {
                new AlertDialog.Builder(requireContext())
                        .setTitle("删除倒数日")
                        .setMessage("确定要删除「" + item.name + "」吗？")
                        .setPositiveButton("删除", (dialog, which) -> {
                            countdownList.remove(item);
                            CountdownStore.save(requireContext(), countdownList);
                            renderCountdownList();
                            Toast.makeText(requireContext(), "已删除", Toast.LENGTH_SHORT).show();
                        })
                        .setNegativeButton("取消", null)
                        .show();
            });

            llCountdownList.addView(row);
        }
    }

    /**
     * 计算距离目标日期还有多少天
     * @return 正数表示还有多少天，0表示今天，负数表示已过多少天
     */
    private long calculateDaysRemaining(int year, int month, int day) {
        Calendar target = Calendar.getInstance();
        target.set(Calendar.YEAR, year);
        target.set(Calendar.MONTH, month - 1); // Calendar的月份是0-11
        target.set(Calendar.DAY_OF_MONTH, day);
        target.set(Calendar.HOUR_OF_DAY, 0);
        target.set(Calendar.MINUTE, 0);
        target.set(Calendar.SECOND, 0);
        target.set(Calendar.MILLISECOND, 0);

        Calendar today = Calendar.getInstance();
        today.set(Calendar.HOUR_OF_DAY, 0);
        today.set(Calendar.MINUTE, 0);
        today.set(Calendar.SECOND, 0);
        today.set(Calendar.MILLISECOND, 0);

        long diffMillis = target.getTimeInMillis() - today.getTimeInMillis();
        return TimeUnit.MILLISECONDS.toDays(diffMillis);
    }
}

