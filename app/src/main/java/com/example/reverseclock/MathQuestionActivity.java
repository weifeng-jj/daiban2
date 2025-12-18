package com.example.reverseclock;

import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.util.Random;

// 数学题关闹钟：100以内加减法，连续答对5道
public class MathQuestionActivity extends AppCompatActivity {
    // 控件声明（确保和布局文件ID完全一致）
    private TextView tvQuestion, tvProgress, tvTargetHint;
    private EditText etAnswer;
    private Button btnSubmit;

    private int correctCount = 0; // 连续答对数
    private int correctAnswer; // 正确答案
    private int targetCount = 5; // 目标题目数量（可配置）

    // 天气相关UI
    private TextView tvWeatherTitle;
    private TextView tvWeatherDetail;
    private Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 绑定布局文件（确保布局文件名正确）
        setContentView(R.layout.activity_math_question);

        // 初始化控件（兼容所有Android版本，无高版本API依赖）
        try {
            tvQuestion = findViewById(R.id.tvQuestion);
            etAnswer = findViewById(R.id.etAnswer);
            btnSubmit = findViewById(R.id.btnSubmit);
            tvProgress = findViewById(R.id.tvProgress);
            // 天气区域（可选，如果布局中不存在则忽略）
            tvWeatherTitle = findViewById(R.id.tvWeatherTitle);
            tvWeatherDetail = findViewById(R.id.tvWeatherDetail);
        } catch (Exception e) {
            // 控件ID匹配失败时的容错提示
            Toast.makeText(this, "控件初始化失败，请检查布局ID是否匹配", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        // 读取目标题目数量（来自闹钟配置）
        targetCount = getIntent().getIntExtra("math_target", 5);
        Log.d("MathDebug", "目标题目数量：" + targetCount);
        
        // 初始化提示文本
        tvTargetHint = findViewById(R.id.tvTargetHint);
        if (tvTargetHint != null) {
            tvTargetHint.setText("连续答对" + targetCount + "道题关闭闹钟！");
        }
        
        if (tvProgress != null) {
            tvProgress.setText("已答对：0/" + targetCount);
        }

        // 生成第一道题
        generateQuestion();

        // 异步获取当前位置 + 天气，并在顶部展示
        loadWeatherAsync();

        // 提交答案点击事件（兼容低版本点击监听）
        btnSubmit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkAnswer();
            }
        });
    }

    // 异步获取天气信息并更新UI
    private void loadWeatherAsync() {
        if (tvWeatherTitle == null || tvWeatherDetail == null) {
            return; // 布局里没有天气区域则直接返回
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
                                    // 如果城市名称获取失败，继续尝试GPS定位
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
                    Log.d("WeatherLocation", "开始获取位置信息...");
                    LocationManager lm = (LocationManager) getSystemService(LOCATION_SERVICE);
                    Location location = null;
                    if (lm == null) {
                        Log.e("WeatherLocation", "LocationManager为null");
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                tvWeatherTitle.setText("天气信息不可用");
                                tvWeatherDetail.setText("无法获取定位服务。");
                            }
                        });
                        return;
                    }
                    
                    try {
                        Log.d("WeatherLocation", "尝试获取位置（优先网络定位，更准确）...");
                        // 优先使用网络定位（更准确，特别是对于模拟器）
                        location = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                        if (location != null) {
                            Log.d("WeatherLocation", "网络缓存位置获取成功: " + location.getLatitude() + "," + location.getLongitude());
                            // 检查位置是否合理（避免使用模拟器默认测试坐标）
                            if (isLocationValid(location)) {
                                Log.d("WeatherLocation", "位置验证通过");
                            } else {
                                Log.w("WeatherLocation", "位置可能不准确（可能是模拟器默认坐标），尝试GPS...");
                                location = null; // 清除，尝试GPS
                            }
                        }
                        
                        // 如果网络定位失败或无效，尝试GPS
                        if (location == null) {
                            Log.d("WeatherLocation", "尝试GPS定位...");
                            location = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                            if (location != null) {
                                Log.d("WeatherLocation", "GPS缓存位置获取成功: " + location.getLatitude() + "," + location.getLongitude());
                                if (!isLocationValid(location)) {
                                    Log.w("WeatherLocation", "GPS位置也可能不准确");
                                }
                            } else {
                                Log.d("WeatherLocation", "所有缓存位置都为空，开始请求实时定位...");
                            }
                        }
                        
                        // 如果缓存位置为空，主动请求一次实时定位
                        if (location == null) {
                            Log.d("WeatherLocation", "调用requestFreshLocation请求实时定位");
                            requestFreshLocation(lm);
                            return; // 等待定位回调
                        }
                    } catch (SecurityException se) {
                        // 没有定位权限时给出文案提示
                        Log.e("WeatherLocation", "定位权限被拒绝: " + se.getMessage());
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

                    // 使用获取到的位置获取天气
                    final Location finalLocation = location;
                    if (finalLocation == null) {
                        Log.e("WeatherLocation", "最终位置为null，无法获取天气");
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                tvWeatherTitle.setText("天气获取失败");
                                tvWeatherDetail.setText("无法获取当前位置，请检查定位权限和网络连接。\n\n提示：可以在设置闹钟时手动输入城市名称。");
                            }
                        });
                        return;
                    }
                    
                    Log.d("WeatherLocation", "使用位置获取天气: " + finalLocation.getLatitude() + "," + finalLocation.getLongitude());
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
        // 或者坐标明显不合理（经纬度超出正常范围）
        if (Math.abs(lat - 37.422) < 0.01 && Math.abs(lon - (-122.084)) < 0.01) {
            Log.w("WeatherLocation", "检测到可能是模拟器默认测试坐标");
            return false;
        }
        
        // 检查坐标是否在合理范围内（纬度-90到90，经度-180到180）
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
        if (lm == null) {
            Log.e("WeatherLocation", "LocationManager为null，无法请求实时定位");
            return;
        }
        
        try {
            Log.d("WeatherLocation", "检查定位服务状态...");
            boolean gpsEnabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
            boolean networkEnabled = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
            Log.d("WeatherLocation", "GPS启用: " + gpsEnabled + ", 网络定位启用: " + networkEnabled);
            
            if (!gpsEnabled && !networkEnabled) {
                Log.e("WeatherLocation", "所有定位服务都未启用");
                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        tvWeatherTitle.setText("天气信息不可用");
                        tvWeatherDetail.setText("系统定位开关未打开，请在设置中开启位置服务。");
                    }
                });
                return;
            }
            
            Log.d("WeatherLocation", "注册LocationListener，开始请求实时定位...");
            LocationListener listener = new LocationListener() {
                @Override
                public void onLocationChanged(Location location) {
                    if (location != null && lm != null) {
                        Log.d("WeatherLocation", "收到实时定位结果: " + location.getLatitude() + "," + location.getLongitude());
                        lm.removeUpdates(this);
                        
                        // 验证位置是否合理
                        if (!isLocationValid(location)) {
                            Log.w("WeatherLocation", "实时定位结果可能不准确，但继续使用");
                            // 即使位置可能不准确，也继续使用（至少比没有好）
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

    // 生成100以内加减法题（无高版本API，纯基础逻辑）
    private void generateQuestion() {
        Random random = new Random();
        int a = random.nextInt(100);
        int b = random.nextInt(100);

        // 确保减法结果非负，避免负数答案
        if (a < b) {
            int temp = a;
            a = b;
            b = temp;
        }

        // 随机生成加法/减法题
        boolean isAdd = random.nextBoolean();
        String question;
        if (isAdd) {
            question = a + " + " + b + " = ?";
            correctAnswer = a + b;
        } else {
            question = a + " - " + b + " = ?";
            correctAnswer = a - b;
        }

        // 赋值到控件（空指针容错）
        if (tvQuestion != null) {
            tvQuestion.setText(question);
        }
        if (etAnswer != null) {
            etAnswer.setText("");
        }
    }

    // 检查答案（纯基础逻辑，兼容所有版本）
    private void checkAnswer() {
        // 空值校验（避免空指针）
        if (etAnswer == null || tvProgress == null) {
            return;
        }

        String input = etAnswer.getText().toString().trim();
        if (input.isEmpty()) {
            Toast.makeText(this, "请输入答案", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            int userAnswer = Integer.parseInt(input);
            if (userAnswer == correctAnswer) {
                correctCount++;
                tvProgress.setText("已答对：" + correctCount + "/" + targetCount);
                Toast.makeText(this, "答对了！", Toast.LENGTH_SHORT).show();

                // 连续答对 targetCount 道，关闭闹钟并退出页面
                if (correctCount >= targetCount) {
                    // 停止闹钟铃声（兼容低版本，避免静态方法跨版本问题）
                    try {
                        AlarmReceiver.stopAlarmSound();
                    } catch (Exception e) {
                        // 低版本容错：即使铃声停止失败，也关闭页面
                        e.printStackTrace();
                    }
                    Toast.makeText(this, "恭喜！闹钟已关闭", Toast.LENGTH_LONG).show();
                    finish(); // 关闭当前页面
                    return;
                }
                // 生成下一题
                generateQuestion();
            } else {
                // 答错重置计数
                correctCount = 0;
                tvProgress.setText("已答对：0/" + targetCount);
                Toast.makeText(this, "答错了，重新开始！", Toast.LENGTH_SHORT).show();
                generateQuestion();
            }
        } catch (NumberFormatException e) {
            // 仅捕获数字格式错误，避免全盘捕获
            Toast.makeText(this, "请输入有效数字", Toast.LENGTH_SHORT).show();
        }
    }
}