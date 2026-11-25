package com.sviridova.nexus.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.util.AttributeSet

/**
 * Стеклянная панель с неоновыми уголками — базовый контейнер HUD.
 */
class GlassPanel @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : android.widget.FrameLayout(context, attrs) {

    private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val corner = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeCap = Paint.Cap.SQUARE }
    private val path = Path()
    private val rect = RectF()

    var cornerRadius: Float = 20f * resources.displayMetrics.density
    var accentColor: Int = NexusTheme.accent(context)
    var showCorners: Boolean = true
    var highlightTop: Boolean = true
    var fillAlpha: Int = 30

    init {
        setWillNotDraw(false)
        isClickable = false
    }

    fun refreshAccent() {
        accentColor = NexusTheme.accent(context)
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        rect.set(0f, 0f, w, h)

        // Заливка.
        fill.shader = LinearGradient(
            0f, 0f, w, h,
            intArrayOf(Color.argb(20, 255, 255, 255), Color.argb(fillAlpha, 8, 15, 26), Color.argb(fillAlpha + 18, 3, 6, 12)),
            floatArrayOf(0f, 0.5f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, fill)
        fill.shader = null

        if (highlightTop) {
            fill.shader = LinearGradient(
                0f, 0f, 0f, h * 0.5f,
                intArrayOf(Color.argb(30, 255, 255, 255), Color.TRANSPARENT),
                floatArrayOf(0f, 1f),
                Shader.TileMode.CLAMP
            )
            canvas.drawRoundRect(rect, cornerRadius, cornerRadius, fill)
            fill.shader = null
        }

        stroke.color = Color.argb(34, Color.red(accentColor), Color.green(accentColor), Color.blue(accentColor))
        stroke.strokeWidth = 1.1f * resources.displayMetrics.density
        rect.inset(stroke.strokeWidth / 2f, stroke.strokeWidth / 2f)
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, stroke)

        if (showCorners) {
            drawCorners(canvas, rect)
        }
    }

    private fun drawCorners(canvas: Canvas, r: RectF) {
        val len = cornerRadius * 0.85f
        corner.color = Color.argb(190, Color.red(accentColor), Color.green(accentColor), Color.blue(accentColor))
        corner.strokeWidth = 1.6f * resources.displayMetrics.density
        val inset = cornerRadius
        path.reset()
        // Верхний левый.
        path.moveTo(r.left, r.top + inset + len)
        path.lineTo(r.left, r.top + inset)
        path.lineTo(r.left + inset + len, r.top)
        // Верхний правый.
        path.moveTo(r.right - inset - len, r.top)
        path.lineTo(r.right - inset, r.top)
        path.lineTo(r.right, r.top + inset)
        // Нижний правый.
        path.moveTo(r.right, r.bottom - inset - len)
        path.lineTo(r.right, r.bottom - inset)
        path.lineTo(r.right - inset - len, r.bottom)
        // Нижний левый.
        path.moveTo(r.left + inset + len, r.bottom)
        path.lineTo(r.left + inset, r.bottom)
        path.lineTo(r.left, r.bottom - inset)
        canvas.drawPath(path, corner)
    }
}
