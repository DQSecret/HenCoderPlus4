package com.hencoder.a18_scalable_image_view.view

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.widget.OverScroller
import com.hencoder.a18_scalable_image_view.dp
import com.hencoder.a18_scalable_image_view.getAvatar
import kotlin.math.max
import kotlin.math.min

/**
 * 1. drawBitmap
 * 2. 居中
 * 3. 缩放
 * 4. 双击
 * 5. 动画
 * 6. 手指移动(canvas逆向思维?没懂)
 * 7. 惯性滑动
 * 8. 缩小时回到中心
 * 9. 手指点击中心,放大
 */
class ScalableImageViewByDQ(context: Context?, attrs: AttributeSet?) : View(context, attrs) {

    companion object {
        private val IMAGE_WIDTH = 300.dp()
        private const val OVER_SCALE_FACTOR = 1.5f
    }

    private val paint: Paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val bitmap: Bitmap = resources.getAvatar(IMAGE_WIDTH)

    // 起始位置偏移
    private var originalOffsetX = 0.0f
    private var originalOffsetY = 0.0f
    // 双击缩放
    private var isBig = false
    private var smallScale = 0.0f
    private var bigScale = 0.0f
    // 动画需要
    private var currentScale = 0.0f
        set(value) {
            field = value
            invalidate()
        }
    private val scaleAnimator: ObjectAnimator = ObjectAnimator.ofFloat(this, "currentScale", smallScale, bigScale)
        get() {
            field.setFloatValues(smallScale, bigScale)
            return field
        }

    // 触摸移动偏移,及最大偏移
    private var offsetX = 0f
    private var offsetY = 0f
    private var maxOffsetX = 0f
    private var maxOffsetY = 0f
    // 惯性滑动: OverScroller <-> Scroller(会忽略手指离屏的最后速度)
    private var scroller = OverScroller(context)

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        originalOffsetX = (width - bitmap.width) / 2f
        originalOffsetY = (height - bitmap.height) / 2f

        smallScale = min(width / bitmap.width.toFloat(), height / bitmap.height.toFloat())
        bigScale = max(width / bitmap.width.toFloat(), height / bitmap.height.toFloat()) * OVER_SCALE_FACTOR
        currentScale = smallScale

        maxOffsetX = (bitmap.width * bigScale - width) / 2f
        maxOffsetY = (bitmap.height * bigScale - height) / 2f
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        canvas?.run {
            // 让偏移量受scaleFraction的影响即可解决, 缩小时不回到中心的位置
            val scaleFraction = (currentScale - smallScale) / (bigScale - smallScale)
            canvas.translate(offsetX * scaleFraction, offsetY * scaleFraction)
            canvas.scale(currentScale, currentScale, width / 2f, height / 2f)
            drawBitmap(bitmap, originalOffsetX, originalOffsetY, paint)
        }
    }

    /**
     * 使用自己的触摸反馈算法,替代原有的
     */
    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        // 这里太暴力了, 需要自己思考, 怎么区分两种场景
        var result = multiScaleDetector.onTouchEvent(event)
        if (!multiScaleDetector.isInProgress) {
            result = detector.onTouchEvent(event)
        }
        return result
    }

    /**
     * 边界判断
     */
    private fun fixOffsets() {
        offsetX = min(maxOffsetX, max(-maxOffsetX, offsetX))
        offsetY = min(maxOffsetY, max(-maxOffsetY, offsetY))
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    /// 代码分离
    ////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * 把事件处理的逻辑抽离出来
     */
    private val detector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
        override fun onDown(e: MotionEvent?): Boolean {
            // 拦截事件流
            return true
        }

        override fun onDoubleTap(e: MotionEvent?): Boolean {
            isBig = !isBig
            if (isBig) {
                e?.run {
                    offsetX = (e.x - width / 2f) * (1 - bigScale / smallScale)
                    offsetY = (e.y - height / 2f) * (1 - bigScale / smallScale)
                    fixOffsets()
                }
                scaleAnimator.start()
            } else {
                scaleAnimator.reverse()
            }
            return super.onDoubleTap(e)
        }

        override fun onScroll(e1: MotionEvent?, e2: MotionEvent?, distanceX: Float, distanceY: Float): Boolean {
            if (isBig) {
                // 偏移量: distanceX = old-new, 每次的小移动, 所以要累积, 而且相反
                offsetX -= distanceX
                offsetY -= distanceY
                fixOffsets()
                invalidate()
            }
            return super.onScroll(e1, e2, distanceX, distanceY)
        }

        override fun onFling(e1: MotionEvent?, e2: MotionEvent?, velocityX: Float, velocityY: Float): Boolean {
            if (isBig) {
                scroller.fling(
                        offsetX.toInt(), offsetY.toInt(), velocityX.toInt(), velocityY.toInt(),
                        -maxOffsetX.toInt(), maxOffsetX.toInt(), -maxOffsetY.toInt(), maxOffsetY.toInt(),
                        50.dp(), 50.dp() // 回弹效果 - 最大回弹的距离
                )
                // 下一帧动画
                postOnAnimation { refresh() }
            }
            return super.onFling(e1, e2, velocityX, velocityY)
        }

        /**
         * 自己调用自己, 多帧动画
         */
        fun refresh() {
            if (scroller.computeScrollOffset()) { // 返回值代表计算结束
                offsetX = scroller.currX.toFloat()
                offsetY = scroller.currY.toFloat()
                invalidate()
                postOnAnimation { refresh() }
            }
        }
    })

    /**
     * 双指缩放
     * ps: 1-在 onTouch() 区分两个场景; 2-随手指中心放大缩小
     */
    private val multiScaleDetector = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        // 获得初始比例
        private var initialCurrentScale = 1f

        override fun onScaleBegin(detector: ScaleGestureDetector?): Boolean {
            initialCurrentScale = currentScale
            return super.onScaleBegin(detector)
        }

        /**
         * 双指滑动过程中, detector.scaleFactor(倍数): 缩小时(0 ~ 1], 放大时[1 ~ ∞)
         * 表示的是一个, 具体倍数
         * 但是, 之前的 scaleFraction 是一个动画完成度, [0 ~ 1], 这两者没有直接关系,
         * 所以使用 currentScale 来桥接两者
         */
        override fun onScale(detector: ScaleGestureDetector?): Boolean {
            detector?.run {
                currentScale = initialCurrentScale * detector.scaleFactor
                invalidate()
            }
            return super.onScale(detector)
        }
    })
}