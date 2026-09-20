package com.marko.turista

import android.view.View
import android.app.Activity
import android.graphics.Color
import android.os.Build
import android.view.WindowInsets

/** Keep interactive content outside status/navigation bars, including Android 15+. */
object ScreenInsets {
    @Suppress("DEPRECATION")
    fun applyTo(root: View) {
        (root.context as? Activity)?.window?.let { window ->
            window.statusBarColor = Color.TRANSPARENT
            window.navigationBarColor = Color.rgb(243, 240, 232)
            window.decorView.systemUiVisibility = window.decorView.systemUiVisibility or
                View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        }
        val left = root.paddingLeft
        val top = root.paddingTop
        val right = root.paddingRight
        val bottom = root.paddingBottom
        root.setOnApplyWindowInsetsListener { view, insets ->
            val cutout = insets.displayCutout
            if (Build.VERSION.SDK_INT >= 30) {
                val bars = insets.getInsets(WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout() or WindowInsets.Type.ime())
                view.setPadding(left + bars.left, top + bars.top, right + bars.right, bottom + bars.bottom)
            } else {
                view.setPadding(
                    left + maxOf(insets.systemWindowInsetLeft, cutout?.safeInsetLeft ?: 0),
                    top + maxOf(insets.systemWindowInsetTop, cutout?.safeInsetTop ?: 0),
                    right + maxOf(insets.systemWindowInsetRight, cutout?.safeInsetRight ?: 0),
                    bottom + maxOf(insets.systemWindowInsetBottom, cutout?.safeInsetBottom ?: 0)
                )
            }
            insets
        }
        root.requestApplyInsets()
    }
}
