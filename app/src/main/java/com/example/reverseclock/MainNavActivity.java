package com.example.reverseclock;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainNavActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_nav);

        BottomNavigationView nav = findViewById(R.id.bottomNav);
        nav.setOnItemSelectedListener(item -> {
            Fragment fragment;
            int id = item.getItemId();
            if (id == R.id.nav_alarm) {
                fragment = new AlarmFragment();
            } else if (id == R.id.nav_todo) {
                fragment = new TodoFragment();
            } else if (id == R.id.nav_timetool) {
                // 合并后的计时工具（秒表 + 计时器）
                fragment = new TimeToolFragment();
            } else if (id == R.id.nav_countdown) {
                // 倒数日功能
                fragment = new CountdownFragment();
            } else {
                fragment = new AlarmFragment();
            }
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragmentContainer, fragment)
                    .commit();
            return true;
        });

        if (savedInstanceState == null) {
            nav.setSelectedItemId(R.id.nav_alarm);
        }
    }
}
