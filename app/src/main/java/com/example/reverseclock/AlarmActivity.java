package com.example.reverseclock;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Vibrator;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import java.util.Random;

public class AlarmActivity extends AppCompatActivity implements SensorEventListener {
    private MediaPlayer mediaPlayer; // 铃声播放器
    private Vibrator vibrator; // 振动器
    private int selectedTask; // 选中的任务编号
    private int mathCorrectCount = 0; // 数学题答对次数
    private int shakeCount = 0; // 摇晃次数
    private SensorManager sensorManager; // 传感器管理（摇晃检测）
    private static final int REQUEST_CAMERA = 100; // 相机权限请求码
    private static final int REQUEST_IMAGE_CAPTURE = 101; // 拍照请求码
    private static final int REQUEST_VIBRATE = 102; // 振动权限请求码

    // 数学题相关控件
    private TextView tvMathQuestion;
    private EditText etMathAnswer;
    private Button btnCheckMath;

    // 摇晃任务相关控件
    private TextView tvShakeCount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alarm);

        // 获取传递的任务编号
        selectedTask = getIntent().getIntExtra("task", 0);

        // 初始化控件
        TextView tvAlarmTip = findViewById(R.id.tvAlarmTip);
        LinearLayout llTaskContainer = findViewById(R.id.llTaskContainer);
        Button btnFinishTask = findViewById(R.id.btnFinishTask);

        // 初始化传感器（用于摇晃检测）
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

        // 播放闹钟铃声（系统默认铃声）
        mediaPlayer = MediaPlayer.create(this, android.provider.Settings.System.DEFAULT_ALARM_ALERT_URI);
        mediaPlayer.setLooping(true); // 循环播放
        mediaPlayer.start();

        // 初始化振动器 + 检查/请求振动权限
        vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.VIBRATE) != PackageManager.PERMISSION_GRANTED) {
            // 未获取振动权限，主动请求
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.VIBRATE}, REQUEST_VIBRATE);
        } else {
            // 已获取权限，启动振动
            vibrator.vibrate(new long[]{0, 1000, 500, 1000}, 0);
        }

        // 根据任务类型显示对应的任务界面
        switch (selectedTask) {
            case 0: // 数学题任务
                tvAlarmTip.setText("连续答对3道数学题即可关闭！");
                showMathTask(llTaskContainer, btnFinishTask);
                break;
            case 1: // 摇晃任务
                tvAlarmTip.setText("摇晃手机10次即可关闭！");
                showShakeTask(llTaskContainer, btnFinishTask);
                break;
            case 2: // 拍照任务
                tvAlarmTip.setText("拍摄一张照片即可关闭！");
                showCameraTask(llTaskContainer, btnFinishTask);
                break;
            case 3: // 拼图任务
                tvAlarmTip.setText("完成3x3拼图即可关闭！");
                showPuzzleTask(llTaskContainer, btnFinishTask);
                break;
        }

        // 关闭闹钟按钮点击事件
        btnFinishTask.setOnClickListener(v -> {
            // 停止铃声
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.stop();
            }
            // 停止振动（核心修正：先检查权限再调用cancel）
            if (vibrator != null && ActivityCompat.checkSelfPermission(this, Manifest.permission.VIBRATE) == PackageManager.PERMISSION_GRANTED) {
                vibrator.cancel();
            }
            // 关闭页面
            finish();
        });
    }

    // 显示数学题任务
    private void showMathTask(LinearLayout container, Button finishBtn) {
        // 创建数学题控件
        tvMathQuestion = new TextView(this);
        tvMathQuestion.setTextSize(20f);
        tvMathQuestion.setPadding(0, 0, 0, 20);
        generateMathQuestion(); // 生成第一题

        etMathAnswer = new EditText(this);
        etMathAnswer.setHint("输入答案");
        etMathAnswer.setTextSize(18f);
        etMathAnswer.setWidth(300);

        btnCheckMath = new Button(this);
        btnCheckMath.setText("提交答案");
        btnCheckMath.setTextSize(18f);
        btnCheckMath.setPadding(15, 10, 15, 10);
        btnCheckMath.setOnClickListener(v -> checkMathAnswer(finishBtn));

        // 添加到容器
        container.addView(tvMathQuestion);
        container.addView(etMathAnswer);
        container.addView(btnCheckMath);
    }

    // 生成数学题（100以内加减法）
    private void generateMathQuestion() {
        Random random = new Random();
        int a = random.nextInt(100);
        int b = random.nextInt(100);
        boolean isAdd = random.nextBoolean();
        if (isAdd) {
            tvMathQuestion.setText(a + " + " + b + " = ?");
        } else {
            // 减法确保结果非负
            if (a < b) {
                int temp = a;
                a = b;
                b = temp;
            }
            tvMathQuestion.setText(a + " - " + b + " = ?");
        }
    }

    // 检查数学题答案
    private void checkMathAnswer(Button finishBtn) {
        String answerStr = etMathAnswer.getText().toString();
        if (answerStr.isEmpty()) {
            Toast.makeText(this, "请输入答案！", Toast.LENGTH_SHORT).show();
            return;
        }

        int userAnswer;
        try {
            userAnswer = Integer.parseInt(answerStr);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "请输入有效的数字！", Toast.LENGTH_SHORT).show();
            return;
        }

        String question = tvMathQuestion.getText().toString();
        int correctAnswer;

        // 计算正确答案
        if (question.contains("+")) {
            String[] parts = question.split(" \\+ ");
            int a = Integer.parseInt(parts[0]);
            int b = Integer.parseInt(parts[1].split(" = ")[0]);
            correctAnswer = a + b;
        } else {
            String[] parts = question.split(" - ");
            int a = Integer.parseInt(parts[0]);
            int b = Integer.parseInt(parts[1].split(" = ")[0]);
            correctAnswer = a - b;
        }

        // 验证答案
        if (userAnswer == correctAnswer) {
            mathCorrectCount++;
            Toast.makeText(this, "答对啦！已答对" + mathCorrectCount + "/3题", Toast.LENGTH_SHORT).show();
            etMathAnswer.setText("");
            if (mathCorrectCount >= 3) {
                // 完成任务，显示关闭按钮
                btnCheckMath.setEnabled(false);
                finishBtn.setVisibility(View.VISIBLE);
            } else {
                generateMathQuestion(); // 生成下一题
            }
        } else {
            Toast.makeText(this, "答错啦，再试试！", Toast.LENGTH_SHORT).show();
        }
    }

    // 显示摇晃任务
    private void showShakeTask(LinearLayout container, Button finishBtn) {
        tvShakeCount = new TextView(this);
        tvShakeCount.setText("当前摇晃次数：0/10");
        tvShakeCount.setTextSize(20f);
        container.addView(tvShakeCount);

        // 注册加速度传感器
        Sensor accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        if (accelerometer != null) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        } else {
            Toast.makeText(this, "设备不支持加速度传感器！", Toast.LENGTH_SHORT).show();
        }
    }

    // 传感器监听（摇晃检测）
    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            // 计算加速度（摇晃的判定：加速度变化超过15）
            float x = event.values[0];
            float y = event.values[1];
            float z = event.values[2];
            double acceleration = Math.sqrt(x * x + y * y + z * z) - SensorManager.GRAVITY_EARTH;
            if (acceleration > 15) {
                shakeCount++;
                tvShakeCount.setText("当前摇晃次数：" + shakeCount + "/10");
                if (shakeCount >= 10) {
                    // 完成任务，显示关闭按钮
                    sensorManager.unregisterListener(this);
                    findViewById(R.id.btnFinishTask).setVisibility(View.VISIBLE);
                }
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // 空实现，无需处理
    }

    // 显示拍照任务
    private void showCameraTask(LinearLayout container, Button finishBtn) {
        Button btnTakePhoto = new Button(this);
        btnTakePhoto.setText("点击拍照");
        btnTakePhoto.setTextSize(18f);
        btnTakePhoto.setOnClickListener(v -> {
            // 检查相机权限
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA);
                return;
            }
            // 启动相机
            Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
            } else {
                Toast.makeText(this, "设备不支持相机功能！", Toast.LENGTH_SHORT).show();
            }
        });
        container.addView(btnTakePhoto);
    }

    // 拍照结果回调
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            // 拍照成功，显示关闭按钮
            Toast.makeText(this, "拍照成功！", Toast.LENGTH_SHORT).show();
            findViewById(R.id.btnFinishTask).setVisibility(View.VISIBLE);
        }
    }

    // 显示拼图任务
    private void showPuzzleTask(LinearLayout container, Button finishBtn) {
        // 创建拼图View
        PuzzleView puzzleView = new PuzzleView(this);
        int puzzleSize = dp2px(this, 250f);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(puzzleSize, puzzleSize);
        puzzleView.setLayoutParams(layoutParams);
        container.addView(puzzleView);

        // 拼图完成回调
        puzzleView.setOnPuzzleCompleteListener(() -> {
            Toast.makeText(this, "拼图完成！", Toast.LENGTH_SHORT).show();
            finishBtn.setVisibility(View.VISIBLE);
        });
    }

    // dp转px工具方法
    private int dp2px(Context context, float dpValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);
    }

    // 权限请求结果回调（处理振动/相机权限）
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_VIBRATE) {
            // 振动权限请求结果
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // 权限获取成功，启动振动
                if (vibrator != null) {
                    vibrator.vibrate(new long[]{0, 1000, 500, 1000}, 0);
                }
            } else {
                Toast.makeText(this, "未获取振动权限，闹钟将无振动！", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == REQUEST_CAMERA) {
            // 相机权限请求结果
            if (grantResults.length > 0 && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "未获取相机权限，无法完成拍照任务！", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 释放资源（全量权限检查）
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
        if (vibrator != null && ActivityCompat.checkSelfPermission(this, Manifest.permission.VIBRATE) == PackageManager.PERMISSION_GRANTED) {
            vibrator.cancel();
        }
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }
    }
}