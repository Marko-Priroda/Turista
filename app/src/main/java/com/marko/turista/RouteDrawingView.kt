package com.marko.turista

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View

/** Only active in explicit drawing mode; normal map gestures remain available otherwise. */
class RouteDrawingView(context: Context, attrs: AttributeSet? = null): View(context, attrs) {
    var onPoint: ((Float, Float) -> Unit)? = null
    var onStrokeStart: (() -> Unit)? = null
    var onStrokeCancel: (() -> Unit)? = null
    private var cancelled=false
    private var x = 0f
    private var y = 0f
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when(event.actionMasked) {
            MotionEvent.ACTION_DOWN -> { cancelled=false;onStrokeStart?.invoke(); x=event.x; y=event.y; onPoint?.invoke(x,y) }
            MotionEvent.ACTION_POINTER_DOWN -> {if(!cancelled) onStrokeCancel?.invoke();cancelled=true}
            MotionEvent.ACTION_MOVE -> if(!cancelled && event.pointerCount == 1 && kotlin.math.hypot(event.x-x,event.y-y) >= 8*resources.displayMetrics.density) {
                x=event.x; y=event.y; onPoint?.invoke(x,y)
            }
            MotionEvent.ACTION_UP -> { if(!cancelled) onPoint?.invoke(event.x,event.y); performClick() }
            MotionEvent.ACTION_CANCEL -> {if(!cancelled) onStrokeCancel?.invoke();cancelled=true}
        }
        return true
    }
    override fun performClick(): Boolean { super.performClick(); return true }
}
