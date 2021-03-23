package com.wizt.utils

import android.content.Context
import android.support.v7.widget.RecyclerView
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View

class MyRecyclerView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyle: Int = 0) :
    RecyclerView(context, attrs, defStyle) {
    private var mScrollable: Boolean = false
    var isAnimated = false

    init {
        mScrollable = false
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        return !mScrollable || super.dispatchTouchEvent(ev)
    }

    override protected fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        super.onLayout(changed, l, t, r, b)
        if (isAnimated) return

        isAnimated = true
        for (i in 0 until getChildCount()) {
            animate(getChildAt(i), i)

            if (i == getChildCount() - 1) {
                getHandler().postDelayed(Runnable { mScrollable = true }, i * 100L)
            }
        }
    }

    private fun animate(view: View, pos: Int) {
        view.animate().cancel()
        view.translationY = 100f
        view.alpha = 0f
        view.animate().alpha(1.0f).translationY(0f).setDuration(300).startDelay = (pos * 100).toLong()
    }
}