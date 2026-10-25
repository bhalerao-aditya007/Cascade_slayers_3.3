package com.cascade.attentiontwin

import android.app.Notification
import android.content.ComponentName
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.cascade.attentiontwin.engine.Enforcer
import com.cascade.attentiontwin.engine.NotifFeatures

class AttentionListener : NotificationListenerService(), Enforcer {

    override fun onListenerConnected() {
        super.onListenerConnected()
        Graph.onConnected(this)
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        Graph.onDisconnected()
        try {
            requestRebind(ComponentName(this, AttentionListener::class.java))
        } catch (e: Exception) {
            Log.w("TwinListener", "requestRebind failed", e)
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?, rm: RankingMap?) {
        if (sbn == null || rm == null) return
        try {
            Graph.engine.onPosted(extract(sbn, rm), this)
        } catch (t: Throwable) {
            // unknown = SHOW
            Log.w("TwinListener", "decision failed, leaving notification alone", t)
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?, rm: RankingMap?, reason: Int) {
        if (sbn == null) return
        try {
            Graph.engine.onRemoved(sbn.key, reason, System.currentTimeMillis())
        } catch (_: Throwable) {}
    }

    override fun snooze(key: String, ms: Long): Boolean = try {
        snoozeNotification(key, ms)
        true
    } catch (e: Exception) {
        Log.w("TwinListener", "snoozeNotification failed", e)
        false
    }

    override fun stillActive(key: String): Boolean = try {
        activeNotifications?.any { it.key == key } ?: false
    } catch (e: Exception) {
        false
    }

    private fun extract(sbn: StatusBarNotification, rm: RankingMap): NotifFeatures {
        val r = Ranking()
        rm.getRanking(sbn.key, r)
        val n = sbn.notification
        val ch = r.channel
        return NotifFeatures(
            key = sbn.key,
            pkg = sbn.packageName,
            postTime = sbn.postTime,
            channelId = ch?.id,
            channelName = ch?.name?.toString(),
            category = n.category,
            importance = r.importance,
            bypassDnd = ch?.canBypassDnd() ?: false,
            isConversation = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && r.isConversation,
            ongoing = sbn.isOngoing,
            isGroupSummary = (n.flags and Notification.FLAG_GROUP_SUMMARY) != 0,
            isForegroundService = (n.flags and Notification.FLAG_FOREGROUND_SERVICE) != 0,
            hasReplyAction = n.actions?.any { a -> a.remoteInputs?.isNotEmpty() == true } == true,
            isDefaultSms = Graph.groups.isDefaultSms(sbn.packageName),
            isAuthenticator = Graph.groups.isAuthenticator(sbn.packageName)
        )
    }
}
