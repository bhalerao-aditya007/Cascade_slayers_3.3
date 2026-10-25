package com.cascade.attentiontwin.engine

import com.cascade.attentiontwin.data.AppDb
import com.cascade.attentiontwin.data.CtrlCount
import com.cascade.attentiontwin.data.CtrlProbe
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.concurrent.ConcurrentHashMap

class CtrlTracker(
    private val db: AppDb,
    private val contextTracker: ContextTracker,
    private val ioScope: CoroutineScope
) {
    private val blockCounts = ConcurrentHashMap<Int, Pair<Int, Int>>() // block -> (n, k)
    @Volatile
    private var cachedMuS: Double = 3.0
    @Volatile
    private var lastSweepTs: Long = 0L

    fun init() {
        ioScope.launch {
            refreshCounts()
            sweepProbes(System.currentTimeMillis())
        }
    }

    suspend fun refreshCounts() {
        val counts = db.dao().ctrlCounts()
        blockCounts.clear()
        for (c in counts) {
            blockCounts[c.block] = Pair(c.n, c.k)
        }
        val muSample = db.dao().muSamples()
        val totalPhoneMin = muSample.avgMin * muSample.n
        cachedMuS = (totalPhoneMin + 3.0 * 3.0) / (muSample.n + 3.0)
    }

    suspend fun sweepProbes(now: Long) {
        val allProbes = db.dao().allCtrlProbes()
        val lastProbeTime = allProbes.lastOrNull()?.ts ?: (now - 24 * 60 * 60_000L)
        val gridMs = 15 * 60_000L
        val maxT = now - 10 * 60_000L

        var t = ((lastProbeTime / gridMs) + 1) * gridMs
        while (t <= maxT) {
            // Check if screen was off at t (not inside any screen_interval)
            val overlapping = db.dao().intervalsOverlapping(t, t + 1)
            val screenOffAtT = overlapping.isEmpty()

            if (screenOffAtT) {
                // Check no notification event in [t - 10 min, t]
                val eventsBefore = db.dao().eventsSince(t - 10 * 60_000L)
                val noNotif = eventsBefore.none { it.postTs in (t - 10 * 60_000L)..t }

                if (noNotif) {
                    val unlocks = db.dao().unlocksBetween(t, t + 10 * 60_000L)
                    val y = unlocks > 0
                    val phoneMin = contextTracker.phoneMinutes(t, t + 10 * 60_000L)

                    val cal = Calendar.getInstance().apply { timeInMillis = t }
                    val hour = cal.get(Calendar.HOUR_OF_DAY)
                    val block = (hour / 4).coerceIn(0, 5)

                    db.dao().insertCtrlProbe(CtrlProbe(ts = t, block = block, y = y, phoneMin = phoneMin))
                }
            }
            t += gridMs
        }

        refreshCounts()
        lastSweepTs = now
    }

    fun pCtrl(block: Int, p0: Double): Double {
        val pair = blockCounts[block] ?: Pair(0, 0)
        val n = pair.first
        val k = pair.second
        val m = 12.0
        return (k + m * p0) / (n + m)
    }

    fun muS(): Double {
        return cachedMuS
    }
}
