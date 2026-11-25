package com.sviridova.nexus.ui

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.sviridova.nexus.Prefs
import com.sviridova.nexus.R
import com.sviridova.nexus.data.AppRepository
import com.sviridova.nexus.data.NotesStore
import com.sviridova.nexus.data.WeatherRepository
import com.sviridova.nexus.model.AppEntry
import com.sviridova.nexus.util.withAlpha
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Рабочий стол NEXUS.
 *
 * Раскладка зависит от числа столбцов, высоты экрана и настроек, поэтому
 * собирается кодом; иконки, часы, обои и панели рисуются Canvas-вью.
 * Лаунчер не зависит от AppCompat — только от androidx.core.
 */
class HomeActivity : Activity() {

    private lateinit var root: FrameLayout
    private lateinit var wallpaper: WallpaperView
    private lateinit var content: LinearLayout
    private lateinit var topBar: LinearLayout

    private lateinit var clock: ClockView
    private lateinit var weather: WeatherWidget
    private lateinit var battery: BatteryWidget
    private lateinit var notes: NotesWidget
    private lateinit var pager: AppPagerView
    private lateinit var searchPill: SearchPill
    private lateinit var searchOverlay: SearchOverlay
    private lateinit var dock: LinearLayout
    private lateinit var dockFrame: DockBar

    private var apps: List<AppEntry> = emptyList()

