package com.cascade.attentiontwin.engine

import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.PowerManager
import androidx.core.content.ContextCompat
import com.cascade.attentiontwin.data.AppDb
import com.cascade.attentiontwin.data.Prefs
import com.cascade.attentiontwin.data.ScreenInterval
import com.cascade.attentiontwin.data.UnlockEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.ArrayDeque
import java.util.Calendar

class ContextTracker(
    private val context: Context,
    private val db: AppDb,
    private val prefs: Prefs,
    private val usageHelper: UsageHelper,
    private val groups: AppGroups,
    private val ioScope: CoroutineScope
) {
    private val notifHistory = ArrayDeque<Long>()
    private val unlockHistory = ArrayDeque<Long>()

    @Volatile
    private var lastUnlockTs: Long = 0L

    @Volatile
    private var screenOn: Boolean = false

    @Volatile
    private var openIntervalStart: Long? = null

    @Volatile
    private var openIntervalDbId: Long? = null

    @Volatile
    private var receiverRegistered = false

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context?, intent: Intent?) {
            val now = System.currentTimeMillis()
            when (intent?.action) {
                Intent.ACTION_SCREEN_ON -> onScreenOn(now)
                Intent.ACTION_SCREEN_OFF -> onScreenOff(now)
                Intent.ACTION_USER_PRESENT -> onUserPresent(now)
            }
        }
    }

    fun onConnected() {
        val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        screenOn = pm?.isInteractive ?: false

        if (!receiverRegistered) {
            val filter = IntentFilter().apply {
                addAction(Intent.ACTION_SCREEN_ON)
                addAction(Intent.ACTION_SCREEN_OFF)
                addAction(Intent.ACTION_USER_PRESENT)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
            } else {
                context.registerReceiver(receiver, filter)
            }
            receiverRegistered = true
        }

        ioScope.launch {
            rebuild(System.currentTimeMillis())
        }
    }

    fun onDisconnected() {
        if (receiverRegistered) {
            try {
                context.unregisterReceiver(receiver)
            } catch (_: Exception) {}
            receiverRegistered = false
        }
        val now = System.currentTimeMillis()
        if (openIntervalStart != null) {
            onScreenOff(now)
        }
    }

    private suspend fun rebuild(now: Long) {
        val cutoff = now - 30 * 60_000L
        val recentIntervals = db.dao().recentScreenIntervals(cutoff)
        val recentUnlocks = db.dao().recentUnlocks(cutoff)
        val latestInterval = db.dao().latestScreenInterval()

        synchronized(unlockHistory) {
            unlockHistory.clear()
            recentUnlocks.forEach { unlockHistory.addLast(it.ts) }
            lastUnlockTs = recentUnlocks.lastOrNull()?.ts ?: 0L
        }

        if (latestInterval != null && latestInterval.endTs == null) {
            if (screenOn) {
                openIntervalStart = latestInterval.startTs
                openIntervalDbId = latestInterval.id
            } else {
                latestInterval.endTs = now
                ioScope.launch { db.dao().updateScreenInterval(latestInterval) }
                openIntervalStart = null
                openIntervalDbId = null
            }
        } else if (screenOn) {
            openIntervalStart = now
            ioScope.launch {
                val id = db.dao().insertScreenInterval(ScreenInterval(startTs = now, endTs = null))
                openIntervalDbId = id
            }
        }
    }

    private fun onScreenOn(now: Long) {
        screenOn = true
        openIntervalStart = now
        ioScope.launch {
            val id = db.dao().insertScreenInterval(ScreenInterval(startTs = now, endTs = null))
            openIntervalDbId = id
        }
    }

    private fun onScreenOff(now: Long) {
        screenOn = false
        val start = openIntervalStart
        val id = openIntervalDbId
        openIntervalStart = null
        openIntervalDbId = null

        if (start != null && id != null) {
            ioScope.launch {
                db.dao().updateScreenInterval(ScreenInterval(id = id, startTs = start, endTs = now))
            }
        }
    }

    private fun onUserPresent(now: Long) {
        lastUnlockTs = now
        synchronized(unlockHistory) {
            unlockHistory.addLast(now)
            pruneHistory(unlockHistory, now)
        }
        ioScope.launch {
            db.dao().insertUnlock(UnlockEvent(ts = now))
        }
    }

    fun recordNotification(now: Long) {
        synchronized(notifHistory) {
            notifHistory.addLast(now)
            pruneHistory(notifHistory, now)
        }
    }

    private fun pruneHistory(deque: ArrayDeque<Long>, now: Long) {
        val cutoff = now - 30 * 60_000L
        while (deque.isNotEmpty() && deque.first < cutoff) {
            deque.removeFirst()
        }
    }

    fun build(now: Long, pkg: String, ownPkg: String): Ctx {
        val km = context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
        val isLocked = km?.isKeyguardLocked ?: false
        val locked = isLocked || (prefs.current.simForceLocked && pkg == ownPkg)

        val minsSinceUnlock = if (lastUnlockTs > 0L) {
            (now - lastUnlockTs).toDouble() / 60_000.0
        } else {
            600.0
        }

        val fg = usageHelper.foreground(now)
        val usageKnown = usageHelper.usageGranted()

        val state = when {
            locked -> PhoneState.LOCKED
            fg.pkg == pkg && pkg != ownPkg -> PhoneState.IN_SOURCE_APP
            fg.group in setOf("productivity", "video") -> PhoneState.IN_TASK_APP
            else -> PhoneState.IN_OTHER_APP
        }

        val cal = Calendar.getInstance().apply { timeInMillis = now }
        val hour = cal.get(Calendar.HOUR_OF_DAY)
        val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
        val weekend = (dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY)

        val notifs30 = synchronized(notifHistory) {
            pruneHistory(notifHistory, now)
            notifHistory.size
        }
        val unlocks30 = synchronized(unlockHistory) {
            pruneHistory(unlockHistory, now)
            unlockHistory.size
        }

        return Ctx(
            nowMs = now,
            hour = hour,
            weekend = weekend,
            notifs30 = notifs30,
            unlocks30 = unlocks30,
            minsSinceUnlock = minsSinceUnlock,
            screenOn = screenOn,
            state = state,
            fgPkg = fg.pkg,
            fgGroup = fg.group,
            usageKnown = usageKnown,
            waitingForCode = fg.waitingForCode
        )
    }

    suspend fun phoneMinutes(t0: Long, t1: Long): Double {
        val intervals = db.dao().intervalsOverlapping(t0, t1)
        var totalMs = 0L
        for (i in intervals) {
            val s = maxOf(i.startTs, t0)
            val e = minOf(i.endTs ?: System.currentTimeMillis(), t1)
            if (e > s) {
                totalMs += (e - s)
            }
        }
        val currentStart = openIntervalStart
        if (currentStart != null && currentStart < t1) {
            val s = maxOf(currentStart, t0)
            val e = minOf(System.currentTimeMillis(), t1)
            if (e > s) {
                // If it wasn't already in intervals from DB
                if (intervals.none { it.id == openIntervalDbId }) {
                    totalMs += (e - s)
                }
            }
        }
        return totalMs.toDouble() / 60_000.0
    }
}
