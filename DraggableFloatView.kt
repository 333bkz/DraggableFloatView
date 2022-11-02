package com.general.view

import android.animation.Animator
import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.core.view.children
import androidx.core.view.isVisible
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class DraggableFloatView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : LinearLayoutCompat(context, attrs, defStyleAttr), Animator.AnimatorListener {

    companion object {
        private const val DURATION_MILLIS: Long = 250L
    }

    private var widgetDX = 0f
    private var widgetInitialX = 0f
    private var widgetInitialY = 0f
    private var widgetDY = 0f
    private val marginTop = 0f
    private val marginBottom = 0f
    private val array = intArrayOf(0, 0)
    private var state = State.EXPAND //默认展开
    private var isMove = false
    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
    private var canHide = false
    private val hideHandler = object : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            if (canHide) {
                val viewParent = parent as View
                val parentWidth = viewParent.width
                val xMiddle = parentWidth / 2
                val xMax = parentWidth - width
                if (x >= xMiddle) {
                    animate()
                        .x(xMax + width * 2 / 3f)
                        .setDuration(DURATION_MILLIS)
                        .setListener(this@DraggableFloatView)
                        .start()
                } else {
                    animate()
                        .x(-width * 2 / 3f)
                        .setDuration(DURATION_MILLIS)
                        .setListener(this@DraggableFloatView)
                        .start()
                }
            }
        }
    }

    private enum class State {
        EXPAND, COLLAPSE, SETTING,
    }

    override fun onInterceptTouchEvent(event: MotionEvent?) = true

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if (event == null) {
            return super.onTouchEvent(event)
        }
        val viewParent = parent as View
        val parentHeight = viewParent.height
        val parentWidth = viewParent.width
        val xMax = parentWidth.toFloat() - width
        val xMiddle = parentWidth / 2
        val yMax = parentHeight - height - marginBottom
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                widgetDX = x - event.rawX
                widgetDY = y - event.rawY
                widgetInitialX = x
                widgetInitialY = y
                isMove = false
                hideHandler.removeMessages(0)
            }
            MotionEvent.ACTION_MOVE -> {
                var newX = event.rawX + widgetDX
                var newY = event.rawY + widgetDY
                newX = max(if (state == State.COLLAPSE) -width * 2 / 3f else 0f, newX)
                newX = min(if (state == State.COLLAPSE) xMax + width * 2 / 3f else xMax, newX)
                newY = max(marginTop, newY)
                newY = min(yMax, newY)
                if (abs(newX - widgetInitialX) > touchSlop || abs(newY - widgetInitialY) > touchSlop) {
                    if (state == State.EXPAND) {
                        x = newX
                        y = newY
                    }
                    isMove = true
                }
            }
            MotionEvent.ACTION_UP -> {
                if (state == State.EXPAND) {
                    if (event.rawX >= xMiddle) {
                        animate().x(xMax).setDuration(DURATION_MILLIS).setListener(this).start()
                    } else {
                        animate().x(0f).setDuration(DURATION_MILLIS).setListener(this).start()
                    }
                    if (!isMove && x == widgetInitialX && y == widgetInitialY) {
                        children.run breaking@{
                            forEach {
                                if (it.isVisible) {
                                    it.getLocationInWindow(array)
                                    val maxX = it.measuredWidth + array[0]
                                    val maxY = it.measuredHeight + array[1]
                                    if (event.rawX.toInt() in array[0]..maxX && event.rawY.toInt() in array[1]..maxY) {
                                        it.performClick()
                                        return@breaking
                                    }
                                }
                            }
                        }
                    }
                } else if (state == State.COLLAPSE && !isMove) {
                    if (event.rawX >= xMiddle) {
                        animate().x(xMax).setDuration(DURATION_MILLIS).setListener(this).start()
                    } else {
                        animate().x(0f).setDuration(DURATION_MILLIS).setListener(this).start()
                    }
                }
                hideHandler.sendEmptyMessageDelayed(0, 3_000)
            }
            else -> return false
        }
        return true
    }

    override fun onAnimationStart(animation: Animator?) {
        state = State.SETTING
    }

    override fun onAnimationEnd(animation: Animator?) {
        val viewParent = parent as View
        val parentWidth = viewParent.width
        val xMax = parentWidth.toFloat() - width
        state = if (x == xMax || x == 0f) State.EXPAND else State.COLLAPSE
    }

    override fun onAnimationCancel(animation: Animator?) {
        state = State.COLLAPSE
    }

    override fun onAnimationRepeat(animation: Animator?) {}

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        hideHandler.removeCallbacksAndMessages(null)
        animate().cancel()
    }

    fun setHideEnable(value: Boolean) {
        if (canHide != value) {
            canHide = value
            if (value) {
                hideHandler.removeMessages(0)
                hideHandler.sendEmptyMessageDelayed(0, 3_000)
            }
        }
    }

    fun expand() {
        if (state == State.COLLAPSE) {
            if (x < 0f) {
                animate().x(0f).setDuration(DURATION_MILLIS).setListener(this).start()
            } else {
                val viewParent = parent as View
                val parentWidth = viewParent.width
                val xMax = parentWidth.toFloat() - width
                animate().x(xMax).setDuration(DURATION_MILLIS).setListener(this).start()
            }
        }
    }
}
