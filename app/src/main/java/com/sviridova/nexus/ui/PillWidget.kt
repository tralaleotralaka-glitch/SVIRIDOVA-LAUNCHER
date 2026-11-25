package com.sviridova.nexus.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import com.sviridova.nexus.util.withAlpha

/**
 * Компактная стеклянная плашка: заголовок, значение и необязательный прогресс.
 */
open class PillWidget @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    var title: String = ""
        set(value) { field = value; invalidate() }

    var value: String = ""
        set(value) { field = value; invalidate() }

    var caption: String = ""
        set(value) { field = value; invalidate() }

    var progress: Float = -1f
        set(value) { field = value; invalidate() }

    protected val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    protected val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    protected val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = NexusTheme.semiboldTypeface(context)
        letterSpacing = 0.2f
    }
    protected val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = NexusTheme.displayTypeface(context)
    }
    protected val captionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = NexusTheme.regularTypeface(context)
        letterSpacing = 0.12f
    }
    protected val rect = RectF()
    protected val accent: Int get() = NexusTheme.accent(context)

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val w = MeasureSpec.getSize(widthMeasureSpec)
        val d = resources.displayMetrics.density
        setMeasuredDimension(w, resolveSize((76 * d).toInt(), heightMeasureSpec))
    }

    override fun onDraw(canvas: Canvas) {
        val d = resources.displayMetrics.density
        val r = 18f * d
        rect.set(0f, 0f, width.toFloat(), height.toFloat())
        fillPaint.color = Color.argb(34, 255, 255, 255)
        canvas.drawRoundRect(rect, r, r, fillPaint)
        fillPaint.color = Color.argb(32, 6, 12, 20)
        canvas.drawRoundRect(rect, r, r, fillPaint)
        strokePaint.color = withAlpha(accent, 40)
        strokePaint.strokeWidth = d
        canvas.drawRoundRect(rect, r, r, strokePaint)

        // Уголки HUD.
        strokePaint.color = withAlpha(accent, 150)
        strokePaint.strokeWidth = 1.4f * d
        val corner = 13f * d
        val p = Path()
        p.moveTo(r, 0f); p.lineTo(r + corner, 0f)
        p.moveTo(0f, r); p.lineTo(0f, r + corner)
        p.moveTo(width - r - corner, height.toFloat()); p.lineTo(width - r, height.toFloat())
        p.moveTo(width.toFloat(), height - r - corner); p.lineTo(width.toFloat(), height - r)
        canvas.drawPath(p, strokePaint)

        titlePaint.textSize = 9.5f * d
        titlePaint.color = NexusTheme.TERTIARY
        canvas.drawText(title, 14 * d, 21 * d, titlePaint)

        valuePaint.textSize = 23f * d
        valuePaint.color = NexusTheme.PRIMARY
        canvas.drawText(value, 14 * d, 48 * d, valuePaint)

        if (caption.isNotEmpty()) {
            captionPaint.textSize = 10f * d
            captionPaint.color = withAlpha(accent, 215)
            canvas.drawText(caption, 14 * d, 64 * d, captionPaint)
        }

        if (progress >= 0f) {
            val top = height - 11f * d
            val left = 14 * d
            val right = width - 14 * d
            fillPaint.color = withAlpha(accent, 44)
            canvas.drawRoundRect(left, top, right, top + 2.6f * d, 2 * d, 2 * d, fillPaint)
            fillPaint.color = withAlpha(accent, 235)
            canvas.drawRoundRect(left, top, left + (right - left) * progress.coerceIn(0f, 1f), top + 2.6f * d, 2 * d, 2 * d, fillPaint)
        }
    }
}
