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

public class MultiTouchView1ByDQ extends View {

    private static int IMAGE_WIDTH = (int) Utils.dpToPx(200);

    Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    Bitmap bitmap;

    // 绘制 bitmap 的偏移
    float offsetX, offsetY;
    // 手指按下的坐标, 计算正确的偏移量
    float downX, downY;
    // 第二次滑动时, 需要加上旧的偏移量
    float originalOffsetX, originalOffsetY;
    // 当前追踪的 point (相当于, 以哪一个手指为主)
    int trackingPointerId;

    public MultiTouchView1ByDQ(Context context, @Nullable AttributeSet attrs) {
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
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN: // 第一个 point 按下
                trackingPointerId = event.getPointerId(0);
                downX = event.getX(); // 默认获得第一个 point 的 x
                downY = event.getY();
                originalOffsetX = offsetX;
                originalOffsetY = offsetY;
                break;
            case MotionEvent.ACTION_POINTER_DOWN: // 其他 point 按下
                int actionIndex = event.getActionIndex(); // 这个方法只有在 down-up 才有效, 获取活动的 point-index
                trackingPointerId = event.getPointerId(actionIndex); // 根据 point-index 获得 point-id
                downX = event.getX(actionIndex); // 根据 id 获取新的 downX, 意为用新的 point 替代旧的
                downY = event.getY(actionIndex);
                originalOffsetX = offsetX;
                originalOffsetY = offsetY;
                break;
            case MotionEvent.ACTION_MOVE: // 移动 更新offset 刷新
                int index = event.findPointerIndex(trackingPointerId);
                offsetX = event.getX(index) - downX + originalOffsetX;
                offsetY = event.getY(index) - downY + originalOffsetY;
                invalidate();
                break;
            case MotionEvent.ACTION_POINTER_UP: // 其他 point 抬起
                actionIndex = event.getActionIndex(); // 获取活动的 point-index
                int pointerId = event.getPointerId(actionIndex); // 根据 point-index 获得 point-id
                if (pointerId == trackingPointerId) { // 判断是不是正在追踪的 point-id
                    int newIndex;
                    if (actionIndex == event.getPointerCount() - 1) { // 如果是取倒数第一个
                        newIndex = event.getPointerCount() - 2; // 取倒数第二个
                    } else {
                        newIndex = event.getPointerCount() - 1; // 取倒数第一个
                    }
                    trackingPointerId = event.getPointerId(newIndex); // 记录新的 追踪point
                    downX = event.getX();
                    downY = event.getY();
                    originalOffsetX = offsetX;
                    originalOffsetY = offsetY;
                    // TODO: 2019-11-12 这里会出现一个小问题描述如下: point1(down-move-up,down-move-up), point2(down-move), 会出现跳跃
                }
                break;
            default:
                break;
        }
        return true;
    }
}