package com.example.ankillmtooltip

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.WindowManager
import android.widget.TextView

/**
 * Owns a single floating bubble drawn as a TYPE_ACCESSIBILITY_OVERLAY
 * window (the layer type an AccessibilityService is allowed to add
 * without a separate SYSTEM_ALERT_WINDOW grant). Mirrors the dark
 * tooltip styling from the original desktop addon's CSS_TEMPLATE.
 */
object OverlayController {

    private var windowManager: WindowManager? = null
    private var bubble: TextView? = null

    private fun ensureBubble(ctx: Context): TextView {
        bubble?.let { return it }
        val wm = ctx.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        windowManager = wm

        val bg = GradientDrawable().apply {
            setColor(Color.parseColor("#1B1B1F"))
            cornerRadius = 24f
            setStroke(2, Color.parseColor("#3A3A40"))
        }
        val tv = TextView(ctx).apply {
            setTextColor(Color.parseColor("#F0F0F2"))
            textSize = 14f
            setPadding(28, 20, 28, 20)
            background = bg
            elevation = 16f
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            android.graphics.PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 60
            y = 200
            width = 720 // px; roughly matches the 420dp max-width from the original CSS
        }

        wm.addView(tv, params)
        bubble = tv
        return tv
    }

    fun showLoading(ctx: Context) {
        ensureBubble(ctx).text = "Explaining…"
    }

    fun showResult(ctx: Context, text: String) {
        ensureBubble(ctx).text = text
    }

    fun showError(ctx: Context, message: String) {
        ensureBubble(ctx).apply {
            text = message
            setTextColor(Color.parseColor("#FF8080"))
        }
    }

    fun hide() {
        val wm = windowManager ?: return
        val b = bubble ?: return
        try {
            wm.removeView(b)
        } catch (_: Exception) {
        }
        bubble = null
    }
}
