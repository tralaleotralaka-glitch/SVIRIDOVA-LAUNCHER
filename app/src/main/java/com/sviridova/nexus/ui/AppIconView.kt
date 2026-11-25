package com.sviridova.nexus.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View
import com.sviridova.nexus.IconShape
import com.sviridova.nexus.Prefs
import com.sviridova.nexus.model.AppEntry
import com.sviridova.nexus.util.withAlpha
import kotlin.math.min

/**
 * Ячейка сетки: иконка приложения в маске-форме, подпись и подсветка.
 *
 * Собственная отрисовка вместо ImageView/TextView даёт одинаковую
 * геометрию для всех форм масок и дешёвую RecycleView-переработку.
 */
class AppIconView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    var entry: AppEntry? = null
        set(value) {
            field = value
            invalidate()
        }

    private val iconPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val maskPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = NexusTheme.semiboldTypeface(context)
        textAlign = Paint.Align.CENTER
        letterSpacing = 0.02f
    }
    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private val maskPath = Path()
    private val rect = RectF()
    private val pressRect = RectF()
    private var pressed = false

    init {
        isClickable = true
        isFocusable = true
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val w = MeasureSpec.getSize(widthMeasureSpec)
        val d = resources.displayMetrics.density
        val h = (w * 0.86f + 22 * d).toInt()
        setMeasuredDimension(w, resolveSize(h, heightMeasureSpec))
    }

    fun bind(item: AppEntry?) {
        entry = item
    }

    override fun setPressed(pressed: Boolean) {
        this.pressed = pressed
        animate().scaleX(if (pressed) 0.88f else 1f)
            .scaleY(if (pressed) 0.88f else 1f)
            .setDuration(if (pressed) 90 else 160)
            .start()
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        val item = entry ?: return
        val d = resources.displayMetrics.density
        val accent = NexusTheme.accent(context)
        val glow = NexusTheme.glowEnabled()

        val size = min(width * 0.62f, height * 0.66f)
        val left = (width - size) / 2f
        val top = (height - size) / 2f - 6 * d
        rect.set(left, top, left + size, top + size)

        buildMask(rect, Prefs.iconShape)

        // Ореол вокруг иконки.
        if (glow) {
            glowPaint.shader = android.graphics.RadialGradient(
                rect.centerX(), rect.centerY(), size * 1.05f,
                intArrayOf(withAlpha(accent, if (pressed) 110 else 46), withAlpha(accent, 0)),
                floatArrayOf(0f, 1f),
                Shader.TileMode.CLAMP
            )
            canvas.drawCircle(rect.centerX(), rect.centerY(), size * 1.05f, glowPaint)
            glowPaint.shader = null
        }

        // Подложка под иконкой (если у приложения нет своей).
        maskPaint.color = withAlpha(0xFFFFFF, 22)
        canvas.drawPath(maskPath, maskPaint)

        val bitmap = item.icon
        if (bitmap != null && !bitmap.isRecycled) {
            canvas.save()
            canvas.clipPath(maskPath)
            val src = RectF(0f, 0f, bitmap.width.toFloat(), bitmap.height.toFloat())
            // Кадрируем иконку по маске, чтобы ячейки сетки выглядели единообразно.
            canvas.drawBitmap(bitmap, src, rect, iconPaint)
            canvas.restore()
            // Лёгкий внутренний блик, чтобы иконка читалась как объёмная.
            val sheen = Paint(Paint.ANTI_ALIAS_FLAG)
            sheen.shader = android.graphics.LinearGradient(
                rect.left, rect.top, rect.left, rect.bottom,
                intArrayOf(withAlpha(0xFFFFFF, 34), withAlpha(0xFFFFFF, 0)),
                floatArrayOf(0f, 0.55f),
                Shader.TileMode.CLAMP
            )
            canvas.save()
            canvas.clipPath(maskPath)
            canvas.drawRect(rect, sheen)
            canvas.restore()
        } else {
            // Заглушка: первая буква названия.
            val letterPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                typeface = NexusTheme.displayTypeface(context)
                textAlign = Paint.Align.CENTER
                color = withAlpha(accent, 220)
                textSize = size * 0.4f
            }
            val cy = rect.centerY() - (letterPaint.descent() + letterPaint.ascent()) / 2f
            canvas.drawText(item.label.take(1).uppercase(), rect.centerX(), cy, letterPaint)
        }

        // Обводка формы.
        strokePaint.color = withAlpha(accent, if (pressed) 220 else 70)
        strokePaint.strokeWidth = if (glow) 1.6f * d else 1f * d
        canvas.drawPath(maskPath, strokePaint)

        // Подпись.
        if (!Prefs.hideLabels) {
            labelPaint.textSize = 10.5f * d
            labelPaint.color = if (pressed) NexusTheme.PRIMARY else NexusTheme.SECONDARY
            val label = item.label
            val maxWidth = width - 10 * d
            val text = ellipsize(label, labelPaint, maxWidth)
            canvas.drawText(text, width / 2f, height - 6 * d, labelPaint)
        }

        // Индикатор нажатия.
        if (pressed) {
            pressRect.set(rect)
            pressRect.inset(-4 * d, -4 * d)
            strokePaint.color = withAlpha(accent, 140)
            canvas.drawRoundRect(pressRect, 18 * d, 18 * d, strokePaint)
        }
    }

    private fun ellipsize(text: String, paint: Paint, maxWidth: Float): String {
        if (paint.measureText(text) <= maxWidth) return text
        var cut = text
        while (cut.isNotEmpty() && paint.measureText("$cut…") > maxWidth) {
            cut = cut.dropLast(1)
        }
        return "$cut…"
    }

    private fun buildMask(r: RectF, shape: IconShape) {
        maskPath.reset()
        val radiusFactor = 0.24f
        when (shape) {
            IconShape.SQUIRCLE -> {
                maskPath.addRoundRect(r, r.width() * radiusFactor, r.width() * radiusFactor, Path.Direction.CW)
            }
            IconShape.CIRCLE -> maskPath.addCircle(r.centerX(), r.centerY(), r.width() / 2f, Path.Direction.CW)
            IconShape.SQUARE -> maskPath.addRect(r, Path.Direction.CW)
            IconShape.HEXAGON -> {
                val cx = r.centerX()
                val cy = r.centerY()
                val rad = r.width() / 2f
                for (i in 0 until 6) {
                    val angle = Math.toRadians((60.0 * i - 30.0))
                    val x = cx + rad * Math.cos(angle).toFloat()
                    val y = cy + rad * Math.sin(angle).toFloat()
                    if (i == 0) maskPath.moveTo(x, y) else maskPath.lineTo(x, y)
                }
                maskPath.close()
            }
        }
    }
}
