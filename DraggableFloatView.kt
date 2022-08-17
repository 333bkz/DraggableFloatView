package com.general.view

import android.animation.Animator
import android.annotation.SuppressLint
import android.content.Context
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
    private var state = State.COLLAPSE
    private var isMove = false
    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop

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
                handler.removeCallbacksAndMessages(null)
            }
            MotionEvent.ACTION_MOVE -> {
                var newX = event.rawX + widgetDX
                var newY = event.rawY + widgetDY
                newX = max(if (state == State.COLLAPSE) -width / 2f else 0f, newX)
                newX = min(if (state == State.COLLAPSE) xMax + width / 2 else xMax, newX)
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
                        children.forEach {
                            if (it.isVisible) {
                                it.getLocationInWindow(array)
                                val maxX = it.measuredWidth + array[0]
                                val maxY = it.measuredHeight + array[1]
                                if (event.rawX.toInt() in array[0]..maxX && event.rawY.toInt() in array[1]..maxY) {
                                    it.performClick()
                                    return@forEach
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
                handler.postDelayed(hideRunnable, 3_000)
            }
            else -> return false
        }
        return true
    }

    private val hideRunnable = Runnable {
        val viewParent = parent as View
        val parentWidth = viewParent.width
        val xMiddle = parentWidth / 2
        val xMax = parentWidth - width
        if (x >= xMiddle) {
            animate().x(xMax + width / 2f).setDuration(DURATION_MILLIS).setListener(this).start()
        } else {
            animate().x(-width / 2f).setDuration(DURATION_MILLIS).setListener(this).start()
        }
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
        handler.removeCallbacksAndMessages(null)
        animate().cancel()
    }
}