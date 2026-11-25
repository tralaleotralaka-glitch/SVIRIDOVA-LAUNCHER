package com.sviridova.nexus.model

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap

/** A launchable app (or a system shortcut such as the dialer) on the home screen. */
data class AppEntry(
    val packageName: String,
    val label: String,
    val icon: Bitmap?,
    val component: ComponentName? = null,
    val customIntent: Intent? = null
) {
    fun launch(context: Context): Boolean {
        val intent = when {
            customIntent != null -> Intent(customIntent)
            component != null -> Intent(Intent.ACTION_MAIN)
                .addCategory(Intent.CATEGORY_LAUNCHER)
                .setComponent(component)
            else -> context.packageManager.getLaunchIntentForPackage(packageName)
        } ?: return false
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
        return runCatching {
            context.startActivity(intent)
            true
        }.getOrDefault(false)
    }

    val key: String
        get() = component?.flattenToString() ?: packageName
}
