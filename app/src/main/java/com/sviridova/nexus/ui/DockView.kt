package com.sviridova.nexus.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import com.sviridova.nexus.util.withAlpha

/**
 * Стеклянная «капсула» дока с двумя неоновыми линиями.
 */
class DockBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val rect = RectF()

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val w = MeasureSpec.getSize(widthMeasureSpec)
        val d = resources.displayMetrics.density
        setMeasuredDimension(w, resolveSize((92 * d).toInt(), heightMeasureSpec))
    }

    override fun onDraw(canvas: Canvas) {
        val d = resources.displayMetrics.density
        val accent = NexusTheme.accent(context)
        val radius = height / 2f
        rect.set(1 * d, 1 * d, width.toFloat() - d, height.toFloat() - d)

        paint.color = Color.argb(46, 255, 255, 255)
        canvas.drawRoundRect(rect, radius, radius, paint)
        paint.color = Color.argb(46, 5, 10, 18)
        canvas.drawRoundRect(rect, radius, radius, paint)

        stroke.color = withAlpha(accent, 60)
        stroke.strokeWidth = 1.2f * d
        canvas.drawRoundRect(rect, radius, radius, stroke)

        // Неоновая линия сверху.
        val gw = width * 0.42f
        val gx = (width - gw) / 2f
        paint.shader = android.graphics.LinearGradient(
            gx, 0f, gx + gw, 0f,
            intArrayOf(withAlpha(accent, 0), withAlpha(accent, 190), withAlpha(accent, 0)),
            null, android.graphics.Shader.TileMode.CLAMP
        )
        canvas.drawRoundRect(gx, 1.4f * d, gx + gw, 2.8f * d, 1.4f * d, 1.4f * d, paint)
        paint.shader = null

        // Точки-маркеры пагинации.
        val dots = 3
        val dotY = height.toFloat() - 7f * d
        for (i in 0 until dots) {
            paint.color = if (i == 0) withAlpha(accent, 230) else withAlpha(accent, 56)
            canvas.drawCircle(width / 2f + (i - (dots - 1) / 2f) * 9f * d, dotY, (if (i == 0) 2.2f else 1.5f) * d, paint)
        }
    }
}
