package com.sviridova.nexus.data

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Handler
import android.os.Looper
import android.os.Process
import androidx.core.graphics.drawable.toBitmap
import com.sviridova.nexus.model.AppEntry
import java.text.Collator
import java.util.Locale
import java.util.concurrent.Executors

/**
 * Loads the launchable app list.
 *
 * A launcher is a privacy-sensitive component, so we use [LauncherApps] first
 * (it is restricted to visible / launchable packages) with a PackageManager
 * fallback. Loading happens off the main thread and the result is delivered
 * back on it.
 */
object AppRepository {

    private val executor = Executors.newSingleThreadExecutor()
    private val main = Handler(Looper.getMainLooper())
    private val cache = HashMap<String, Bitmap>()

    fun load(context: Context, callback: (List<AppEntry>) -> Unit) {
        val app = context.applicationContext
        executor.execute {
            val entries = runCatching { loadBlocking(app) }.getOrDefault(emptyList())
            main.post { callback(entries) }
        }
    }

    private fun loadBlocking(context: Context): List<AppEntry> {
        val pm = context.packageManager
        val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as? LauncherApps
        val found = LinkedHashMap<String, AppEntry>()

        val activityList = runCatching {
            launcherApps?.getActivityList(null, Process.myUserHandle())
        }.getOrNull()

        if (!activityList.isNullOrEmpty()) {
            for (activity in activityList) {
                val key = activity.componentName.flattenToString()
                if (found.containsKey(key)) continue
                val label = activity.label?.toString()?.ifBlank { null }
                    ?: activity.applicationInfo.loadLabel(pm).toString()
                val icon = iconFor(context, activity.componentName?.packageName ?: activity.applicationInfo.packageName)
                found[key] = AppEntry(
                    packageName = activity.applicationInfo.packageName,
                    label = label,
                    icon = icon,
                    component = activity.componentName
                )
            }
        } else {
            val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
            val activities = runCatching { pm.queryIntentActivities(intent, 0) }.getOrDefault(emptyList())
            for (info in activities) {
                val component = ComponentName(info.activityInfo.packageName, info.activityInfo.name)
                val key = component.flattenToString()
                if (found.containsKey(key)) continue
                found[key] = AppEntry(
                    packageName = info.activityInfo.packageName,
                    label = info.loadLabel(pm).toString(),
                    icon = iconFor(context, info.activityInfo.packageName),
                    component = component
                )
            }
        }

        val collator = Collator.getInstance(Locale("ru", "RU")).apply { strength = Collator.PRIMARY }
        return found.values.sortedWith { a, b -> collator.compare(a.label, b.label) }
    }

    private fun iconFor(context: Context, packageName: String): Bitmap? {
        cache[packageName]?.let { return it }
        val pm = context.packageManager
        val drawable: Drawable = runCatching { pm.getApplicationIcon(packageName) }.getOrNull() ?: return null
        val bitmap = runCatching {
            val size = (context.resources.displayMetrics.density * 56).toInt().coerceAtLeast(96)
            drawable.toBitmap(size, size)
        }.getOrElse {
            runCatching { (drawable as BitmapDrawable).bitmap }.getOrNull()
        }
        return bitmap?.also { cache[packageName] = it }
    }

    /** Best-effort dock entry for a system action, falling back to a normal lookup. */
    fun resolveSystemEntry(context: Context, type: SystemEntryType): AppEntry? {
        val pm = context.packageManager
        val intent = type.intent(context)
        val resolved = runCatching {
            pm.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
        }.getOrNull() ?: return null

        val label = resolved.loadLabel(pm).toString()
        val icon = iconFor(context, resolved.activityInfo.packageName)
        return AppEntry(
            packageName = resolved.activityInfo.packageName,
            label = label,
            icon = icon,
            component = ComponentName(resolved.activityInfo.packageName, resolved.activityInfo.name),
            customIntent = intent
        )
    }

    enum class SystemEntryType(val fallbackLabel: String) {
        PHONE("Телефон"),
        MESSAGES("Сообщения"),
        CAMERA("Камера"),
        BROWSER("Браузер");

        fun intent(context: Context): Intent = when (this) {
            PHONE -> Intent(Intent.ACTION_DIAL)
            MESSAGES -> Intent(Intent.ACTION_SENDTO).setData(android.net.Uri.parse("smsto:"))
            CAMERA -> Intent("android.media.action.STILL_IMAGE_CAMERA")
            BROWSER -> Intent(Intent.ACTION_VIEW).setData(android.net.Uri.parse("https://www.google.com"))
        }
    }
}
