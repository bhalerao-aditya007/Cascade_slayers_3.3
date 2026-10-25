package com.cascade.attentiontwin.engine

import android.service.notification.NotificationListenerService
import com.cascade.attentiontwin.data.AppDb
import com.cascade.attentiontwin.data.ChannelStat
import com.cascade.attentiontwin.data.OutcomeRow
import com.cascade.attentiontwin.data.Prefs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap

class OutcomeTracker(
    private val db: AppDb,
    private val prefs: Prefs,
    private val contextTracker: ContextTracker,
    private val statCache: ConcurrentHashMap<String, ChannelStat>,
    private val ioScope: CoroutineScope
) {
    private val closeMutex = Mutex()

    fun sweepPending() {
        ioScope.launch {
            val now = System.currentTimeMillis()
            val pendingIds = db.dao().pendingWindows(now)
            for (id in pendingIds) {
                close(id)
            }
        }
    }

    suspend fun close(eventId: Long) = closeMutex.withLock {
        // Idempotency: skip if outcome already exists
        val existingEvent = db.dao().eventById(eventId) ?: return
        val existingDecision = db.dao().decisionByEventId(eventId) ?: return
        val windowEnd = existingEvent.postTs + 10 * 60_000L

        val intervals = db.dao().intervalsOverlapping(existingEvent.postTs, windowEnd)
        // If no screen interval info recorded in the database, consider censored
        val censored = intervals.isEmpty()

        val unlocks10m = db.dao().unlocksBetween(existingEvent.postTs, windowEnd)
        val phoneMin10m = contextTracker.phoneMinutes(existingEvent.postTs, windowEnd)

        val removalReason = existingDecision.removalReason
        val removedTs = existingDecision.removedTs

        val inWindow = removedTs != null && removedTs in existingEvent.postTs..windowEnd
        val clicked = removalReason == NotificationListenerService.REASON_CLICK && inWindow
        val dismissed = (removalReason == NotificationListenerService.REASON_CANCEL ||
                removalReason == NotificationListenerService.REASON_CANCEL_ALL) && inWindow
        val clickedWithin2m = clicked && removedTs != null && (removedTs - existingEvent.postTs < 120_000L)
        val engaged = unlocks10m > 0 || clicked || dismissed

        val outcome = OutcomeRow(
            eventId = eventId,
            windowEnd = windowEnd,
            engaged = engaged,
            clicked = clicked,
            dismissed = dismissed,
            unlocks10m = unlocks10m,
            phoneMin10m = phoneMin10m,
            clickedWithin2m = clickedWithin2m,
            digestFeedback = null,
            censored = censored
        )
        db.dao().insertOutcome(outcome)

        if (censored || prefs.current.learningPaused) {
            return
        }

        val cacheKey = "${existingEvent.pkg}|${existingEvent.channelId ?: ""}"
        val st = statCache[cacheKey] ?: db.dao().getChannelStat(existingEvent.pkg, existingEvent.channelId ?: "")
        if (st == null) return

        if (existingDecision.action == "SHOW") {
            // SGD update on bShow
            val y = if (engaged) 1.0 else 0.0
            val grad = y - existingDecision.pShow
            st.bShow = (st.bShow + 0.05 * grad).coerceIn(-2.0, 2.0)

            if (engaged) {
                val obs = phoneMin10m
                if (st.nPull < 5) {
                    st.mu = (st.mu * (st.nPull + 2) + obs) / (st.nPull + 3)
                } else {
                    st.mu = 0.7 * st.mu + 0.3 * obs
                }
                val diff = obs - st.mu
                st.varPull = 0.7 * st.varPull + 0.3 * (diff * diff)
                st.nPull++
                st.nEngaged++
            }

            if (clickedWithin2m) {
                st.a += 1.0
            } else if (dismissed) {
                st.b += 1.0
            } else {
                st.b += 0.3
            }

            if (existingDecision.phoneState != "LOCKED") {
                val switchSignal = if (clickedWithin2m) 1.0 else 0.0
                st.pSwitch = 0.9 * st.pSwitch + 0.1 * switchSignal
            }
        } else if (existingDecision.action == "HOLD") {
            // Action HOLD: no importance update unless released and clicked within 60s
            val releasedTs = existingDecision.releasedTs
            if (releasedTs != null && removedTs != null &&
                removalReason == NotificationListenerService.REASON_CLICK &&
                (removedTs - releasedTs in 0..60_000L)
            ) {
                st.a += 1.0
            }
        }

        st.updatedTs = System.currentTimeMillis()
        statCache[cacheKey] = st
        db.dao().updateChannelStat(st)
    }

    fun submitFeedback(eventId: Long, kind: String) {
        ioScope.launch {
            val event = db.dao().eventById(eventId) ?: return@launch
            val cacheKey = "${event.pkg}|${event.channelId ?: ""}"
            val st = statCache[cacheKey] ?: db.dao().getChannelStat(event.pkg, event.channelId ?: "") ?: return@launch

            when (kind) {
                "mattered" -> st.a += 2.0
                "not_matter" -> st.b += 1.0
                "sooner" -> {
                    st.a += 3.0
                    st.prot = true
                    st.never = true
                }
            }
            st.updatedTs = System.currentTimeMillis()
            statCache[cacheKey] = st
            db.dao().updateChannelStat(st)

            // Update digestFeedback in outcome table if exists
            val decision = db.dao().decisionByEventId(eventId)
            if (decision != null) {
                db.dao().insertOutcome(
                    OutcomeRow(
                        eventId = eventId,
                        windowEnd = event.postTs + 10 * 60_000L,
                        engaged = true,
                        clicked = false,
                        dismissed = false,
                        unlocks10m = 0,
                        phoneMin10m = 0.0,
                        clickedWithin2m = false,
                        digestFeedback = kind,
                        censored = false
                    )
                )
            }
        }
    }
}
