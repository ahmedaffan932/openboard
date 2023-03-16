package com.example.translatorguru.classes

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.ViewCompat


open class OverScrollBounceBehavior : CoordinatorLayout.Behavior<View?> {
    private var mOverScrollY = 0

    constructor() {}
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {}

    override fun onStartNestedScroll(
        coordinatorLayout: CoordinatorLayout,
        child: View,
        directTargetChild: View,
        target: View,
        nestedScrollAxes: Int
    ): Boolean {
        mOverScrollY = 0
        return true
    }

    override fun onNestedScroll(
        coordinatorLayout: CoordinatorLayout,
        child: View,
        target: View,
        dxConsumed: Int,
        dyConsumed: Int,
        dxUnconsumed: Int,
        dyUnconsumed: Int
    ) {
        if (dyUnconsumed == 0) {
            return
        }
        mOverScrollY -= dyUnconsumed
        val group = target as ViewGroup
        val count = group.childCount
        for (i in 0 until count) {
            val view = group.getChildAt(i)
            view.translationY = mOverScrollY.toFloat()
        }
    }

    override fun onStopNestedScroll(
        coordinatorLayout: CoordinatorLayout,
        child: View,
        target: View
    ) {
        val group = target as ViewGroup
        val count = group.childCount
        for (i in 0 until count) {
            val view = group.getChildAt(i)
            ViewCompat.animate(view).translationY(0F).start()
        }
    }
}