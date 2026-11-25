package com.sviridova.nexus.ui

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Switch
import android.widget.TextView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.sviridova.nexus.Accent
import com.sviridova.nexus.BuildConfig
import com.sviridova.nexus.IconShape
import com.sviridova.nexus.Prefs
import com.sviridova.nexus.R
import com.sviridova.nexus.WallpaperStyle
import com.sviridova.nexus.util.withAlpha

/**
 * Настройки NEXUS. Экраны собраны кодом, чтобы палитра, типографика
 * и геометрия совпадали с рабочим столом.
 */
class SettingsActivity : Activity() {

    private lateinit var root: FrameLayout
    private lateinit var scroll: ScrollView
    private lateinit var content: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Prefs.init(this)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isStatusBarContrastEnforced = false
            window.isNavigationBarContrastEnforced = false
        }
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = false
        }

        root = FrameLayout(this)
        setContentView(root)
        root.addView(WallpaperView(this), match())
        root.addView(
            View(this).apply { setBackgroundColor(Color.argb(214, 3, 5, 9)) },
            match()
        )

        scroll = ScrollView(this).apply { isFillViewport = true }
        root.addView(scroll, match())
        content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20f), dp(10f), dp(20f), dp(36f))
        }
        scroll.addView(
            content,
            ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        )

        ViewCompat.setOnApplyWindowInsetsListener(scroll) { _, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
            content.updatePadding(top = dp(10f) + bars.top, bottom = dp(36f) + bars.bottom)
            insets
        }

        rebuild()
    }

    // ------------------------------------------------------------- sections

    private fun rebuild() {
        content.removeAllViews()
        buildHeader()
        buildAppearance()
        buildLayout()
        buildBehavior()
        buildAbout()
    }

    private fun buildHeader() {
        val row = LinearLayout(this@SettingsActivity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        row.addView(
            HudIconButton(this).apply {
                glyph = HudIconButton.Glyph.CLOSE
                setOnClickListener { close() }
            },
            LinearLayout.LayoutParams(dp(44f), dp(44f))
        )
        row.addView(
            hudText(this, getString(R.string.settings_title), 15f, NexusTheme.PRIMARY, R.font.orbitron, 0.18f),
            LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply { leftMargin = dp(6f) }
        )
        content.addView(row, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(56f)))
    }

    private fun buildAppearance() {
        section(getString(R.string.settings_appearance))

        card(getString(R.string.settings_accent)) {
            val row = LinearLayout(this@SettingsActivity).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(0, this@SettingsActivity.dp(12f), 0, this@SettingsActivity.dp(2f))
            }
            for (accent in Accent.values()) {
                val selected = Prefs.accent == accent
                val dot = View(this@SettingsActivity).apply {
                    background = GradientDrawable().apply {
                        shape = GradientDrawable.OVAL
                        setColor(accent.color)
                        setStroke(this@SettingsActivity.dp(if (selected) 2.4f else 1f), if (selected) Color.WHITE else withAlpha(accent.color, 90))
                    }
                    setOnClickListener {
                        Prefs.accentIndex = accent.ordinal
                        this@SettingsActivity.rebuild()
                    }
                }
                row.addView(dot, LinearLayout.LayoutParams(this@SettingsActivity.dp(34f), this@SettingsActivity.dp(34f)).apply { rightMargin = this@SettingsActivity.dp(11f) })
            }
            addView(row)
        }

        card(getString(R.string.settings_wallpaper)) {
            val row = LinearLayout(this@SettingsActivity).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(0, this@SettingsActivity.dp(12f), 0, this@SettingsActivity.dp(2f))
            }
            for (style in WallpaperStyle.values()) {
                row.addView(chip(this@SettingsActivity.getString(style.labelRes), Prefs.wallpaper == style) {
                    Prefs.wallpaperIndex = style.ordinal
                    this@SettingsActivity.rebuild()
                }, this@SettingsActivity.chipParams())
            }
            addView(row)
        }

        card(getString(R.string.settings_icon_shape)) {
            val row = LinearLayout(this@SettingsActivity).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(0, this@SettingsActivity.dp(12f), 0, this@SettingsActivity.dp(2f))
            }
            for (shape in IconShape.values()) {
                row.addView(chip(this@SettingsActivity.getString(shape.labelRes), Prefs.iconShape == shape) {
                    Prefs.iconShapeIndex = shape.ordinal
                    this@SettingsActivity.rebuild()
                }, this@SettingsActivity.chipParams())
            }
            addView(row)
        }

        toggle(getString(R.string.settings_glow), Prefs.glow) { Prefs.glow = it }
    }

    private fun buildLayout() {
        section(getString(R.string.settings_grid_section))

        card(getString(R.string.settings_grid), value = "${Prefs.columns}") {
            val row = LinearLayout(this@SettingsActivity).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(0, this@SettingsActivity.dp(12f), 0, this@SettingsActivity.dp(2f))
            }
            for (cols in 4..6) {
                val selected = Prefs.columns == cols
                val cell = TextView(this@SettingsActivity).apply {
                    text = cols.toString()
                    textSize = 15f
                    typeface = NexusTheme.displayTypeface(this@SettingsActivity)
                    gravity = Gravity.CENTER
                    setTextColor(if (selected) NexusTheme.PRIMARY else NexusTheme.SECONDARY)
                    background = GradientDrawable().apply {
                        cornerRadius = this@SettingsActivity.dp(10f).toFloat()
                        setColor(if (selected) withAlpha(Prefs.accent.color, 46) else Color.argb(24, 255, 255, 255))
                        setStroke(this@SettingsActivity.dp(1f), if (selected) withAlpha(Prefs.accent.color, 220) else Color.argb(30, 255, 255, 255))
                    }
                    setOnClickListener {
                        Prefs.columns = cols
                        this@SettingsActivity.rebuild()
                    }
                }
                row.addView(cell, LinearLayout.LayoutParams(0, this@SettingsActivity.dp(40f), 1f).apply { rightMargin = this@SettingsActivity.dp(8f) })
            }
            addView(row)
        }

        toggle(getString(R.string.settings_search_bar), Prefs.showSearchBar) { Prefs.showSearchBar = it }
        toggle(getString(R.string.settings_clock_seconds), Prefs.clockSeconds) { Prefs.clockSeconds = it }
        toggle(getString(R.string.settings_hide_labels), Prefs.hideLabels) { Prefs.hideLabels = it }
    }

    private fun buildBehavior() {
        section(getString(R.string.settings_behavior))

        toggle(getString(R.string.settings_haptics), Prefs.haptics) { Prefs.haptics = it }

        card(
            getString(R.string.settings_default_launcher),
            subtitle = getString(R.string.settings_default_hint)
        ) {
            addView(
                action(getString(R.string.settings_default_cta), primary = true) {
                    runCatching {
                        this@SettingsActivity.startActivity(Intent(Settings.ACTION_HOME_SETTINGS))
                    }
                },
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    this@SettingsActivity.dp(40f)
                ).apply { topMargin = this@SettingsActivity.dp(12f) }
            )
        }

        card(
            getString(R.string.settings_reset),
            subtitle = getString(R.string.settings_reset_hint)
        ) {
            addView(
                action("СБРОС", primary = false) {
                    Prefs.reset()
                    android.widget.Toast.makeText(this@SettingsActivity, R.string.settings_reset_done, android.widget.Toast.LENGTH_SHORT).show()
                    this@SettingsActivity.rebuild()
                },
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    this@SettingsActivity.dp(40f)
                ).apply { topMargin = this@SettingsActivity.dp(12f) }
            )
        }
    }

    private fun buildAbout() {
        section(getString(R.string.settings_about))
        card(getString(R.string.settings_version), value = BuildConfig.VERSION_NAME)
        card(getString(R.string.settings_made_for), value = "SVIRIDOVA · NEXUS")

        val tip = hudText(
            this,
            getString(R.string.settings_tip),
            12f,
            NexusTheme.TERTIARY,
            R.font.rajdhani_regular,
            0.02f,
            upper = false
        )
        tip.setLineSpacing(dp(3f).toFloat(), 1f)
        content.addView(
            tip,
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                topMargin = dp(14f)
            }
        )
    }

    // -------------------------------------------------------------- helpers

    private fun section(title: String) {
        val label = hudText(this, title, 10f, Prefs.accent.color, R.font.rajdhani_bold, 0.22f)
        content.addView(
            label,
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                topMargin = dp(20f)
                bottomMargin = dp(8f)
            }
        )
    }

    private fun card(title: String, value: String? = null, subtitle: String? = null, builder: LinearLayout.() -> Unit = {}) {
        val panel = GlassPanel(this).apply {
            cornerRadius = dp(16f).toFloat()
            fillAlpha = 52
            setPadding(dp(16f), dp(12f), dp(16f), dp(14f))
        }
        val column = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val row = LinearLayout(this@SettingsActivity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        row.addView(
            hudText(this, title, 13f, NexusTheme.PRIMARY, R.font.rajdhani_semibold, 0.08f, upper = false),
            LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        )
        if (value != null) {
            row.addView(hudText(this, value, 12f, Prefs.accent.color, R.font.rajdhani_bold, 0.1f))
        }
        column.addView(row)
        if (subtitle != null) {
            val s = hudText(this, subtitle, 11f, NexusTheme.TERTIARY, R.font.rajdhani_regular, 0.02f, upper = false)
            s.setLineSpacing(dp(2f).toFloat(), 1f)
            column.addView(
                s,
                LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                    .apply { topMargin = dp(3f) }
            )
        }
        column.builder()
        panel.addView(
            column,
            FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        )
        content.addView(
            panel,
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                .apply { bottomMargin = dp(10f) }
        )
    }

    @Suppress("UseSwitchCompatOrMaterialCode", "DEPRECATION")
    private fun toggle(title: String, initial: Boolean, onChange: (Boolean) -> Unit) {
        val panel = GlassPanel(this).apply {
            cornerRadius = dp(16f).toFloat()
            fillAlpha = 52
            setPadding(dp(16f), dp(4f), dp(10f), dp(4f))
        }
        val row = LinearLayout(this@SettingsActivity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            minimumHeight = dp(52f)
        }
        row.addView(
            hudText(this, title, 13f, NexusTheme.PRIMARY, R.font.rajdhani_semibold, 0.08f, upper = false),
            LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        )
        row.addView(Switch(this).apply {
            isChecked = initial
            thumbTintList = android.content.res.ColorStateList.valueOf(Prefs.accent.color)
            trackTintList = android.content.res.ColorStateList.valueOf(withAlpha(Prefs.accent.color, 90))
            setOnCheckedChangeListener { _, checked -> onChange(checked) }
        })
        panel.addView(row)
        content.addView(
            panel,
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                .apply { bottomMargin = dp(10f) }
        )
    }

    private fun chip(label: String, selected: Boolean, onClick: () -> Unit): TextView =
        TextView(this).apply {
            text = label.uppercase()
            textSize = 10f
            typeface = NexusTheme.semiboldTypeface(context)
            letterSpacing = 0.04f
            gravity = Gravity.CENTER
            setTextColor(if (selected) NexusTheme.PRIMARY else NexusTheme.SECONDARY)
            background = GradientDrawable().apply {
                cornerRadius = this@SettingsActivity.dp(10f).toFloat()
                setColor(if (selected) withAlpha(Prefs.accent.color, 46) else Color.argb(24, 255, 255, 255))
                setStroke(this@SettingsActivity.dp(1f), if (selected) withAlpha(Prefs.accent.color, 220) else Color.argb(30, 255, 255, 255))
            }
            setOnClickListener { onClick() }
        }

    private fun chipParams() = LinearLayout.LayoutParams(0, dp(36f), 1f).apply { rightMargin = dp(6f) }

    private fun action(label: String, primary: Boolean, onClick: () -> Unit): TextView =
        hudText(
            this,
            label,
            11f,
            if (primary) Prefs.accent.color else NexusTheme.SECONDARY,
            R.font.rajdhani_bold,
            0.16f
        ).apply {
            gravity = Gravity.CENTER
            background = GradientDrawable().apply {
                cornerRadius = this@SettingsActivity.dp(10f).toFloat()
                setColor(if (primary) withAlpha(Prefs.accent.color, 36) else Color.argb(26, 255, 255, 255))
                setStroke(this@SettingsActivity.dp(1f), if (primary) withAlpha(Prefs.accent.color, 200) else Color.argb(60, 255, 255, 255))
            }
            setOnClickListener { onClick() }
        }

    private fun close() {
        finish()
        overridePendingTransition(0, 0)
    }

    private fun match() = FrameLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.MATCH_PARENT
    )

    private fun dp(value: Float): Int = (value * resources.displayMetrics.density).toInt()
}
