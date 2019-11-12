package com.hencoder.a19_multi_touch.view;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.Nullable;

import com.hencoder.a19_multi_touch.Utils;

public class MultiTouchView2ByDQ extends View {

    private static int IMAGE_WIDTH = (int) Utils.dpToPx(200);

    Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    Bitmap bitmap;

    // 绘制 bitmap 的偏移
    float offsetX, offsetY;
    // 手指按下的坐标, 计算正确的偏移量
    float downX, downY;
    // 第二次滑动时, 需要加上旧的偏移量
    float originalOffsetX, originalOffsetY;

    public MultiTouchView2ByDQ(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        bitmap = Utils.getAvatar(getResources(), IMAGE_WIDTH);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawBitmap(bitmap, offsetX, offsetY, paint);
    }

    /**
     * onTouchEvent: 是对于 View 来说的整个事件流
     * 多指触摸: 多个 point(x,y,index,id)
     * x,y: 坐标
     * index: 用于遍历
     * id: 用于追踪(区别)
     */
    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // 所有 point 的交点(中间)
        float sumX = 0, sumY = 0;
        boolean isPointerUp = event.getActionMasked() == MotionEvent.ACTION_POINTER_UP;
        for (int i = 0; i < event.getPointerCount(); i++) {
            if (!isPointerUp || i != event.getActionIndex()) {
                sumX += event.getX(i);
                sumY += event.getY(i);
            }
        }
        int pointerCount = event.getPointerCount();
        if (isPointerUp) pointerCount--;
        float focusX = sumX / pointerCount, focusY = sumY / pointerCount;

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_POINTER_DOWN:
            case MotionEvent.ACTION_POINTER_UP:
                downX = focusX; // 默认获得第一个 point 的 x
                downY = focusY;
                originalOffsetX = offsetX;
                originalOffsetY = offsetY;
                break;
            case MotionEvent.ACTION_MOVE: // 移动 更新offset 刷新
                offsetX = focusX - downX + originalOffsetX;
                offsetY = focusY - downY + originalOffsetY;
                invalidate();
                break;
            default:
                break;
        }
        return true;
    }
}