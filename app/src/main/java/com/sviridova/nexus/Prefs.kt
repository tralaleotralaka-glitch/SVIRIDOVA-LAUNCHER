package com.sviridova.nexus

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

/** Accent colours available to the whole launcher. */
enum class Accent(val labelRes: Int, val color: Int) {
    CYAN(R.string.accent_cyan, 0xFF00F0FF.toInt()),
    MAGENTA(R.string.accent_magenta, 0xFFFF2E97.toInt()),
    LIME(R.string.accent_lime, 0xFFB6FF3C.toInt()),
    AMBER(R.string.accent_amber, 0xFFFFB020.toInt()),
    VIOLET(R.string.accent_violet, 0xFF9D5CFF.toInt()),
    ICE(R.string.accent_ice, 0xFF7FB8FF.toInt());

    companion object {
        fun of(index: Int): Accent = values()[index.coerceIn(0, values().size - 1)]
    }
}

/** Animated wallpaper styles. */
enum class WallpaperStyle(val labelRes: Int) {
    GRID(R.string.wall_grid),
    AURORA(R.string.wall_aurora),
    MATRIX(R.string.wall_matrix),
    PARTICLES(R.string.wall_particles),
    PLAIN(R.string.wall_plain);

    companion object {
        fun of(index: Int): WallpaperStyle = values()[index.coerceIn(0, values().size - 1)]
    }
}

/** Icon mask shapes. */
enum class IconShape(val labelRes: Int) {
    SQUIRCLE(R.string.shape_squircle),
    CIRCLE(R.string.shape_circle),
    SQUARE(R.string.shape_square),
    HEXAGON(R.string.shape_hexagon);

    companion object {
        fun of(index: Int): IconShape = values()[index.coerceIn(0, values().size - 1)]
    }
}

/**
 * Single source of truth for every user preference.
 * Initialised once from [NexusApp].
 */
object Prefs {
    private const val FILE = "nexus_prefs"
    private lateinit var sp: SharedPreferences

    fun init(context: Context) {
        if (::sp.isInitialized) return
        sp = context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)
    }

    var accentIndex: Int
        get() = sp.getInt("accent", 0)
        set(value) = sp.edit { putInt("accent", value) }

    val accent: Accent
        get() = Accent.of(accentIndex)

    var wallpaperIndex: Int
        get() = sp.getInt("wallpaper", 0)
        set(value) = sp.edit { putInt("wallpaper", value) }

    val wallpaper: WallpaperStyle
        get() = WallpaperStyle.of(wallpaperIndex)

    var iconShapeIndex: Int
        get() = sp.getInt("icon_shape", 0)
        set(value) = sp.edit { putInt("icon_shape", value) }

    val iconShape: IconShape
        get() = IconShape.of(iconShapeIndex)

    var glow: Boolean
        get() = sp.getBoolean("glow", true)
        set(value) = sp.edit { putBoolean("glow", value) }

    var columns: Int
        get() = sp.getInt("columns", 5).coerceIn(4, 6)
        set(value) = sp.edit { putInt("columns", value.coerceIn(4, 6)) }

    var clockSeconds: Boolean
        get() = sp.getBoolean("clock_seconds", false)
        set(value) = sp.edit { putBoolean("clock_seconds", value) }

    var hideLabels: Boolean
        get() = sp.getBoolean("hide_labels", false)
        set(value) = sp.edit { putBoolean("hide_labels", value) }

    var haptics: Boolean
        get() = sp.getBoolean("haptics", true)
        set(value) = sp.edit { putBoolean("haptics", value) }

    var showSearchBar: Boolean
        get() = sp.getBoolean("search_bar", true)
        set(value) = sp.edit { putBoolean("search_bar", value) }

    // Weather cache
    var weatherTemp: Int
        get() = sp.getInt("wx_temp", Int.MIN_VALUE)
        set(value) = sp.edit { putInt("wx_temp", value) }

    var weatherCode: Int
        get() = sp.getInt("wx_code", -1)
        set(value) = sp.edit { putInt("wx_code", value) }

    var weatherAt: Long
        get() = sp.getLong("wx_at", 0L)
        set(value) = sp.edit { putLong("wx_at", value) }

    fun reset() {
        sp.edit { clear() }
    }
}
