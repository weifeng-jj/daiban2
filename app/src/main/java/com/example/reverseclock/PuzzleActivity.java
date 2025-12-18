package com.example.reverseclock;

import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

// 滑动拼图关闹钟（支持3x3或4x4）
public class PuzzleActivity extends AppCompatActivity {
    private GridView gridPuzzle;
    private ImageView ivReference; // 参考图
    private PuzzleAdapter adapter;
    private List<Integer> puzzleList; // 拼图块索引
    private int emptyPos; // 空白块位置（最后一位）
    private int gridSize = 4; // 3或4
    private int totalCount = 16;

    // 天气相关
    private TextView tvWeatherTitle;
    private TextView tvWeatherDetail;
    private Handler mainHandler = new Handler(Looper.getMainLooper());
    private LocationManager locationManager;
    private LocationListener locationListener;

    // 使用独立图片，最后一张作为空白块（可做成纯色/空白图）
    private int[] puzzleRes;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_puzzle);

        gridPuzzle = findViewById(R.id.gridPuzzle);
        ivReference = findViewById(R.id.ivReference);
        
        // 初始化天气显示控件
        tvWeatherTitle = findViewById(R.id.tvWeatherTitle);
        tvWeatherDetail = findViewById(R.id.tvWeatherDetail);
        
        // 异步加载天气
        loadWeatherAsync();

        // 读取拼图规模（3x3或4x4）
        int mode = getIntent().getIntExtra("puzzle_mode", 4);
        if (mode == 3) {
            gridSize = 3;
            totalCount = 9;
            puzzleRes = new int[]{
                    R.drawable.puzzle_31, R.drawable.puzzle_32, R.drawable.puzzle_33,
                    R.drawable.puzzle_34, R.drawable.puzzle_35, R.drawable.puzzle_36,
                    R.drawable.puzzle_37, R.drawable.puzzle_38, R.drawable.puzzle_39
            };
            // 设置 3x3 参考图
            if (ivReference != null) {
                ivReference.setImageResource(R.drawable.puzzle_x);
            }
        } else {
            gridSize = 4;
            totalCount = 16;
            puzzleRes = new int[]{
                    R.drawable.puzzle_1,  R.drawable.puzzle_2,  R.drawable.puzzle_3,  R.drawable.puzzle_4,
                    R.drawable.puzzle_5,  R.drawable.puzzle_6,  R.drawable.puzzle_7,  R.drawable.puzzle_8,
                    R.drawable.puzzle_9,  R.drawable.puzzle_10, R.drawable.puzzle_11, R.drawable.puzzle_12,
                    R.drawable.puzzle_13, R.drawable.puzzle_14, R.drawable.puzzle_15, R.drawable.puzzle_16
            };
            // 设置 4x4 参考图
            if (ivReference != null) {
                ivReference.setImageResource(R.drawable.puzzle_y);
            }
        }

        // 根据当前拼图规模动态设置 GridView 的列数，避免 3x3 仍然按 4 列显示
        gridPuzzle.setNumColumns(gridSize);

        // 初始化拼图列表（0 ~ totalCount-1，打乱）
        puzzleList = new ArrayList<>();
        for (int i = 0; i < totalCount; i++) {
            puzzleList.add(i);
        }
        Collections.shuffle(puzzleList);
        emptyPos = puzzleList.indexOf(totalCount - 1);

        // 设置适配器
        adapter = new PuzzleAdapter();
        gridPuzzle.setAdapter(adapter);

        // 点击拼图块
        gridPuzzle.setOnItemClickListener((parent, view, position, id) -> movePuzzle(position));
    }

    // 异步获取天气信息并更新UI
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
                    locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
                    Location location = null;
                    if (locationManager != null) {
                        try {
                            // 先尝试获取缓存位置
                            // 优先使用网络定位（更准确，特别是模拟器）
                            location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                            if (location != null && !isLocationValid(location)) {
                                // 如果网络定位是模拟器默认坐标，尝试GPS
                                location = null;
                            }
                            if (location == null) {
                                location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                            }
                            
                            // 如果缓存位置为空，主动请求一次实时定位
                            if (location == null) {
                                requestFreshLocation();
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
                                tvWeatherDetail.setText("可能原因：\n1. 网络连接问题\n2. API Key无效\n3. 高德地图服务异常\n\n请查看Logcat日志获取详细错误信息。");
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
                            requestFreshLocation();
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
    private void requestFreshLocation() {
        if (locationManager == null) return;
        
        try {
            boolean gpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
            boolean networkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
            
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
            
            locationListener = new LocationListener() {
                @Override
                public void onLocationChanged(Location location) {
                    if (location != null && locationManager != null) {
                        locationManager.removeUpdates(this);
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
                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locationListener);
            }
            if (gpsEnabled) {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
            }
            
            // 设置超时：10秒后如果还没拿到位置，取消监听
            mainHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (locationListener != null && locationManager != null) {
                        try {
                            locationManager.removeUpdates(locationListener);
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (locationManager != null && locationListener != null) {
            try {
                locationManager.removeUpdates(locationListener);
            } catch (Exception e) {}
        }
    }

    // 移动拼图块
    private void movePuzzle(int position) {
        // 检查是否可移动（上下左右）
        if (canMove(position)) {
            // 交换位置
            Collections.swap(puzzleList, position, emptyPos);
            emptyPos = position;
            adapter.notifyDataSetChanged();

            // 检查是否完成拼图
            if (isPuzzleCompleted()) {
                AlarmReceiver.stopAlarmSound();
                Toast.makeText(this, "拼图完成！闹钟已关闭", Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }

    // 判断是否可移动（与空格上下左右相邻）
    private boolean canMove(int position) {
        int row = position / gridSize;
        int col = position % gridSize;
        int emptyRow = emptyPos / gridSize;
        int emptyCol = emptyPos % gridSize;

        return (row == emptyRow && Math.abs(col - emptyCol) == 1)
                || (col == emptyCol && Math.abs(row - emptyRow) == 1);
    }

    // 检查拼图是否完成（0~N-1顺序排列）
    private boolean isPuzzleCompleted() {
        for (int i = 0; i < totalCount; i++) {
            if (puzzleList.get(i) != i) {
                return false;
            }
        }
        return true;
    }

    // 拼图适配器
    private class PuzzleAdapter extends BaseAdapter {
        private int itemSize = -1; // 缓存计算后的尺寸
        
        @Override
        public int getCount() {
            return puzzleList.size();
        }

        @Override
        public Object getItem(int position) {
            return puzzleList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ImageView iv;
            if (convertView != null && convertView instanceof ImageView) {
                iv = (ImageView) convertView;
            } else {
                iv = new ImageView(PuzzleActivity.this);
            }
            
            // 动态计算每个拼图块的尺寸，确保行列间距一致
            if (itemSize < 0) {
                // GridView 的可用宽度 = 总宽度 - padding - (列数-1)*间距
                int gridWidth = parent.getWidth();
                if (gridWidth <= 0) {
                    // 如果还没有测量，使用默认值
                    gridWidth = (int) (300 * getResources().getDisplayMetrics().density);
                }
                int padding = gridPuzzle.getPaddingLeft() + gridPuzzle.getPaddingRight();
                int spacing = (int) (4 * getResources().getDisplayMetrics().density); // 4dp 间距
                int totalSpacing = spacing * (gridSize - 1);
                itemSize = (gridWidth - padding - totalSpacing) / gridSize;
            }
            
            iv.setLayoutParams(new GridView.LayoutParams(itemSize, itemSize));
            iv.setScaleType(ImageView.ScaleType.FIT_XY);
            
            // 设置图片
            int resId = puzzleRes[puzzleList.get(position)];
            iv.setImageResource(resId);
            
            // 空白块做半透明处理，方便识别
            if (puzzleList.get(position) == totalCount - 1) {
                iv.setAlpha(0.2f);
            } else {
                iv.setAlpha(1f);
            }
            return iv;
        }
    }
}