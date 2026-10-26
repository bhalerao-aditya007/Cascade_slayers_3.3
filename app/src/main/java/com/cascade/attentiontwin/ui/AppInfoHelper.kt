package com.cascade.attentiontwin.ui

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import com.cascade.attentiontwin.Graph

data class AppInfo(
    val pkg: String,
    val label: String,
    val icon: Bitmap?,
    val group: String,
    val isDistraction: Boolean
)

object AppInfoHelper {
    private var cachedApps: List<AppInfo>? = null

    fun getInstalledApps(context: Context, forceRefresh: Boolean = false): List<AppInfo> {
        if (!forceRefresh) {
            cachedApps?.let { return it }
        }

        val pm = context.packageManager
        val ownPkg = context.packageName
        val intent = Intent(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_LAUNCHER) }
        val resolveList = try {
            pm.queryIntentActivities(intent, 0)
        } catch (_: Exception) {
            emptyList()
        }

        val apps = mutableListOf<AppInfo>()
        val seen = mutableSetOf<String>()

        for (ri in resolveList) {
            val pkg = ri.activityInfo?.packageName ?: continue
            if (pkg == ownPkg || seen.contains(pkg)) continue
            seen.add(pkg)

            val label = try {
                ri.loadLabel(pm).toString()
            } catch (_: Exception) {
                pkg.substringAfterLast('.').replaceFirstChar { it.uppercase() }
            }

            val icon = try {
                drawableToBitmap(ri.loadIcon(pm), 96)
            } catch (_: Exception) {
                null
            }

            val group = try {
                Graph.groups.groupOf(pkg, null)
            } catch (_: Exception) {
                "other"
            }

            val pkgLower = pkg.lowercase()
            val isDistraction = group in setOf("social", "video", "shopping", "messaging") ||
                    pkgLower.contains("whatsapp") || pkgLower.contains("instagram") ||
                    pkgLower.contains("youtube") || pkgLower.contains("telegram") ||
                    pkgLower.contains("twitter") || pkgLower.contains("snapchat") ||
                    pkgLower.contains("reddit") || pkgLower.contains("tiktok") ||
                    pkgLower.contains("netflix") || pkgLower.contains("facebook") ||
                    pkgLower.contains("discord") || pkgLower.contains("threads")

            apps.add(AppInfo(pkg, label, icon, group, isDistraction))
        }

        // If for any reason resolveList was too small (OEM restriction), also check installed applications
        if (apps.size < 5) {
            try {
                val installed = pm.getInstalledApplications(PackageManager.GET_META_DATA)
                for (ai in installed) {
                    if (ai.packageName == ownPkg || seen.contains(ai.packageName)) continue
                    val isLaunchable = pm.getLaunchIntentForPackage(ai.packageName) != null
                    if (!isLaunchable && (ai.flags and ApplicationInfo.FLAG_SYSTEM) != 0) continue

                    seen.add(ai.packageName)
                    val label = try { ai.loadLabel(pm).toString() } catch (_: Exception) { ai.packageName }
                    val icon = try { drawableToBitmap(ai.loadIcon(pm), 96) } catch (_: Exception) { null }
                    val group = try { Graph.groups.groupOf(ai.packageName, null) } catch (_: Exception) { "other" }
                    val isDistraction = group in setOf("social", "video", "shopping", "messaging")

                    apps.add(AppInfo(ai.packageName, label, icon, group, isDistraction))
                }
            } catch (_: Exception) {}
        }

        val sorted = apps.sortedWith(
            compareByDescending<AppInfo> { it.isDistraction }
                .thenBy { it.label.lowercase() }
        )
        cachedApps = sorted
        return sorted
    }

    private fun drawableToBitmap(drawable: Drawable?, size: Int): Bitmap? {
        if (drawable == null) return null
        return try {
            if (drawable is BitmapDrawable && drawable.bitmap != null) {
                Bitmap.createScaledBitmap(drawable.bitmap, size, size, true)
            } else {
                val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bitmap)
                drawable.setBounds(0, 0, canvas.width, canvas.height)
                drawable.draw(canvas)
                bitmap
            }
        } catch (_: Throwable) {
            null
        }
    }
}
