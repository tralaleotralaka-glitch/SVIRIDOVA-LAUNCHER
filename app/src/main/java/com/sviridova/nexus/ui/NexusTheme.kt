package com.sviridova.nexus.ui

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import androidx.core.content.res.ResourcesCompat
import com.sviridova.nexus.Prefs
import com.sviridova.nexus.R
import com.sviridova.nexus.util.withAlpha

/** Общая палитра и типографика NEXUS. */
object NexusTheme {

    val PRIMARY = 0xFFF2FAFF.toInt()
    val SECONDARY = 0xFF8FA6BD.toInt()
    val TERTIARY = 0xFF4E6178.toInt()
    val VOID = 0xFF05070C.toInt()
    val DEEP = 0xFF0A0F17.toInt()

    fun accent(context: Context): Int = Prefs.accent.color

    fun glowEnabled(): Boolean = Prefs.glow

    fun clockTypeface(context: Context): Typeface =
        ResourcesCompat.getFont(context, R.font.orbitron) ?: Typeface.DEFAULT_BOLD

    fun displayTypeface(context: Context): Typeface =
        ResourcesCompat.getFont(context, R.font.rajdhani_bold) ?: Typeface.DEFAULT_BOLD

    fun semiboldTypeface(context: Context): Typeface =
        ResourcesCompat.getFont(context, R.font.rajdhani_semibold) ?: Typeface.DEFAULT

    fun regularTypeface(context: Context): Typeface =
        ResourcesCompat.getFont(context, R.font.rajdhani_regular) ?: Typeface.DEFAULT

    /** Полупрозрачная «стеклянная» панель с тонкой неоновой обводкой. */
    /* Заготовка «стекла» для будущих экранов. */
    fun glassBackground(cornerDp: Float = 22f, fillAlpha: Int = 36): GradientDrawable {
        return GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            intArrayOf(
                Color.argb(28, 255, 255, 255),
                Color.argb(fillAlpha, 8, 14, 24),
                Color.argb(fillAlpha + 24, 4, 8, 14)
            )
        ).apply {
            cornerRadius = cornerDp * 3f
            setStroke(2, withAlpha(0x00F0FF, 28))
        }
    }

}
