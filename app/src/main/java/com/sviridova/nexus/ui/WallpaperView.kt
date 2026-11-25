package com.sviridova.nexus.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View
import com.sviridova.nexus.Prefs
import com.sviridova.nexus.WallpaperStyle
import com.sviridova.nexus.util.withAlpha
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sin
import kotlin.random.Random

/**
 * Живая обводка рабочего стола.
 *
 * Все стили рисуются обычным Canvas (без внешних библиотек) и обновляются
 * с ограничением ~40 к/с, чтобы не съедать батарею.
 */
class WallpaperView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    private val base = Paint(Paint.ANTI_ALIAS_FLAG)
    private val path = Path()

    private var startTime = 0L
    private var lastFrame = 0L

    private val rand = Random(7)
    private val stars = List(90) { floatArrayOf(rand.nextFloat(), rand.nextFloat(), 0.35f + rand.nextFloat() * 1.1f) }
    private val particles = List(46) {
        floatArrayOf(
            rand.nextFloat(), rand.nextFloat(),
            0.05f + rand.nextFloat() * 0.13f,
            0.2f + rand.nextFloat() * 0.8f,
            6f + rand.nextFloat() * 26f
        )
    }

    private val glyphs = "01アイウエオカキクケコサシスセソタチツテトナニヌネノ0127ΔΣ≡".toCharArray()
    private val matrixCols = List(26) {
        floatArrayOf(
            rand.nextFloat(),
            0.2f + rand.nextFloat() * 0.9f,
            rand.nextFloat() * -3000f,
            7f + rand.nextInt(13).toFloat()
        )
    }

    fun observePrefs() {
        invalidate()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        startTime = System.currentTimeMillis()
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        val now = System.currentTimeMillis()
        if (now - lastFrame < 24L) {
            postInvalidateOnAnimation()
            return
        }
        lastFrame = now
        val t = (now - startTime) / 1000f

        drawBase(canvas)

        when (Prefs.wallpaper) {
            WallpaperStyle.GRID -> drawGridHorizon(canvas, t)
            WallpaperStyle.AURORA -> drawAurora(canvas, t)
            WallpaperStyle.MATRIX -> drawMatrix(canvas, t)
            WallpaperStyle.PARTICLES -> drawParticles(canvas, t)
            WallpaperStyle.PLAIN -> Unit
        }

        drawVignette(canvas)
        postInvalidateOnAnimation()
    }

    private fun drawBase(canvas: Canvas) {
        val accent = Prefs.accent.color
        base.shader = LinearGradient(
            0f, 0f, 0f, height.toFloat(),
            intArrayOf(0xFF05070C.toInt(), 0xFF070B13.toInt(), withAlpha(accent, 26), 0xFF05070C.toInt()),
            floatArrayOf(0f, 0.42f, 0.78f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), base)
        base.shader = null

        paint.color = withAlpha(0x000000, 120)
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
    }

    /** Синтвейв-горизонт: перспективная сетка + солнце + звёзды. */
    private fun drawGridHorizon(canvas: Canvas, t: Float) {
        val accent = Prefs.accent.color
        val horizon = height * 0.62f

        // Звёзды с мерцанием.
        paint.style = Paint.Style.FILL
        for (s in stars) {
            val a = (110 + 100 * sin((t * 0.9f + s[2] * 7f).toDouble()).toFloat()).toInt().coerceIn(20, 220)
            paint.color = withAlpha(0xE8F6FF, a)
            canvas.drawCircle(s[0] * width, s[1] * horizon * 0.86f, s[2], paint)
        }

        // Солнце за горизонтом.
        val sunCenter = horizon - height * 0.12f
        val sunR = width * 0.30f
        paint.shader = RadialGradient(
            width / 2f, sunCenter, sunR,
            intArrayOf(withAlpha(accent, 150), withAlpha(accent, 60), withAlpha(accent, 0)),
            floatArrayOf(0f, 0.55f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawCircle(width / 2f, sunCenter, sunR, paint)
        paint.shader = null

        // Диск с горизонтальными прорезями.
        paint.color = withAlpha(accent, 36)
        canvas.save()
        canvas.clipRect(0f, sunCenter - sunR, width.toFloat(), horizon)
        canvas.drawCircle(width / 2f, sunCenter, sunR * 0.74f, paint)
        paint.color = 0xFF05070C.toInt()
        var y = sunCenter - sunR * 0.1f
        var gap = height * 0.006f
        while (y < horizon) {
            canvas.drawRect(0f, y, width.toFloat(), y + gap, paint)
            y += gap * 1.9f
            gap *= 1.16f
        }
        canvas.restore()

        // Горизонтальная линия горизонта.
        linePaint.color = withAlpha(accent, 150)
        linePaint.strokeWidth = 1.6f * resources.displayMetrics.density
        canvas.drawLine(0f, horizon, width.toFloat(), horizon, linePaint)

        // Нижние лучи перспективы.
        linePaint.color = withAlpha(accent, 46)
        linePaint.strokeWidth = resources.displayMetrics.density
        val vx = width / 2f
        val rays = 13
        for (i in -rays..rays) {
            val targetX = vx + i * (width / rays.toFloat()) * 1.6f
            canvas.drawLine(vx, horizon, targetX, height.toFloat(), linePaint)
        }

        // Бегущие горизонтальные линии сетки.
        val rows = 12
        val speed = (t * 0.22f) % 1f
        for (i in 0 until rows) {
            val k = ((i.toFloat() + speed) / rows)
            val per = k.pow(2.4f)
            val y = horizon + per * (height - horizon)
            val alpha = (30 + 120 * per).toInt().coerceAtMost(150)
            linePaint.color = withAlpha(accent, alpha)
            linePaint.strokeWidth = (0.6f + 1.6f * per) * resources.displayMetrics.density
            canvas.drawLine(0f, y, width.toFloat(), y, linePaint)
        }
    }

    private fun drawAurora(canvas: Canvas, t: Float) {
        val accent = Prefs.accent.color
        val blobs = arrayOf(
            floatArrayOf(0.28f, 0.24f, 0.55f),
            floatArrayOf(0.76f, 0.38f, 0.48f),
            floatArrayOf(0.46f, 0.86f, 0.62f),
            floatArrayOf(0.86f, 0.68f, 0.36f)
        )
        val colors = intArrayOf(accent, 0xFF9D5CFF.toInt(), 0xFF2E6BFF.toInt(), 0xFFFF2E97.toInt())
        for ((i, b) in blobs.withIndex()) {
            val cx = (b[0] + 0.10f * sin((t * (0.16f + i * 0.05f) + i * 1.7f).toDouble()).toFloat()) * width
            val cy = (b[1] + 0.08f * cos((t * (0.13f + i * 0.04f) + i * 2.3f).toDouble()).toFloat()) * height
            val r = b[2] * max(width, height) * 0.55f
            paint.shader = RadialGradient(
                cx, cy, r,
                intArrayOf(withAlpha(colors[i % colors.size], 52), withAlpha(colors[i % colors.size], 14), 0),
                floatArrayOf(0f, 0.5f, 1f),
                Shader.TileMode.CLAMP
            )
            canvas.drawCircle(cx, cy, r, paint)
            paint.shader = null
        }

        // Мягкие линии тока.
        linePaint.strokeWidth = 1.2f * resources.displayMetrics.density
        for (i in 0 until 7) {
            path.reset()
            val yBase = height * (0.14f + i * 0.11f)
            path.moveTo(0f, yBase)
            var x = 0f
            while (x <= width) {
                val y = yBase + 26f * sin((x / width * 2.4f * PI + t * (0.5f + i * 0.08f)).toDouble()).toFloat() +
                    14f * cos((x / width * 5.1f * PI - t * 0.7f).toDouble()).toFloat()
                path.lineTo(x, y)
                x += width / 24f
            }
            linePaint.color = withAlpha(accent, 20 + i * 5)
            canvas.drawPath(path, linePaint)
        }
    }

    private fun drawMatrix(canvas: Canvas, t: Float) {
        val accent = Prefs.accent.color
        val d = resources.displayMetrics.density
        paint.typeface = NexusTheme.semiboldTypeface(context)
        paint.textSize = 15f * d
        paint.style = Paint.Style.FILL
        for ((i, c) in matrixCols.withIndex()) {
            val x = c[0] * width
            val speed = c[1] * 46f * d
            val offset = c[2] + t * speed
            val step = c[3] * d * 1.9f
            val startY = (offset % (height + step * 12)) - step * 12
            val count = 14
            for (k in 0 until count) {
                val y = startY + k * step
                if (y < -step || y > height + step) continue
                val fade = (k.toFloat() / count)
                val alpha = (20 + fade * 150).toInt()
                paint.color = withAlpha(accent, alpha)
                val ch = glyphs[(i * 7 + k * 3) % glyphs.size]
                canvas.drawText(ch.toString(), x, y, paint)
            }
            // Голова колонны — ярче.
            val headY = startY + (count - 1) * step
            if (headY in -step..(height + step)) {
                paint.color = withAlpha(0xFFFFFF, 210)
                canvas.drawText(glyphs[i % glyphs.size].toString(), x, headY, paint)
            }
        }
    }

    private fun drawParticles(canvas: Canvas, t: Float) {
        val accent = Prefs.accent.color
        val pts = particles.map {
            val x = (it[0] + it[2] * t * 0.06f) % 1f
            val y = (it[1] + 0.02f * sin((t * it[3] * 0.7f + it[0] * 9f).toDouble()).toFloat()) % 1f
            floatArrayOf(x * width, y * height, it[4] * resources.displayMetrics.density)
        }
        linePaint.strokeWidth = 0.7f * resources.displayMetrics.density
        for (i in pts.indices) {
            for (j in i + 1 until pts.size) {
                val dx = pts[i][0] - pts[j][0]
                val dy = pts[i][1] - pts[j][1]
                val dist = kotlin.math.sqrt(dx * dx + dy * dy)
                if (dist < width * 0.24f) {
                    val a = (70 * (1f - dist / (width * 0.24f))).toInt()
                    linePaint.color = withAlpha(accent, a)
                    canvas.drawLine(pts[i][0], pts[i][1], pts[j][0], pts[j][1], linePaint)
                }
            }
        }
        paint.style = Paint.Style.FILL
        for (p in pts) {
            paint.color = withAlpha(accent, 170)
            canvas.drawCircle(p[0], p[1], p[2] * 0.55f, paint)
            paint.color = withAlpha(0xFFFFFF, 190)
            canvas.drawCircle(p[0], p[1], p[2] * 0.2f, paint)
        }
    }

    private fun drawVignette(canvas: Canvas) {
        paint.shader = RadialGradient(
            width / 2f, height * 0.45f, max(width, height) * 0.72f,
            intArrayOf(0x00000000, withAlpha(0x000000, 40), withAlpha(0x000000, 170)),
            floatArrayOf(0f, 0.62f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
        paint.shader = null
    }
}
