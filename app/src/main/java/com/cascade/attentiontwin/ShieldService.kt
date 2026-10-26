package com.cascade.attentiontwin

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Focus shield: while Focus is on, polls the foreground app and bounces
 * chosen apps to ShieldActivity. Uses UsageStats only (no accessibility, no content).
 */
class ShieldService : Service() {

    companion object {
        const val CHANNEL_ID = "twin_internal_svc"
        private const val NOTIF_ID = 4242

        /** Epoch ms until which the shield is suspended ("Allow 5 minutes"). */
        @Volatile
        var passUntil: Long = 0L
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var loop: Job? = null
    private var lastBounceTs = 0L

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startAsForeground()
        if (loop?.isActive != true) {
            loop = scope.launch {
                while (isActive) {
                    try {
                        tick()
                    } catch (_: Throwable) {
                        // unknown = do nothing
                    }
                    delay(1000L)
                }
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private fun startAsForeground() {
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Focus status", NotificationManager.IMPORTANCE_LOW)
        )
        val n: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Focus is on")
            .setContentText("Chosen apps are paused")
            .setOngoing(true)
            .build()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIF_ID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIF_ID, n)
        }
    }

    private fun tick() {
        val now = System.currentTimeMillis()
        if (now < passUntil) return
        if (now - lastBounceTs < 2500L) return

        val s = Graph.prefs.current
        if (!s.focusActive || !s.shieldEnabled || s.shieldPkgs.isEmpty()) return
        if (!Graph.usageHelper.usageGranted()) return

        val pkg = currentForeground(now) ?: return
        if (pkg == packageName || pkg !in s.shieldPkgs) return

        lastBounceTs = now
        val i = Intent(this, ShieldActivity::class.java).apply {
            putExtra("pkg", pkg)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        startActivity(i)
    }

    /** Most recent resumed package in the last 10 s, or null if it has since paused. */
    private fun currentForeground(now: Long): String? {
        val usm = getSystemService(USAGE_STATS_SERVICE) as? UsageStatsManager ?: return null
        val events = usm.queryEvents(now - 10_000L, now)
        val e = UsageEvents.Event()
        var cur: String? = null
        while (events.hasNextEvent()) {
            events.getNextEvent(e)
            when (e.eventType) {
                UsageEvents.Event.ACTIVITY_RESUMED -> cur = e.packageName
                UsageEvents.Event.ACTIVITY_PAUSED -> if (cur == e.packageName) cur = null
            }
        }
        return cur
    }
}
