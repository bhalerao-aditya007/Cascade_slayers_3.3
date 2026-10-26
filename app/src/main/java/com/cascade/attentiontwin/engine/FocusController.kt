package com.cascade.attentiontwin.engine

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.cascade.attentiontwin.R

/**
 * Quiet layer. While Focus is on, Do-Not-Disturb silences sound/vibration/peek for everything.
 * Protected notifications are then re-alerted through a content-free relay on a bypass-DND channel.
 */
object FocusController {
    const val ALERT_CH = "twin_internal_alert"
    const val SVC_CH = "twin_internal_svc"
    private var prevFilter: Int? = null
    @Volatile var dndActive = false
        private set

    private fun nm(c: Context) = c.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    fun policyGranted(c: Context) = nm(c).isNotificationPolicyAccessGranted

    fun ensureServiceChannel(c: Context) {
        nm(c).createNotificationChannel(
            NotificationChannel(SVC_CH, "Focus status", NotificationManager.IMPORTANCE_LOW)
        )
    }

    fun enter(c: Context) {
        val n = nm(c)
        ensureServiceChannel(c)
        if (!n.isNotificationPolicyAccessGranted) return
        if (prevFilter == null) prevFilter = n.currentInterruptionFilter
        // Created only after policy access exists so setBypassDnd actually sticks.
        n.createNotificationChannel(
            NotificationChannel(ALERT_CH, "Protected alerts", NotificationManager.IMPORTANCE_HIGH)
                .apply { setBypassDnd(true) }
        )
        n.notificationPolicy = NotificationManager.Policy(
            NotificationManager.Policy.PRIORITY_CATEGORY_CALLS or NotificationManager.Policy.PRIORITY_CATEGORY_ALARMS,
            NotificationManager.Policy.PRIORITY_SENDERS_ANY,
            NotificationManager.Policy.PRIORITY_SENDERS_ANY,
            NotificationManager.Policy.SUPPRESSED_EFFECT_PEEK or NotificationManager.Policy.SUPPRESSED_EFFECT_SCREEN_ON
        )
        n.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY)
        dndActive = true
    }

    fun exit(c: Context) {
        val n = nm(c)
        if (n.isNotificationPolicyAccessGranted) {
            n.setInterruptionFilter(prevFilter ?: NotificationManager.INTERRUPTION_FILTER_ALL)
        }
        prevFilter = null
        dndActive = false
    }

    /** DND also silences protected notifications, so re-alert with the app name only (no content). */
    fun relayProtected(c: Context, pkg: String) {
        if (!dndActive) return
        val label = try {
            c.packageManager.getApplicationLabel(c.packageManager.getApplicationInfo(pkg, 0)).toString()
        } catch (_: Exception) { pkg }
        try {
            NotificationManagerCompat.from(c).notify(
                pkg.hashCode(),
                NotificationCompat.Builder(c, ALERT_CH)
                    .setSmallIcon(R.drawable.ic_launcher_foreground)
                    .setContentTitle("Protected: $label")
                    .setContentText("Open $label to view")
                    .setAutoCancel(true)
                    .build()
            )
        } catch (_: SecurityException) {}
    }
}
