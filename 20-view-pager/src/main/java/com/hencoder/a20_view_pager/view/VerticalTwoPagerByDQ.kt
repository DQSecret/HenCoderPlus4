package com.hencoder.a20_view_pager.view

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.widget.OverScroller
import androidx.core.view.children
import kotlin.math.abs

/**
 * 自己实现的纵向惯性滑动的 ViewGroup
 */
class VerticalTwoPagerByDQ(context: Context?, attrs: AttributeSet?) : ViewGroup(context, attrs) {

    // 按下的
    private var downX = 0f
    private var downY = 0f
    // 已经滑动的
    private var downScrollY = 0

    // 当前是否在滑动: 用于对 父子view 的拦截
    private var isScrolling = false
    private var viewConfiguration = ViewConfiguration.get(context)

    // 滑动距离超过半屏, 翻页
    private val overScroller = OverScroller(context)
    // 速度追踪器: 初始速度会影响滑动结果
    private val velocityTracker = VelocityTracker.obtain()
    private val maxVelocity = viewConfiguration.scaledMaximumFlingVelocity.toFloat()
    private val minVelocity = viewConfiguration.scaledMinimumFlingVelocity.toFloat()

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        // 直接把自己的全部限制,给子view
        measureChildren(widthMeasureSpec, heightMeasureSpec)
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        val childLeft = 0
        var childTop = 0
        val childRight = width
        var childBottom = height
        // 利用 layout 纵向排列子view
        children.forEach {
            it.layout(childLeft, childTop, childRight, childBottom)
            childTop += height
            childBottom += height
        }
    }

    override fun onInterceptTouchEvent(ev: MotionEvent?): Boolean {
        ev ?: return false
        var result = false
        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                isScrolling = false
                downX = ev.x
                downY = ev.y
                downScrollY = scrollY
            }
            MotionEvent.ACTION_MOVE -> {
                if (!isScrolling) {
                    // 大于最小滑动距离, 就拦截 父子view
                    val dy = abs(ev.y - downY)
                    if (dy > viewConfiguration.scaledPagingTouchSlop) {
                        isScrolling = true
                        parent.requestDisallowInterceptTouchEvent(true)
                        result = true
                    }
                }
            }
        }
        return result
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        event ?: return false

        // down 的时候清空追踪事件
        if (event.actionMasked == MotionEvent.ACTION_DOWN) {
            velocityTracker.clear()
        }
        // 其他的时候(move)添加事件追踪, 利用多个 move 的时间间隔,以及坐标变化,计算速度
        velocityTracker.addMovement(event)

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downX = event.x
                downY = event.y
                downScrollY = scrollY
            }
            MotionEvent.ACTION_MOVE -> {
                // 这里反着计算, 原因看下面
                var dy = (downY - event.y + downScrollY).toInt()
                if (dy > height) {
                    dy = height
                } else if (dy < 0) {
                    dy = 0
                }
                // 这个滑动方法通常用于 ViewGroup 滑动自己的 Canvas, 因为 子view 比较大,
                // 在视觉上, 跟普通的滑动偏移量方向相反, 所以在计算时, 需要反向计算
                scrollTo(0, dy)
            }
            MotionEvent.ACTION_UP -> {
                // 计算当前的速度, 并且设置速度上限
                velocityTracker.computeCurrentVelocity(1000, maxVelocity)
                // 获取纵向的速度
                val vy = velocityTracker.yVelocity
                // 如果速度小于快滑最小值, 则计算距离, 反之根据速度判断
                val targetPage = if (abs(vy) < minVelocity) {
                    // 如果滑动距离大于半屏,则翻到下一页
                    if (scrollY > height / 2f) 1 else 0
                } else {
                    // 方向分速度, 向上为负, 视觉向下翻页
                    if (vy < 0) 1 else 0
                }
                // 根据页码, 计算滑动距离
                val scrollDistance = if (targetPage == 1) height - scrollY else -scrollY
                overScroller.startScroll(0, scrollY, 0, scrollDistance)
                // 下一帧重绘,会触发 onDraw()
                postInvalidateOnAnimation()
            }
        }
        return true
    }

    /**
     * 在 onDraw() 中会被调用
     */
    override fun computeScroll() {
        if (overScroller.computeScrollOffset()) {
            scrollTo(overScroller.currX, overScroller.currY)
            postInvalidateOnAnimation()
        }
    }
}