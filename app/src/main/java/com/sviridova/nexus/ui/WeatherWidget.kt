package com.sviridova.nexus.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import com.sviridova.nexus.R
import com.sviridova.nexus.data.WeatherRepository
import com.sviridova.nexus.util.withAlpha
import kotlin.math.sin

/**
 * Погодный виджет: температура, состояние, ветер и влажность.
 */
class WeatherWidget @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val bg = Paint(Paint.ANTI_ALIAS_FLAG)
    private val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val icon = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND }
    private val iconFill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val tempPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = NexusTheme.clockTypeface(context)
    }
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = NexusTheme.semiboldTypeface(context)
        letterSpacing = 0.16f
    }
    private val smallPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = NexusTheme.regularTypeface(context)
    }
    private val rect = RectF()

    var temp: Int = Int.MIN_VALUE
        set(value) { field = value; invalidate() }
    var code: Int = -1
        set(value) { field = value; invalidate() }
    var description: String = ""
        set(value) { field = value; invalidate() }
    var windKmh: Int = 0
        set(value) { field = value; invalidate() }
    var humidity: Int = 0
        set(value) { field = value; invalidate() }

    private var loading = false

    fun setLoading(value: Boolean) {
        loading = value
        invalidate()
    }

    private val accent: Int get() = NexusTheme.accent(context)

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val w = MeasureSpec.getSize(widthMeasureSpec)
        val d = resources.displayMetrics.density
        setMeasuredDimension(w, resolveSize((150 * d).toInt(), heightMeasureSpec))
    }

    override fun onDraw(canvas: Canvas) {
        val d = resources.displayMetrics.density
        val radius = 20f * d
        rect.set(0f, 0f, width.toFloat(), height.toFloat())
        bg.color = android.graphics.Color.argb(34, 255, 255, 255)
        canvas.drawRoundRect(rect, radius, radius, bg)
        bg.color = android.graphics.Color.argb(30, 6, 12, 20)
        canvas.drawRoundRect(rect, radius, radius, bg)
        stroke.color = withAlpha(accent, 42); stroke.strokeWidth = d
        canvas.drawRoundRect(rect, radius, radius, stroke)

        val hasData = temp != Int.MIN_VALUE
        labelPaint.textSize = 9.5f * d
        labelPaint.color = NexusTheme.TERTIARY
        canvas.drawText(context.getString(R.string.weather_label).uppercase(), 16 * d, 22 * d, labelPaint)
        if (loading) {
            smallPaint.textSize = 9f * d
            smallPaint.color = withAlpha(accent, 200)
            canvas.drawText(context.getString(R.string.weather_loading), width - 96 * d, 22 * d, smallPaint)
        }
        // Live-маркер.
        iconFill.color = withAlpha(accent, 220)
        canvas.drawCircle(width - 18 * d, 18 * d, 2.6f * d, iconFill)
        iconFill.color = withAlpha(accent, 50)
        canvas.drawCircle(width - 18 * d, 18 * d, 6f * d, iconFill)

        if (hasData) {
            tempPaint.textSize = 46f * d
            tempPaint.color = NexusTheme.PRIMARY
            canvas.drawText(if (temp > 0) "+$temp°" else "$temp°", 16 * d, 72 * d, tempPaint)

            smallPaint.textSize = 11.5f * d
            smallPaint.color = NexusTheme.SECONDARY
            canvas.drawText(description, 16 * d, 92 * d, smallPaint)

            smallPaint.textSize = 10f * d
            smallPaint.color = withAlpha(accent, 190)
            canvas.drawText(
                context.getString(R.string.weather_wind, windKmh).uppercase(),
                16 * d, 114 * d, smallPaint
            )
            smallPaint.color = NexusTheme.TERTIARY
            canvas.drawText(
                context.getString(R.string.weather_humidity, humidity).uppercase(),
                16 * d, 132 * d, smallPaint
            )

            drawWeatherGlyph(canvas, width - 58f * d, 62f * d, 30f * d)
        } else {
            smallPaint.textSize = 11f * d
            smallPaint.color = NexusTheme.TERTIARY
            canvas.drawText(context.getString(R.string.weather_permission).uppercase(), 16 * d, 60 * d, smallPaint)
            smallPaint.color = NexusTheme.TERTIARY
            canvas.drawText(context.getString(R.string.weather_permission_hint), 16 * d, 78 * d, smallPaint)
            drawWeatherGlyph(canvas, width - 58f * d, 56f * d, 26f * d, faded = true)
        }

        // Мини-график на 6 часов — декоративный импульс.
        val gx = 16 * d
        val gy = height - 12f * d
        val gw = width - 32f * d
        icon.strokeWidth = 1.4f * d
        val path = android.graphics.Path()
        val points = 18
        for (i in 0..points) {
            val x = gx + gw * i / points
            val amp = if (hasData) ((sin(i * 0.7) * 4.0 + sin(i * 1.9) * 2.0) * d).toFloat() else 0f
            val y = gy - 6 * d + amp
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        icon.color = withAlpha(accent, if (hasData) 120 else 40)
        canvas.drawPath(path, icon)
    }

    private fun drawWeatherGlyph(canvas: Canvas, cx: Float, cy: Float, r: Float, faded: Boolean = false) {
        val d = resources.displayMetrics.density
        val color = if (faded) withAlpha(accent, 70) else withAlpha(accent, 220)
        icon.color = color
        icon.strokeWidth = 1.8f * d
        when {
            code in 95..99 -> { // гроза
                iconFill.color = withAlpha(accent, if (faded) 40 else 120)
                canvas.drawCircle(cx, cy - r * 0.3f, r * 0.55f, iconFill)
                canvas.drawCircle(cx + r * 0.35f, cy - r * 0.1f, r * 0.4f, iconFill)
                canvas.drawCircle(cx - r * 0.4f, cy, r * 0.35f, iconFill)
                val bolt = android.graphics.Path()
                bolt.moveTo(cx - r * 0.1f, cy + r * 0.1f)
                bolt.lineTo(cx + r * 0.12f, cy + r * 0.1f)
                bolt.lineTo(cx - r * 0.05f, cy + r * 0.62f)
                bolt.lineTo(cx + r * 0.22f, cy + r * 0.2f)
                bolt.lineTo(cx + r * 0.02f, cy + r * 0.2f)
                bolt.close()
                iconFill.color = color
                canvas.drawPath(bolt, iconFill)
            }
            code in 51..67 || code in 80..82 -> { // дождь
                iconFill.color = withAlpha(accent, if (faded) 40 else 110)
                canvas.drawCircle(cx - r * 0.35f, cy - r * 0.15f, r * 0.42f, iconFill)
                canvas.drawCircle(cx + r * 0.1f, cy - r * 0.35f, r * 0.5f, iconFill)
                canvas.drawCircle(cx + r * 0.45f, cy - r * 0.05f, r * 0.36f, iconFill)
                icon.color = color
                for (i in -1..1) {
                    canvas.drawLine(cx + i * r * 0.32f, cy + r * 0.22f, cx + i * r * 0.32f - r * 0.12f, cy + r * 0.62f, icon)
                }
            }
            code in 71..77 || code in 85..86 -> { // снег
                iconFill.color = withAlpha(accent, if (faded) 40 else 110)
                canvas.drawCircle(cx, cy - r * 0.2f, r * 0.6f, iconFill)
                icon.color = color
                for (i in 0 until 3) {
                    val x = cx + (i - 1) * r * 0.35f
                    canvas.drawLine(x, cy + r * 0.35f, x, cy + r * 0.65f, icon)
                }
            }
            code in 45..48 -> { // туман
                icon.color = color
                for (i in 0 until 4) {
                    canvas.drawLine(cx - r * 0.6f, cy - r * 0.3f + i * r * 0.3f, cx + r * 0.6f, cy - r * 0.3f + i * r * 0.3f, icon)
                }
            }
            code == 0 || code == 1 -> { // солнце
                val rr = r * 0.42f
                iconFill.color = withAlpha(accent, if (faded) 40 else 140)
                canvas.drawCircle(cx, cy, rr, iconFill)
                icon.color = color
                canvas.drawCircle(cx, cy, rr, icon)
                for (i in 0 until 8) {
                    val a = Math.toRadians(i * 45.0)
                    val sx = cx + (rr + r * 0.14f) * kotlin.math.cos(a).toFloat()
                    val sy = cy + (rr + r * 0.14f) * kotlin.math.sin(a).toFloat()
                    val ex = cx + (rr + r * 0.32f) * kotlin.math.cos(a).toFloat()
                    val ey = cy + (rr + r * 0.32f) * kotlin.math.sin(a).toFloat()
                    canvas.drawLine(sx, sy, ex, ey, icon)
                }
            }
            else -> { // облако
                iconFill.color = withAlpha(accent, if (faded) 40 else 120)
                canvas.drawCircle(cx - r * 0.35f, cy + r * 0.05f, r * 0.4f, iconFill)
                canvas.drawCircle(cx + r * 0.05f, cy - r * 0.25f, r * 0.48f, iconFill)
                canvas.drawCircle(cx + r * 0.42f, cy + r * 0.05f, r * 0.34f, iconFill)
                if (code == 2) {
                    icon.color = withAlpha(accent, 200)
                    icon.strokeWidth = 1.6f * d
                    val sr = r * 0.28f
                    canvas.drawCircle(cx + r * 0.45f, cy - r * 0.5f, sr, icon)
                }
            }
        }
    }

    fun describe(code: Int): String = WeatherRepository.describe(code)
}
