package com.example.reverseclock;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

// 摇晃手机关闹钟：自定义次数（适配模拟器+防崩溃）
public class ShakeActivity extends AppCompatActivity implements SensorEventListener {
    private TextView tvShakeCount;
    private SensorManager sensorManager;
    private Sensor accelerometer;

    private int shakeCount = 0; // 已摇晃次数
    private int targetCount = 10; // 目标次数
    private long lastShakeTime = 0; // 上次摇晃时间（防重复计数）

    // 天气相关
    private TextView tvWeatherTitle;
    private TextView tvWeatherDetail;
    private Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shake);

        // 初始化控件（增加容错）
        try {
            tvShakeCount = findViewById(R.id.tvShakeCount);
            // 尝试获取天气区域控件（如果布局中定义了）
            tvWeatherTitle = findViewById(R.id.tvWeatherTitle);
            tvWeatherDetail = findViewById(R.id.tvWeatherDetail);
        } catch (Exception e) {
            Log.e("ShakeDebug", "初始化控件失败：" + e.getMessage());
            Toast.makeText(this, "页面初始化失败", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        // 获取目标摇晃次数（增加容错）
        try {
            targetCount = getIntent().getIntExtra(MainActivity.EXTRA_SHAKE_COUNT, 10);
        } catch (Exception e) {
            targetCount = 10; // 默认值
        }
        tvShakeCount.setText("已摇晃：0/" + targetCount + "次");

        // 异步加载天气
        loadWeatherAsync();

        // 初始化传感器 + 容错处理
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager == null) {
            Toast.makeText(this, "传感器服务获取失败！", Toast.LENGTH_LONG).show();
        } else {
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            if (accelerometer == null) {
                Toast.makeText(this, "当前设备不支持加速度传感器！", Toast.LENGTH_LONG).show();
            }
        }

        // 新增：手动测试按钮（防崩溃版）
        try {
            Button testBtn = new Button(this);
            testBtn.setText("手动+1次");
            testBtn.setTextSize(18);
            testBtn.setBackgroundColor(ContextCompat.getColor(this, R.color.primary));
            testBtn.setTextColor(ContextCompat.getColor(this, R.color.white));

            // 将按钮安全地添加到根布局
            ViewGroup contentView = findViewById(android.R.id.content);
            LinearLayout rootLayout = null;
            if (contentView != null && contentView.getChildCount() > 0 && contentView.getChildAt(0) instanceof LinearLayout) {
                rootLayout = (LinearLayout) contentView.getChildAt(0);
            } else {
                rootLayout = new LinearLayout(this);
                rootLayout.setOrientation(LinearLayout.VERTICAL);
                rootLayout.setGravity(Gravity.CENTER);
                if (contentView != null) {
                    contentView.removeAllViews();
                    contentView.addView(rootLayout);
                } else {
                    setContentView(rootLayout);
                }
                if (tvShakeCount != null) {
                    rootLayout.addView(tvShakeCount);
                }
            }

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            params.setMargins(0, 30, 0, 0);
            testBtn.setLayoutParams(params);
            rootLayout.addView(testBtn);

            testBtn.setOnClickListener(v -> {
                shakeCount++;
                tvShakeCount.setText("已摇晃：" + shakeCount + "/" + targetCount + "次");
                if (shakeCount >= targetCount) {
                    AlarmReceiver.stopAlarmSound();
                    Toast.makeText(this, "摇晃完成！闹钟已关闭", Toast.LENGTH_LONG).show();
                    finish();
                }
            });
        } catch (Exception e) {
            Log.e("ShakeDebug", "添加测试按钮失败：" + e.getMessage());
        }
    }

    // 与数学题页面类似：获取当前位置 + 天气信息
    private void loadWeatherAsync() {
        if (tvWeatherTitle == null || tvWeatherDetail == null) {
            return;
        }
        tvWeatherTitle.setText("正在获取当前位置天气...");
        tvWeatherDetail.setText("");

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    // 优先使用保存的城市名称获取天气
                    android.content.SharedPreferences sp = getSharedPreferences("weather_prefs", MODE_PRIVATE);
                    String savedCity = sp.getString("city_name", "");
                    
                    if (!savedCity.isEmpty()) {
                        Log.d("WeatherLocation", "使用保存的城市名称获取天气: " + savedCity);
                        final WeatherHelper.WeatherInfo info = WeatherHelper.fetchWeatherByCityName(savedCity);
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                if (info == null) {
                                    tvWeatherTitle.setText("天气获取失败");
                                    tvWeatherDetail.setText("无法获取 " + savedCity + " 的天气，将尝试GPS定位...");
                                    loadWeatherByLocation();
                                } else {
                                    tvWeatherTitle.setText(savedCity + " 实时天气");
                                    String detail = "温度：" + Math.round(info.tempC) + "℃\n"
                                            + "紫外线指数：" + String.format("%.1f", info.uvIndex) + "\n"
                                            + info.suggestion;
                                    tvWeatherDetail.setText(detail);
                                }
                            }
                        });
                        return;
                    }
                    
                    // 如果没有保存的城市名称，使用GPS定位
                    LocationManager lm = (LocationManager) getSystemService(LOCATION_SERVICE);
                    Location location = null;
                    if (lm != null) {
                        try {
                            // 优先使用网络定位（更准确，特别是模拟器）
                            location = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                            if (location != null && !isLocationValid(location)) {
                                // 如果网络定位是模拟器默认坐标，尝试GPS
                                location = null;
                            }
                            if (location == null) {
                                location = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                            }
                            
                            // 如果缓存位置为空，主动请求一次实时定位
                            if (location == null) {
                                requestFreshLocation(lm);
                                return; // 等待定位回调
                            }
                        } catch (SecurityException se) {
                            final String msg = "未授予定位权限，无法获取天气。";
                            mainHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    tvWeatherTitle.setText("天气信息不可用");
                                    tvWeatherDetail.setText(msg);
                                }
                            });
                            return;
                        }
                    }

                    // 使用获取到的位置获取天气
                    final Location finalLocation = location;
                    if (finalLocation == null) {
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                tvWeatherTitle.setText("天气获取失败");
                                tvWeatherDetail.setText("无法获取当前位置，请检查定位权限和网络连接。\n\n提示：可以在设置闹钟时手动输入城市名称。");
                            }
                        });
                        return;
                    }
                    
                    final WeatherHelper.WeatherInfo info = WeatherHelper.fetchWeatherByLocation(finalLocation);
                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            if (info == null) {
                                tvWeatherTitle.setText("天气获取失败");
                                tvWeatherDetail.setText("可能原因：\n1. 网络连接问题\n2. API Key无效或权限不足\n3. 和风天气服务异常\n\n请查看Logcat中'WeatherHelper'标签的详细错误信息。");
                            } else {
                                tvWeatherTitle.setText("当前位置天气小助手");
                                String detail = "温度：" + Math.round(info.tempC) + "℃\n"
                                        + "估算紫外线指数：" + String.format("%.1f", info.uvIndex) + "\n"
                                        + info.suggestion;
                                tvWeatherDetail.setText(detail);
                            }
                        }
                    });
                } catch (Exception e) {
                    final String err = e.getMessage();
                    e.printStackTrace();
                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            tvWeatherTitle.setText("天气获取异常");
                            tvWeatherDetail.setText("异常信息：" + (err != null ? err : "未知错误") + "\n\n请查看Logcat日志获取详细堆栈信息。");
                        }
                    });
                }
            }
        }).start();
    }

    // 检查位置是否合理（避免使用模拟器默认测试坐标）
    private boolean isLocationValid(Location location) {
        if (location == null) return false;
        double lat = location.getLatitude();
        double lon = location.getLongitude();
        
        // 检查是否是Google模拟器的默认测试坐标（-122.084, 37.422）
        if (Math.abs(lat - 37.422) < 0.01 && Math.abs(lon - (-122.084)) < 0.01) {
            return false;
        }
        
        // 检查坐标是否在合理范围内
        if (lat < -90 || lat > 90 || lon < -180 || lon > 180) {
            return false;
        }
        
        return true;
    }

    // 通过GPS定位获取天气（当没有保存的城市名称时使用）
    private void loadWeatherByLocation() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    LocationManager lm = (LocationManager) getSystemService(LOCATION_SERVICE);
                    if (lm == null) {
                        mainHandler.post(() -> {
                            tvWeatherTitle.setText("天气信息不可用");
                            tvWeatherDetail.setText("无法获取定位服务。");
                        });
                        return;
                    }
                    
                    Location location = null;
                    try {
                        location = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                        if (location != null && !isLocationValid(location)) {
                            location = null;
                        }
                        if (location == null) {
                            location = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                        }
                        
                        if (location == null) {
                            requestFreshLocation(lm);
                            return;
                        }
                        
                        final Location finalLocation = location;
                        final WeatherHelper.WeatherInfo info = WeatherHelper.fetchWeatherByLocation(finalLocation);
                        mainHandler.post(() -> {
                            if (info == null) {
                                tvWeatherTitle.setText("天气获取失败");
                                tvWeatherDetail.setText("可能原因：\n1. 网络连接问题\n2. API Key无效或权限不足\n3. 和风天气服务异常\n\n请查看Logcat中'WeatherHelper'标签的详细错误信息。");
                            } else {
                                tvWeatherTitle.setText("当前位置天气小助手");
                                String detail = "温度：" + Math.round(info.tempC) + "℃\n"
                                        + "紫外线指数：" + String.format("%.1f", info.uvIndex) + "\n"
                                        + info.suggestion;
                                tvWeatherDetail.setText(detail);
                            }
                        });
                    } catch (SecurityException se) {
                        mainHandler.post(() -> {
                            tvWeatherTitle.setText("天气信息不可用");
                            tvWeatherDetail.setText("未授予定位权限，无法获取天气。");
                        });
                    }
                } catch (Exception e) {
                    mainHandler.post(() -> {
                        tvWeatherTitle.setText("天气获取异常");
                        tvWeatherDetail.setText("异常信息：" + e.getMessage());
                    });
                }
            }
        }).start();
    }

    // 主动请求一次实时定位
    private void requestFreshLocation(LocationManager lm) {
        if (lm == null) return;
        
        try {
            boolean gpsEnabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
            boolean networkEnabled = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
            
            if (!gpsEnabled && !networkEnabled) {
                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        tvWeatherTitle.setText("天气信息不可用");
                        tvWeatherDetail.setText("系统定位开关未打开，请在设置中开启位置服务。");
                    }
                });
                return;
            }
            
            LocationListener listener = new LocationListener() {
                @Override
                public void onLocationChanged(Location location) {
                    if (location != null && lm != null) {
                        lm.removeUpdates(this);
                        // 验证位置是否合理
                        if (!isLocationValid(location)) {
                            // 位置可能不准确，但仍然使用（至少比没有好）
                        }
                        // 使用新获取的位置获取天气
                        final WeatherHelper.WeatherInfo info = WeatherHelper.fetchWeatherByLocation(location);
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                if (info == null) {
                                    tvWeatherTitle.setText("天气获取失败");
                                    tvWeatherDetail.setText("可以稍后检查网络或稍微再试一次。");
                                } else {
                                    tvWeatherTitle.setText("当前位置天气小助手");
                                    String detail = "温度：" + Math.round(info.tempC) + "℃\n"
                                            + "估算紫外线指数：" + String.format("%.1f", info.uvIndex) + "\n"
                                            + info.suggestion;
                                    tvWeatherDetail.setText(detail);
                                }
                            }
                        });
                    }
                }
                
                @Override
                public void onStatusChanged(String provider, int status, Bundle extras) {}
                @Override
                public void onProviderEnabled(String provider) {}
                @Override
                public void onProviderDisabled(String provider) {}
            };
            
            if (networkEnabled) {
                lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, listener);
            }
            if (gpsEnabled) {
                lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, listener);
            }
            
            // 设置超时：10秒后如果还没拿到位置，取消监听
            mainHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (listener != null && lm != null) {
                        try {
                            lm.removeUpdates(listener);
                        } catch (Exception e) {}
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                tvWeatherTitle.setText("天气信息不可用");
                                tvWeatherDetail.setText("定位超时，请检查网络和定位权限。");
                            }
                        });
                    }
                }
            }, 10000);
        } catch (SecurityException e) {
            mainHandler.post(new Runnable() {
                @Override
                public void run() {
                    tvWeatherTitle.setText("天气信息不可用");
                    tvWeatherDetail.setText("未授予定位权限，无法获取天气。");
                }
            });
        }
    }

    // 注册传感器监听（增加容错）
    @Override
    protected void onResume() {
        super.onResume();
        if (sensorManager != null && accelerometer != null) {
            try {
                sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME);
            } catch (Exception e) {
                Log.e("ShakeDebug", "注册传感器失败：" + e.getMessage());
            }
        }
    }

    // 取消监听
    @Override
    protected void onPause() {
        super.onPause();
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }
    }

    // 传感器数据变化（适配模拟器）
    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event == null || event.sensor == null) return;
        if (event.sensor.getType() != Sensor.TYPE_ACCELEROMETER) return;

        try {
            // 获取加速度（x/y/z）
            float x = event.values[0];
            float y = event.values[1];
            float z = event.values[2];

            // 计算总加速度
            float acceleration = (float) Math.sqrt(x*x + y*y + z*z) - SensorManager.GRAVITY_EARTH;
            Log.d("ShakeDebug", "当前加速度：" + acceleration);

            // 降低阈值+缩短防抖间隔
            if (acceleration > 5) {
                long currentTime = System.currentTimeMillis();
                if (currentTime - lastShakeTime > 300) {
                    shakeCount++;
                    tvShakeCount.setText("已摇晃：" + shakeCount + "/" + targetCount + "次");
                    lastShakeTime = currentTime;

                    if (shakeCount >= targetCount) {
                        AlarmReceiver.stopAlarmSound();
                        Toast.makeText(this, "摇晃完成！闹钟已关闭", Toast.LENGTH_LONG).show();
                        finish();
                    }
                }
            }
        } catch (Exception e) {
            Log.e("ShakeDebug", "传感器处理失败：" + e.getMessage());
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}
}