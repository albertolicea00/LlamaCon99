package com.llamacon99.monitor

import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.text.TextUtils
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import com.llamacon99.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** Optional identity card above other apps while ringing; never handles or answers a call. */
@Singleton
class CallerIdOverlay @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val windowManager = context.getSystemService(WindowManager::class.java)
    private val handler = Handler(Looper.getMainLooper())
    private var card: View? = null
    private val timeout = Runnable { dismiss() }

    fun canShow(): Boolean = Settings.canDrawOverlays(context)

    fun show(name: String, localNumber: String) = show(name, localNumber, 120_000L)

    fun show(name: String, localNumber: String, autoDismissMs: Long) {
        dismiss()
        if (!canShow()) {
            Log.i("CallerIdOverlay", "Floating card skipped: display-over-other-apps permission missing")
            return
        }
        val dark = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
        val foreground = if (dark) Color.WHITE else Color.rgb(6, 33, 10)
        val background = if (dark) {
            Color.argb(228, 0, 0, 0)
        } else {
            Color.argb(228, 255, 255, 255)
        }
        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(12), dp(20), dp(20))
            this.background = GradientDrawable().apply {
                setColor(background)
                cornerRadius = dp(24).toFloat()
            }
            elevation = dp(8).toFloat()
        }
        val header = LinearLayout(context).apply { gravity = Gravity.CENTER_VERTICAL }
        header.addView(label(context.getString(R.string.caller_id_overlay_label), 13f, foreground),
            LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        header.addView(Button(context).apply {
            text = "×"
            textSize = 22f
            setTextColor(foreground)
            this.background = null
            contentDescription = context.getString(R.string.caller_id_overlay_dismiss)
            setOnClickListener { dismiss() }
        }, LinearLayout.LayoutParams(dp(48), dp(48)))
        root.addView(header)
        root.addView(label(name, 24f, foreground).apply { setTypeface(typeface, Typeface.BOLD) })
        root.addView(label(context.getString(R.string.caller_id_phone_number, localNumber), 20f, foreground))
        val params = WindowManager.LayoutParams(
            (context.resources.displayMetrics.widthPixels - dp(32)).coerceAtMost(dp(480)),
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            val position = context.getSharedPreferences(POSITION_PREFERENCES, Context.MODE_PRIVATE)
            x = position.getInt(KEY_X, 0)
            y = position.getInt(KEY_Y, dp(88))
        }
        val screenWidth = context.resources.displayMetrics.widthPixels
        val screenHeight = context.resources.displayMetrics.heightPixels
        fun keepOnScreen() {
            val width = root.width.takeIf { it > 0 } ?: params.width
            val height = root.height.takeIf { it > 0 } ?: dp(180)
            val maxHorizontalOffset = ((screenWidth - width) / 2).coerceAtLeast(0)
            params.x = params.x.coerceIn(-maxHorizontalOffset, maxHorizontalOffset)
            params.y = params.y.coerceIn(0, (screenHeight - height).coerceAtLeast(0))
        }
        var downRawX = 0f
        var downRawY = 0f
        var downX = 0
        var downY = 0
        var moved = false
        root.setOnTouchListener { view, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    downRawX = event.rawX
                    downRawY = event.rawY
                    downX = params.x
                    downY = params.y
                    moved = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val deltaX = (event.rawX - downRawX).toInt()
                    val deltaY = (event.rawY - downRawY).toInt()
                    val touchSlop = android.view.ViewConfiguration.get(context).scaledTouchSlop
                    if (!moved && (kotlin.math.abs(deltaX) > touchSlop || kotlin.math.abs(deltaY) > touchSlop)) {
                        moved = true
                    }
                    if (moved) {
                        params.x = downX + deltaX
                        params.y = downY + deltaY
                        keepOnScreen()
                        try {
                            windowManager.updateViewLayout(view, params)
                        } catch (error: IllegalArgumentException) {
                            Log.w("CallerIdOverlay", "Could not move caller ID card", error)
                        }
                    }
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    if (moved) {
                        context.getSharedPreferences(POSITION_PREFERENCES, Context.MODE_PRIVATE)
                            .edit()
                            .putInt(KEY_X, params.x)
                            .putInt(KEY_Y, params.y)
                            .apply()
                    }
                    true
                }
                else -> false
            }
        }
        try {
            windowManager.addView(root, params)
            card = root
            root.post {
                keepOnScreen()
                try {
                    windowManager.updateViewLayout(root, params)
                } catch (error: IllegalArgumentException) {
                    Log.w("CallerIdOverlay", "Could not restore caller ID card position", error)
                }
            }
            Log.i("CallerIdOverlay", "Floating caller ID card shown")
            // Safety net if a device fails to deliver the end-of-ringing callback.
            handler.postDelayed(timeout, autoDismissMs)
        } catch (error: SecurityException) {
            Log.w("CallerIdOverlay", "Overlay permission unavailable", error)
        } catch (error: WindowManager.BadTokenException) {
            Log.w("CallerIdOverlay", "Overlay window unavailable", error)
        }
    }

    fun dismiss() {
        handler.removeCallbacks(timeout)
        val current = card ?: return
        card = null
        try {
            windowManager.removeView(current)
        } catch (_: IllegalArgumentException) {
            // The system may already have removed it after permission was revoked.
        }
    }

    private fun label(value: String, size: Float, color: Int) = TextView(context).apply {
        text = value
        textSize = size
        setTextColor(color)
        maxLines = 2
        ellipsize = TextUtils.TruncateAt.END
    }

    private fun dp(value: Int) = (value * context.resources.displayMetrics.density).toInt()

    private companion object {
        const val POSITION_PREFERENCES = "caller_id_overlay_position"
        const val KEY_X = "x"
        const val KEY_Y = "y"
    }
}
