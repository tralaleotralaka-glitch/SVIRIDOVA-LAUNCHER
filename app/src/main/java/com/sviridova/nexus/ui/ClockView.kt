package com.sviridova.nexus.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.View
import com.sviridova.nexus.Prefs
import com.sviridova.nexus.util.withAlpha
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Крупные часы HUD: время, секунды, дата и день недели.
 */
class ClockView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val timePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = NexusTheme.clockTypeface(context)
        color = NexusTheme.PRIMARY
        letterSpacing = 0.02f
    }
    private val tailPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = NexusTheme.semiboldTypeface(context)
        letterSpacing = 0.06f
    }
    private val datePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = NexusTheme.semiboldTypeface(context)
        letterSpacing = 0.22f
    }
    private val barPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private val rect = RectF()
    private val handler = Handler(Looper.getMainLooper())
    private var blink = true

    private val dateFormatter = DateTimeFormatter.ofPattern("d MMMM", Locale.getDefault())
    private val weekFormatter = DateTimeFormatter.ofPattern("EEEE", Locale.getDefault())

    private val ticker = object : Runnable {
        override fun run() {
            blink = !blink
            invalidate()
            val delay = if (Prefs.clockSeconds) 500L else 30_000L
            handler.postDelayed(this, delay)
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        handler.post(ticker)
    }

    override fun onDetachedFromWindow() {
        handler.removeCallbacks(ticker)
        super.onDetachedFromWindow()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val w = MeasureSpec.getSize(widthMeasureSpec)
        val d = resources.displayMetrics.density
        val h = (104 * d + if (Prefs.clockSeconds) 8 * d else 0f).toInt()
        setMeasuredDimension(w, resolveSize(h, heightMeasureSpec))
    }

    override fun onDraw(canvas: Canvas) {
        val now = LocalDateTime.now()
        val d = resources.displayMetrics.density
        val accent = Prefs.accent.color
        val showSeconds = Prefs.clockSeconds

        var x = paddingLeft.toFloat()
        val baseline = paddingTop + 62f * d

        timePaint.textSize = 62f * d
        timePaint.color = NexusTheme.PRIMARY
        val hh = "%02d".format(now.hour)
        val mm = "%02d".format(now.minute)
        val wHh = timePaint.measureText(hh)
        canvas.drawText(hh, x, baseline, timePaint)
        x += wHh

        // Двоеточие с пульсацией.
        dotPaint.color = withAlpha(accent, if (blink) 255 else 70)
        val colonY = baseline - 22f * d
        canvas.drawCircle(x + 6f * d, colonY - 12f * d, 3.4f * d, dotPaint)
        canvas.drawCircle(x + 6f * d, colonY + 12f * d, 3.4f * d, dotPaint)
        x += 18f * d

        canvas.drawText(mm, x, baseline, timePaint)
        x += timePaint.measureText(mm)

        if (showSeconds) {
            tailPaint.textSize = 24f * d
            tailPaint.color = withAlpha(accent, 235)
            canvas.drawText(":%02d".format(now.second), x + 4f * d, baseline, tailPaint)
        }

        // Дата и день недели.
        val week = weekFormatter.format(now).replaceFirstChar { it.uppercase(Locale.getDefault()) }
        val date = dateFormatter.format(now)
        datePaint.textSize = 12.5f * d
        datePaint.color = NexusTheme.SECONDARY
        val subtitle = "$week  ·  $date".uppercase(Locale.getDefault())
        canvas.drawText(subtitle, paddingLeft.toFloat(), baseline + 30f * d, datePaint)

        // Декоративный сегментный индикатор.
        val segY = baseline + 40f * d
        val accent2 = Prefs.accent.color
        var sx = paddingLeft.toFloat()
        rect.set(sx, segY, sx + 34f * d, segY + 2f * d)
        barPaint.color = withAlpha(accent2, 220)
        canvas.drawRoundRect(rect, 1f * d, 1f * d, barPaint)
        sx += 40f * d
        for (i in 0 until 10) {
            barPaint.color = withAlpha(accent2, if (i < 6) 90 else 30)
            rect.set(sx + i * 7f * d, segY, sx + i * 7f * d + 3f * d, segY + 2f * d)
            canvas.drawRect(rect, barPaint)
        }

        // Индикатор секунд справа (тонкая шкала).
        if (showSeconds) {
            val gaugeRight = width - paddingRight.toFloat()
            val gaugeLeft = gaugeRight - 64f * d
            val gy = baseline + 3f * d
            barPaint.color = withAlpha(accent, 40)
            rect.set(gaugeLeft, gy, gaugeRight, gy + 2f * d)
            canvas.drawRoundRect(rect, 1f * d, 1f * d, barPaint)
            barPaint.color = withAlpha(accent, 220)
            rect.set(gaugeLeft, gy, gaugeLeft + (gaugeRight - gaugeLeft) * (now.second / 60f), gy + 2f * d)
            canvas.drawRoundRect(rect, 1f * d, 1f * d, barPaint)
        }
    }
}
