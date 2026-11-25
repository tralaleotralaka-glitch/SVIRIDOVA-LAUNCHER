package com.sviridova.nexus.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import com.sviridova.nexus.R
import com.sviridova.nexus.util.withAlpha

/** Текстовый помощник в HUD-стиле. */
fun hudText(
    context: Context,
    text: String,
    sizeSp: Float,
    color: Int,
    font: Int = R.font.rajdhani_semibold,
    letterSpacing: Float = 0.16f,
    upper: Boolean = true
): TextView = TextView(context).apply {
    this.text = if (upper) text.uppercase() else text
    textSize = sizeSp
    setTextColor(color)
    typeface = ResourcesCompat.getFont(context, font)
    includeFontPadding = false
    this.letterSpacing = letterSpacing
}

/** Строка поиска-капсула. */
class SearchPill @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = NexusTheme.regularTypeface(context)
    }
    private val rect = RectF()
    var hint: String = context.getString(R.string.search_label)
        set(value) { field = value; invalidate() }

    init {
        setWillNotDraw(false)
        isClickable = true
        isFocusable = true
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val w = MeasureSpec.getSize(widthMeasureSpec)
        val d = resources.displayMetrics.density
        setMeasuredDimension(w, resolveSize((44 * d).toInt(), heightMeasureSpec))
    }

    override fun onDraw(canvas: Canvas) {
        val d = resources.displayMetrics.density
        val accent = NexusTheme.accent(context)
        val r = height / 2f
        rect.set(0f, 0f, width.toFloat(), height.toFloat())

        paint.color = Color.argb(40, 255, 255, 255)
        canvas.drawRoundRect(rect, r, r, paint)
        paint.color = Color.argb(40, 5, 10, 18)
        canvas.drawRoundRect(rect, r, r, paint)
        stroke.color = withAlpha(accent, if (isPressed) 160 else 50)
        stroke.strokeWidth = 1.2f * d
        canvas.drawRoundRect(rect, r, r, stroke)

        // Лупа.
        val lx = 20 * d
        val ly = height / 2f
        stroke.color = withAlpha(accent, 220)
        stroke.strokeWidth = 1.8f * d
        canvas.drawCircle(lx, ly - 1.5f * d, 5.5f * d, stroke)
        canvas.drawLine(lx + 4.2f * d, ly + 3f * d, lx + 8f * d, ly + 7f * d, stroke)

        textPaint.textSize = 12.5f * d
        textPaint.color = NexusTheme.SECONDARY
        textPaint.letterSpacing = 0.14f
        canvas.drawText(hint, 36 * d, ly - (textPaint.descent() + textPaint.ascent()) / 2f, textPaint)

        // Правый маркер.
        paint.color = withAlpha(accent, 130)
        canvas.drawCircle(width - 20 * d, ly, 2f * d, paint)
    }
}

/** Тонкая иконка-кнопка (шестерёнка, закрытие). */
class HudIconButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    enum class Glyph { GEAR, CLOSE, CHECK }

    var glyph: Glyph = Glyph.GEAR
        set(value) { field = value; invalidate() }

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    init {
        isClickable = true
        isFocusable = true
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val d = resources.displayMetrics.density
        val size = resolveSize((44 * d).toInt(), widthMeasureSpec)
        setMeasuredDimension(size, size)
    }

    override fun onDraw(canvas: Canvas) {
        val d = resources.displayMetrics.density
        val accent = NexusTheme.accent(context)
        val cx = width / 2f
        val cy = height / 2f
        paint.color = if (isPressed) withAlpha(accent, 230) else NexusTheme.SECONDARY
        paint.strokeWidth = 1.8f * d
        when (glyph) {
            Glyph.GEAR -> {
                val r = 7f * d
                canvas.drawCircle(cx, cy, r * 0.52f, paint)
                for (i in 0 until 6) {
                    val a = Math.toRadians(i * 60.0)
                    val sx = cx + r * 0.86f * Math.cos(a).toFloat()
                    val sy = cy + r * 0.86f * Math.sin(a).toFloat()
                    val ex = cx + r * 1.24f * Math.cos(a).toFloat()
                    val ey = cy + r * 1.24f * Math.sin(a).toFloat()
                    canvas.drawLine(sx, sy, ex, ey, paint)
                }
            }
            Glyph.CLOSE -> {
                val r = 6f * d
                canvas.drawLine(cx - r, cy - r, cx + r, cy + r, paint)
                canvas.drawLine(cx + r, cy - r, cx - r, cy + r, paint)
            }
            Glyph.CHECK -> {
                val path = Path()
                path.moveTo(cx - 6 * d, cy)
                path.lineTo(cx - 1.5f * d, cy + 5 * d)
                path.lineTo(cx + 7 * d, cy - 5 * d)
                canvas.drawPath(path, paint)
            }
        }
    }
}

/** Статусный маркер: точка + текст. */
class StatusDot(context: Context) : View(context) {
    var color: Int = 0xFF00F0FF.toInt()
        set(value) { field = value; invalidate() }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val d = resources.displayMetrics.density
        val s = (10 * d).toInt()
        setMeasuredDimension(s, s)
    }

    override fun onDraw(canvas: Canvas) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val d = resources.displayMetrics.density
        paint.color = withAlpha(color, 70)
        canvas.drawCircle(width / 2f, height / 2f, 3.4f * d, paint)
        paint.color = withAlpha(color, 255)
        canvas.drawCircle(width / 2f, height / 2f, 1.8f * d, paint)
    }
}


fun Context.density(value: Float): Float = value * resources.displayMetrics.density

fun Context.densityInt(value: Float): Int = (value * resources.displayMetrics.density).toInt()
