package com.infinity.ai.circle

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.FrameLayout
import android.graphics.drawable.GradientDrawable
import android.graphics.Color
import android.util.DisplayMetrics

/**
 * FloatingBubbleView
 *
 * Draggable floating bubble attached to WindowManager.
 * Calls [onTap] when tapped (< 8dp drag distance).
 * Remembers last X/Y position across drags.
 */
class FloatingBubbleView(
    private val context: Context,
    private val onTap: () -> Unit
) {
    private val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val root = FrameLayout(context)
    private lateinit var params: WindowManager.LayoutParams

    private var lastX = 0; private var lastY = 0
    private var downRawX = 0f; private var downRawY = 0f
    private var added = false

    fun show() {
        if (added) return
        buildView()
        wm.addView(root, params)
        added = true
    }

    fun hide() {
        if (!added) return
        runCatching { wm.removeView(root) }
        added = false
    }

    fun isShowing() = added

    private fun buildView() {
        // ── Layout params ──────────────────────────────────────────────────────
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        else
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE

        val dm = DisplayMetrics()
        @Suppress("DEPRECATION")
        wm.defaultDisplay.getMetrics(dm)

        params = WindowManager.LayoutParams(
            dpToPx(64), dpToPx(64),
            lastX.takeIf { it != 0 } ?: (dm.widthPixels - dpToPx(80)),
            lastY.takeIf { it != 0 } ?: (dm.heightPixels / 3),
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply { gravity = Gravity.TOP or Gravity.START }

        // ── Bubble visual ──────────────────────────────────────────────────────
        val bubble = ImageView(context).apply {
            val bg = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                colors = intArrayOf(Color.parseColor("#3B82F6"), Color.parseColor("#8B5CF6"))
                gradientType = GradientDrawable.LINEAR_GRADIENT
                orientation = GradientDrawable.Orientation.TL_BR
            }
            background = bg
            // Draw ∞ symbol as content description fallback
            contentDescription = "Infinity Circle Learn"
            setPadding(dpToPx(12), dpToPx(12), dpToPx(12), dpToPx(12))
        }

        // ── Elevation shadow ───────────────────────────────────────────────────
        root.elevation = dpToPx(6).toFloat()
        root.addView(bubble, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        ))

        // ── Touch handler: drag + tap ──────────────────────────────────────────
        root.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    downRawX = event.rawX; downRawY = event.rawY
                    lastX = params.x;     lastY = params.y
                }
                MotionEvent.ACTION_MOVE -> {
                    params.x = lastX + (event.rawX - downRawX).toInt()
                    params.y = lastY + (event.rawY - downRawY).toInt()
                    if (added) runCatching { wm.updateViewLayout(root, params) }
                }
                MotionEvent.ACTION_UP -> {
                    val dx = event.rawX - downRawX
                    val dy = event.rawY - downRawY
                    val moved = Math.sqrt((dx * dx + dy * dy).toDouble())
                    if (moved < dpToPx(8)) onTap()
                    // Persist final position
                    lastX = params.x; lastY = params.y
                }
            }
            true
        }
    }

    private fun dpToPx(dp: Int): Int =
        (dp * context.resources.displayMetrics.density).toInt()
}
