package com.example.reverseclock; // 包名与你的项目一致

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import androidx.annotation.Nullable;
import java.util.Random;

public class PuzzleView extends View {
    private static final int ROW = 3; // 3行
    private static final int COL = 3; // 3列
    private Bitmap[] puzzleBlocks; // 拼图块数组
    private int blockSize; // 每块尺寸
    private int emptyIndex = 8; // 空白块索引（最后一位）
    private OnPuzzleCompleteListener completeListener; // 完成回调

    // 构造方法
    public PuzzleView(Context context) {
        super(context);
        init();
    }

    public PuzzleView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    // 初始化：切割图片+打乱
    private void init() {
        // 加载原图（必须命名为puzzle_img.jpg，放在res/drawable文件夹）
        Bitmap originalBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.puzzle_img);
        blockSize = originalBitmap.getWidth() / COL; // 计算每块尺寸

        // 切割图片为3x3块
        puzzleBlocks = new Bitmap[ROW * COL];
        int index = 0;
        for (int i = 0; i < ROW; i++) {
            for (int j = 0; j < COL; j++) {
                puzzleBlocks[index] = Bitmap.createBitmap(
                        originalBitmap,
                        j * blockSize,
                        i * blockSize,
                        blockSize,
                        blockSize
                );
                index++;
            }
        }

        // 打乱拼图（确保可还原）
        shufflePuzzle();
    }

    // 打乱拼图
    private void shufflePuzzle() {
        Random random = new Random();
        for (int i = 0; i < 100; i++) { // 随机交换100次
            int randomIndex = random.nextInt(ROW * COL - 1); // 排除空白块
            if (randomIndex != emptyIndex) {
                // 交换拼图块
                Bitmap temp = puzzleBlocks[randomIndex];
                puzzleBlocks[randomIndex] = puzzleBlocks[emptyIndex];
                puzzleBlocks[emptyIndex] = temp;
                emptyIndex = randomIndex;
            }
        }
    }

    // 绘制拼图
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (puzzleBlocks == null) return;

        Paint paint = new Paint();
        int index = 0;
        for (int i = 0; i < ROW; i++) {
            for (int j = 0; j < COL; j++) {
                // 绘制每块拼图（跳过空白块）
                if (puzzleBlocks[index] != null) {
                    canvas.drawBitmap(
                            puzzleBlocks[index],
                            j * blockSize,
                            i * blockSize,
                            paint
                    );
                }
                index++;
            }
        }
    }

    // 处理触摸拖动事件
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            int x = (int) event.getX();
            int y = (int) event.getY();
            // 计算点击的拼图块索引
            int clickRow = y / blockSize;
            int clickCol = x / blockSize;
            int clickIndex = clickRow * COL + clickCol;

            // 判断是否可以与空白块交换（上下左右相邻）
            if (isAdjacent(clickIndex, emptyIndex)) {
                // 交换拼图块和空白块
                Bitmap temp = puzzleBlocks[clickIndex];
                puzzleBlocks[clickIndex] = puzzleBlocks[emptyIndex];
                puzzleBlocks[emptyIndex] = temp;
                emptyIndex = clickIndex;

                invalidate(); // 重绘界面

                // 检查拼图是否完成
                if (isPuzzleComplete()) {
                    completeListener.onComplete(); // 回调通知完成
                }
            }
        }
        return true;
    }

    // 判断两个索引是否上下左右相邻
    private boolean isAdjacent(int index1, int index2) {
        int row1 = index1 / COL;
        int col1 = index1 % COL;
        int row2 = index2 / COL;
        int col2 = index2 % COL;

        // 相邻条件：行数相同列数差1，或列数相同行数差1
        return (row1 == row2 && Math.abs(col1 - col2) == 1)
                || (col1 == col2 && Math.abs(row1 - row2) == 1);
    }

    // 检查拼图是否完成
    private boolean isPuzzleComplete() {
        for (int i = 0; i < puzzleBlocks.length - 1; i++) {
            // 前8块不为空（空白块在最后）
            if (puzzleBlocks[i] == null) {
                return false;
            }
        }
        return true;
    }

    // 拼图完成回调接口
    public interface OnPuzzleCompleteListener {
        void onComplete();
    }

    // 设置回调监听
    public void setOnPuzzleCompleteListener(OnPuzzleCompleteListener listener) {
        this.completeListener = listener;
    }

    // 重写测量方法，确保拼图是正方形
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int size = Math.min(getMeasuredWidth(), getMeasuredHeight());
        setMeasuredDimension(size, size);
    }
}