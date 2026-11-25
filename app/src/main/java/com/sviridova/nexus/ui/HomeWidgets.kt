package com.sviridova.nexus.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.util.AttributeSet
import com.sviridova.nexus.R
import com.sviridova.nexus.util.withAlpha

/** Батарея: уровень, состояние зарядки, сегментная шкала. */
class BatteryWidget @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : PillWidget(context, attrs) {

    private var level = -1
    private var charging = false

    fun set(level: Int, charging: Boolean) {
        this.level = level
        this.charging = charging
        title = context.getString(R.string.battery_title).uppercase()
        value = if (level >= 0) "$level%" else "—"
        caption = when {
            level < 0 -> context.getString(R.string.battery_unknown)
            charging -> context.getString(R.string.battery_charging)
            level <= 15 -> context.getString(R.string.battery_low)
            else -> context.getString(R.string.battery_normal)
        }.uppercase()
        progress = level / 100f
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (level < 0) return
        val d = resources.displayMetrics.density
        // Мини-иконка батареи.
        val w = 22 * d
        val h = 11 * d
        val right = width - 16 * d
        val top = height - 26 * d
        val body = RectF(right - w, top, right, top + h)
        val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 1.3f * d
            color = withAlpha(accent, 200)
        }
        canvas.drawRoundRect(body, 2.4f * d, 2.4f * d, stroke)
        val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = withAlpha(accent, 200) }
        canvas.drawRoundRect(RectF(right, top + h * 0.3f, right + 2 * d, top + h * 0.7f), d, d, fill)
        val inner = RectF(body)
        inner.inset(2 * d, 2 * d)
        inner.right = inner.left + inner.width() * (level / 100f)
        canvas.drawRoundRect(inner, 1.2f * d, 1.2f * d, fill)
        if (charging) {
            val bolt = Path()
            val cx = body.centerX()
            val cy = body.centerY()
            bolt.moveTo(cx - 2.2f * d, cy - 4.5f * d)
            bolt.lineTo(cx + 1.6f * d, cy - 4.5f * d)
            bolt.lineTo(cx - 0.4f * d, cy + 0.4f * d)
            bolt.lineTo(cx + 2.4f * d, cy + 0.4f * d)
            bolt.lineTo(cx - 1.6f * d, cy + 5 * d)
            bolt.lineTo(cx + 0.2f * d, cy + 1.2f * d)
            bolt.lineTo(cx - 1.8f * d, cy + 1.2f * d)
            bolt.close()
            fill.color = 0xFF05070C.toInt()
            canvas.drawPath(bolt, fill)
        }
    }
}

/** Мини-виджет заметок: быстрый доступ к «Заметкам» и счётчик. */
class NotesWidget @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : PillWidget(context, attrs) {

    init {
        title = context.getString(R.string.notes_title).uppercase()
        value = "—"
        caption = context.getString(R.string.notes_empty).uppercase()
    }

    fun setCount(count: Int) {
        value = if (count == 0) "—" else count.toString()
        caption = if (count == 0) {
            context.getString(R.string.notes_empty).uppercase()
        } else {
            context.resources.getQuantityString(R.plurals.notes_count, count, count).uppercase()
        }
        invalidate()
    }
}
