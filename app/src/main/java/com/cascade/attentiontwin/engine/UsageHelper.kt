package com.cascade.attentiontwin.engine

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Process
import java.util.Locale

class UsageHelper(private val context: Context, private val groups: AppGroups) {

    data class ForegroundResult(
        val pkg: String?,
        val group: String?,
        val waitingForCode: Boolean
    )

    @Volatile
    private var cachedResult = ForegroundResult(null, null, false)
    @Volatile
    private var lastQueryTs = 0L

    fun usageGranted(): Boolean {
        return try {
            val ops = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager ?: return false
            val mode = ops.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
            mode == AppOpsManager.MODE_ALLOWED
        } catch (_: Exception) {
            false
        }
    }

    fun foreground(now: Long): ForegroundResult {
        if (!usageGranted()) {
            return ForegroundResult(null, null, false)
        }
        if (now - lastQueryTs < 5000L) {
            return cachedResult
        }

        val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
            ?: return ForegroundResult(null, null, false)

        val beginTime = now - 20 * 60_000L
        val events = try {
            usm.queryEvents(beginTime, now)
        } catch (_: Exception) {
            return ForegroundResult(null, null, false)
        }

        val event = UsageEvents.Event()
        var currentPkg: String? = null
        var waitingForCode = false

        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            val pkg = event.packageName ?: continue
            val eventType = event.eventType
            if (eventType == UsageEvents.Event.ACTIVITY_RESUMED || eventType == 1 /* MOVE_TO_FOREGROUND */) {
                currentPkg = pkg
                val eventGroup = groups.groupOf(pkg, null)
                val age = now - event.timeStamp
                if (age <= 90_000L) {
                    val pkgLower = pkg.lowercase(Locale.US)
                    if (eventGroup == "finance" || eventGroup == "shopping" ||
                        pkgLower.contains("uber") || pkgLower.contains("ola") ||
                        pkgLower.contains("swiggy") || pkgLower.contains("zomato") ||
                        pkgLower.contains("login") || pkgLower.contains("auth")
                    ) {
                        waitingForCode = true
                    }
                }
            } else if (eventType == UsageEvents.Event.ACTIVITY_PAUSED || eventType == 2 /* MOVE_TO_BACKGROUND */) {
                if (currentPkg == pkg) {
                    currentPkg = null
                }
            }
        }

        val fgGroup = if (currentPkg != null) groups.groupOf(currentPkg, null) else null
        if (fgGroup == "finance" || fgGroup == "shopping") {
            waitingForCode = true
        }

        val result = ForegroundResult(currentPkg, fgGroup, waitingForCode)
        cachedResult = result
        lastQueryTs = now
        return result
    }
}