    private var permissionAsked = false
    private var notesSheet: NotesSheet? = null

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
            val status = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
            val charging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL
            if (level >= 0 && scale > 0) battery.set((level * 100f / scale).toInt(), charging)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Prefs.init(this)
        setupWindow()
        buildLayout()
        applyInsets()
        loadApps()
        refreshWeather()
        notes.setCount(NotesStore.count(this))
    }

    override fun onResume() {
        super.onResume()
        registerReceiver(batteryReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        updateBattery()
        applyLook()
    }

    override fun onPause() {
        super.onPause()
        runCatching { unregisterReceiver(batteryReceiver) }
    }

    override fun onBackPressed() {
        when {
            searchOverlay.visibility == View.VISIBLE -> searchOverlay.dismiss()
            notesSheet != null -> notesSheet?.dismiss()
            else -> Unit // На рабочем столе «назад» ничего не закрывает.
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_LOCATION && grantResults.any { it == PackageManager.PERMISSION_GRANTED }) {
            refreshWeather()
        }
    }

    // --------------------------------------------------------------- window

    private fun setupWindow() {
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
    }

    // --------------------------------------------------------------- layout

    private fun buildLayout() {
        root = FrameLayout(this)
        setContentView(root)

        wallpaper = WallpaperView(this)
        root.addView(wallpaper, match())

        content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18f), dp(2f), dp(18f), dp(4f))
        }
        root.addView(content, match())

        topBar = buildTopBar()
        content.addView(topBar, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(48f)))

        clock = ClockView(this).apply { setPadding(0, dp(4f), 0, 0) }
        content.addView(
            clock,
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        )

        searchPill = SearchPill(this).apply { setOnClickListener { openSearch() } }
        content.addView(
            searchPill,
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(42f)).apply { topMargin = dp(12f) }
        )

        content.addView(buildWidgetRow(), LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            dp(146f)
        ).apply { topMargin = dp(14f) })

        pager = AppPagerView(this).apply {
            onLaunch = { launch(it) }
            onLongPress = { app, _ -> showAppMenu(app) }
        }
        content.addView(
            pager,
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f).apply { topMargin = dp(6f) }
        )

        val dockHost = FrameLayout(this)
        dockFrame = DockBar(this)
        dockHost.addView(dockFrame, match())
        dock = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(10f), dp(4f), dp(10f), dp(12f))
        }
        dockHost.addView(dock, match())
        content.addView(
            dockHost,
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(90f)).apply { topMargin = dp(4f) }
        )

        searchOverlay = SearchOverlay(this).apply {
            onLaunch = { launch(it) }
        }
        root.addView(searchOverlay, match())

        if (!Prefs.showSearchBar) {
            searchPill.visibility = View.GONE
        }
    }

    private fun buildWidgetRow(): LinearLayout {
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        weather = WeatherWidget(this).apply {
            setOnClickListener { refreshWeather(requestPermission = true) }
        }
        row.addView(weather, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1.12f))

        val side = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        battery = BatteryWidget(this)
        notes = NotesWidget(this).apply {
            setOnClickListener { openNotes() }
        }
        side.addView(battery, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        side.addView(
            notes,
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f).apply { topMargin = dp(10f) }
        )
        row.addView(
            side,
            LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f).apply { leftMargin = dp(10f) }
        )
        return row
    }

    private fun buildTopBar(): LinearLayout {
        val bar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val brand = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val mark = View(this).apply {
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Prefs.accent.color)
                setStroke(dp(3f), withAlpha(Prefs.accent.color, 60))
            }
        }
        brand.addView(mark, LinearLayout.LayoutParams(dp(9f), dp(9f)))
        brand.addView(
            hudText(this, "NEXUS", 14f, NexusTheme.PRIMARY, R.font.orbitron, 0.24f),
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                .apply { leftMargin = dp(9f) }
        )
        bar.addView(brand, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))

        val today = hudText(
            this,
            DateTimeFormatter.ofPattern("d MMM", Locale("ru")).format(java.time.LocalDate.now()).uppercase(Locale("ru")),
            9.5f,
            NexusTheme.TERTIARY,
            R.font.rajdhani_semibold,
            0.18f
        )
        bar.addView(today, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply { rightMargin = dp(6f) })

        bar.addView(
            HudIconButton(this).apply {
                glyph = HudIconButton.Glyph.GEAR
                setOnClickListener {
                    startActivity(Intent(this@HomeActivity, SettingsActivity::class.java))
                    overridePendingTransition(0, 0)
                }
            },
            LinearLayout.LayoutParams(dp(42f), dp(42f))
        )
        return bar
    }

    private fun applyInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(root) { _, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
            content.updatePadding(top = bars.top + dp(4f), bottom = bars.bottom + dp(4f))
            insets
        }
    }

    // ----------------------------------------------------------------- data

    private fun loadApps() {
        AppRepository.load(this) { list ->
            apps = list
            pager.setApps(list)
            searchOverlay.setApps(list)
            buildDock()
        }
    }

    private fun buildDock() {
        dock.removeAllViews()
        val targets = listOf(
            AppRepository.SystemEntryType.PHONE,
            AppRepository.SystemEntryType.MESSAGES,
            AppRepository.SystemEntryType.CAMERA,
            AppRepository.SystemEntryType.BROWSER
        )
        for (type in targets) {
            val entry = AppRepository.resolveSystemEntry(this, type) ?: AppEntry(
                packageName = type.name,
                label = type.fallbackLabel,
                icon = null,
                customIntent = type.intent(this)
            )
            dock.addView(
                AppIconView(this).apply {
                    bind(entry)
                    setOnClickListener { launch(entry) }
                },
                LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f)
            )
        }
    }

    private fun launch(entry: AppEntry) {
        if (!entry.launch(this)) {
            toast(getString(R.string.app_launch_failed))
        }
    }

    private fun refreshWeather(requestPermission: Boolean = false) {
        val granted = checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (!granted && requestPermission && !permissionAsked) {
            permissionAsked = true
            requestPermissions(
                arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION),
                REQUEST_LOCATION
            )
        }
        val cachedTemp = Prefs.weatherTemp
        if (cachedTemp != Int.MIN_VALUE) {
            weather.temp = cachedTemp
            weather.code = Prefs.weatherCode
            weather.description = WeatherRepository.describe(Prefs.weatherCode)
        }
        if (granted) {
            weather.setLoading(true)
            WeatherRepository.refresh(this) { w ->
                runOnUiThread {
                    weather.setLoading(false)
                    if (w != null) {
                        weather.temp = w.tempC
                        weather.code = w.code
                        weather.description = WeatherRepository.describe(w.code)
                        weather.windKmh = w.windKmh
                        weather.humidity = w.humidity
                    }
                }
            }
        }
    }

    private fun updateBattery() {
        val bm = getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        val level = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        battery.set(level, bm.isCharging)
    }

    // -------------------------------------------------------------- actions

    private fun openSearch() {
        if (!Prefs.showSearchBar) return
        searchOverlay.show()
    }

    private fun openNotes() {
        if (notesSheet != null) return
        val sheet = NotesSheet(this)
        notesSheet = sheet
        sheet.setNotes(NotesStore.all(this))
        sheet.onAdd = { text ->
            sheet.setNotes(NotesStore.add(this, text))
            notes.setCount(NotesStore.count(this))
        }
        sheet.onDelete = { id ->
            sheet.setNotes(NotesStore.remove(this, id))
            notes.setCount(NotesStore.count(this))
        }
        sheet.onDismiss = { notesSheet = null }
        addContentView(sheet, match())
        sheet.animateIn()
    }

    private fun showAppMenu(entry: AppEntry) {
        AlertDialog.Builder(this, R.style.Theme_Nexus_Dialog)
            .setTitle(entry.label)
            .setItems(
                arrayOf(getString(R.string.app_info), getString(R.string.uninstall))
            ) { _, which ->
                val intent = when (which) {
                    0 -> Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                        .setData(android.net.Uri.parse("package:${entry.packageName}"))
                    else -> Intent(Intent.ACTION_DELETE)
                        .setData(android.net.Uri.parse("package:${entry.packageName}"))
                }
                runCatching { startActivity(intent) }
            }
            .setNegativeButton(R.string.close, null)
            .show()
    }

    /** Перерисовка после возврата из настроек. */
    private fun applyLook() {
        wallpaper.observePrefs()
        wallpaper.invalidate()
        topBar.removeAllViews()
        content.removeView(topBar)
        topBar = buildTopBar()
        content.addView(topBar, 0, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(48f)))
        searchPill.visibility = if (Prefs.showSearchBar) View.VISIBLE else View.GONE
        clock.invalidate()
        clock.requestLayout()
        pager.refreshLook()
        dockFrame.refreshAccent()
        weather.invalidate()
        battery.invalidate()
        notes.invalidate()
        searchPill.invalidate()
        searchOverlay.invalidate()
    }

    // ---------------------------------------------------------------- utils

    private fun match() = FrameLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.MATCH_PARENT
    )

    private fun dp(value: Float): Int = (value * resources.displayMetrics.density).toInt()

    private companion object {
        const val REQUEST_LOCATION = 41
    }

    private fun toast(message: String) {
        android.widget.Toast.makeText(this, message, android.widget.Toast.LENGTH_SHORT).show()
    }
}
