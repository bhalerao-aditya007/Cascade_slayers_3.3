package com.cascade.attentiontwin.engine

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.cascade.attentiontwin.data.AppDb
import com.cascade.attentiontwin.data.ChannelStat
import com.cascade.attentiontwin.data.DecisionRow
import com.cascade.attentiontwin.data.EventRow
import com.cascade.attentiontwin.data.Mode
import com.cascade.attentiontwin.data.Prefs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Random
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap

interface Enforcer {
    fun snooze(key: String, ms: Long): Boolean
    fun stillActive(key: String): Boolean
}

class Engine(
    private val context: Context,
    private val db: AppDb,
    private val prefs: Prefs,
    private val models: ModelStore,
    private val groups: AppGroups,
    private val contextTracker: ContextTracker,
    private val ctrlTracker: CtrlTracker,
    private val outcomes: OutcomeTracker,
    private val statCache: ConcurrentHashMap<String, ChannelStat>,
    private val ioScope: CoroutineScope
) {
    private val tag = "TwinEngine"
    private val mainHandler = Handler(Looper.getMainLooper())
    val heldKeys = ConcurrentHashMap<String, Long>()
    val idByKey = ConcurrentHashMap<String, CompletableFuture<Long>>()
    private val rnd = Random()

    fun reloadHeldKeys() {
        ioScope.launch {
            val unreleased = db.dao().unreleasedHolds()
            for (d in unreleased) {
                val ev = db.dao().eventById(d.eventId)
                if (ev != null && d.releaseAt != null) {
                    heldKeys[ev.key] = d.releaseAt
                }
            }
        }
    }

    fun getOrCreateStat(f: NotifFeatures, group: String): ChannelStat {
        val cacheKey = "${f.pkg}|${f.channelId ?: ""}"
        return statCache.computeIfAbsent(cacheKey) {
            val beta = Policy.priorBeta(f, group)
            val poolPrior = POOL_PRIOR[group] ?: (3.0 to 3.0)
            val pSwitchPrior = when (group) {
                "messaging" -> 0.5
                "social" -> 0.3
                else -> 0.15
            }
            ChannelStat(
                pkg = f.pkg,
                channelId = f.channelId ?: "",
                group = group,
                a = beta.a,
                b = beta.b,
                nSeen = 0,
                nEngaged = 0,
                mu = poolPrior.first,
                varPull = poolPrior.second * poolPrior.second,
                nPull = 0,
                bShow = 0.0,
                pSwitch = pSwitchPrior,
                prot = false,
                never = false,
                updatedTs = System.currentTimeMillis()
            ).also { newStat ->
                ioScope.launch { db.dao().insertChannelStat(newStat) }
            }
        }
    }

    fun onPosted(f: NotifFeatures, enf: Enforcer) {
        val now = System.currentTimeMillis()

        // 2. Ignore internal and group summaries
        if (f.channelId == "twin_internal" || f.isGroupSummary) {
            return
        }

        // 3. Re-post of a held key
        val releaseAt = heldKeys[f.key]
        if (releaseAt != null) {
            if (now >= releaseAt - 5000L) {
                heldKeys.remove(f.key)
                ioScope.launch {
                    db.dao().markReleased(f.key, now)
                }
            } else {
                Log.d(tag, "Update while held: ${f.key}")
            }
            return
        }

        // 4. Group and stats
        val group = groups.groupOf(f.pkg, f.channelId)
        val st = getOrCreateStat(f, group)

        // 5. Context
        val ownPkg = context.packageName
        val ctx = contextTracker.build(now, f.pkg, ownPkg)
        contextTracker.recordNotification(now)

        val s = prefs.current

        // 6. Gate 0: Focus Active check
        val d: Decision
        if (!s.focusActive) {
            d = Decision(
                hold = false,
                gate = "FOCUS_OFF",
                reason = "Focus is off; learning only.",
                pImpMean = 0.0,
                pImpUp = 0.0,
                pShow = 0.0,
                pCtrl = 0.0,
                costShow = 0.0,
                costHold = 0.0,
                benefit = 0.0,
                randomized = false,
                propensityHold = 0.0,
                phoneState = ctx.state
            )
        } else {
            val hourBlock = (ctx.hour / 4).coerceIn(0, 5)
            val modelCtrlP = models.ctrl.p(featureVector(ctx, false, null, true))
            val personalCtrl = ctrlTracker.pCtrl(hourBlock, modelCtrlP)
            val muS = ctrlTracker.muS()
            d = Policy.decide(f, group, ctx, st, s, models, personalCtrl, muS, rnd)
        }

        // 8. Enforce
        var enforcement = "none"
        var finalHold = d.hold
        var releaseTime: Long? = null

        if (d.hold) {
            when (s.mode) {
                Mode.SNOOZE -> {
                    val (relAt, duration) = Policy.digestRelease(now, s.maxHoldMin, s.digestMin)
                    releaseTime = relAt
                    val ok = enf.snooze(f.key, duration)
                    if (ok) {
                        heldKeys[f.key] = relAt
                        enforcement = "applied"
                    } else {
                        enforcement = "failed"
                        finalHold = false
                    }

                    // 2-second delayed check on main Handler
                    mainHandler.postDelayed({
                        if (enf.stillActive(f.key)) {
                            Log.w(tag, "Snooze failed: key still active after 2s: ${f.key}")
                            heldKeys.remove(f.key)
                            idByKey[f.key]?.thenAccept { eventId ->
                                ioScope.launch {
                                    val currentDec = db.dao().decisionByEventId(eventId)
                                    if (currentDec != null) {
                                        db.dao().updateDecision(currentDec.copy(enforcement = "failed", action = "SHOW"))
                                    }
                                }
                            }
                        }
                    }, 2000L)
                }
                Mode.SHADOW -> {
                    enforcement = "none"
                }
                Mode.DND_ONLY -> {
                    enforcement = "none"
                    Log.d(tag, "DND_ONLY mode: would hold ${f.key}")
                }
            }
        }

        // 9. Increment nSeen
        st.nSeen++

        // 10. Async persist event and decision
        val future = CompletableFuture<Long>()
        idByKey[f.key] = future

        val source = if (f.pkg == ownPkg) "sim" else "real"
        ioScope.launch {
            val eventRow = EventRow(
                key = f.key,
                pkg = f.pkg,
                channelId = f.channelId,
                channelName = f.channelName,
                appGroup = group,
                category = f.category,
                importance = f.importance,
                isConversation = f.isConversation,
                ongoing = f.ongoing,
                postTs = now,
                source = source
            )
            val eventId = db.dao().insertEvent(eventRow)

            val decisionRow = DecisionRow(
                eventId = eventId,
                action = if (finalHold) "HOLD" else "SHOW",
                gate = d.gate,
                reason = d.reason,
                mode = s.mode.name,
                phoneState = d.phoneState.name,
                pImpMean = d.pImpMean,
                pImpUp = d.pImpUp,
                pShow = d.pShow,
                pCtrl = d.pCtrl,
                costShow = d.costShow,
                costHold = d.costHold,
                benefit = d.benefit,
                randomized = d.randomized,
                propensityHold = d.propensityHold,
                policyVersion = "1.0",
                enforcement = enforcement,
                releaseAt = releaseTime,
                releasedTs = null,
                removalReason = null,
                removedTs = null
            )
            db.dao().insertDecision(decisionRow)
            db.dao().updateChannelStat(st)

            future.complete(eventId)

            mainHandler.postDelayed({
                ioScope.launch {
                    outcomes.close(eventId)
                }
            }, 10 * 60_000L)
        }
    }

    fun onRemoved(key: String, reason: Int, now: Long) {
        val future = idByKey[key]
        if (future != null) {
            future.thenAcceptAsync({ eventId ->
                ioScope.launch {
                    db.dao().setRemoval(eventId, reason, now)
                }
            }, { ioScope.launch { it.run() } })
        }
    }

    data class WhatIfResult(
        val decision: Decision,
        val channelStat: ChannelStat,
        val costShow: Double,
        val costHold: Double,
        val harm: Double,
        val harmSd: Double,
        val pEngageShow: Double,
        val pullMinutesShow: Double,
        val resumePenaltyShow: Double,
        val pPickupHold: Double,
        val pullMinutesHold: Double,
        val tau: Double
    )

    fun whatIf(ctx: Ctx, f: NotifFeatures, group: String): WhatIfResult {
        val cacheKey = "${f.pkg}|${f.channelId ?: ""}"
        val st = statCache[cacheKey] ?: db.dao().let {
            // Synchronous fallback from memory or prior
            val beta = Policy.priorBeta(f, group)
            val poolPrior = POOL_PRIOR[group] ?: (3.0 to 3.0)
            ChannelStat(
                pkg = f.pkg,
                channelId = f.channelId ?: "",
                group = group,
                a = beta.a,
                b = beta.b,
                nSeen = 10,
                nEngaged = 3,
                mu = poolPrior.first,
                varPull = poolPrior.second * poolPrior.second,
                nPull = 3,
                bShow = 0.0,
                pSwitch = 0.3,
                prot = false,
                never = false
            )
        }

        val s = prefs.current.copy(experiments = false)
        val hourBlock = (ctx.hour / 4).coerceIn(0, 5)
        val modelCtrlP = models.ctrl.p(featureVector(ctx, false, null, true))
        val personalCtrl = ctrlTracker.pCtrl(hourBlock, modelCtrlP)
        val muS = ctrlTracker.muS()

        val d = Policy.decide(f, group, ctx, st, s, models, personalCtrl, muS, rnd)
        val costBreakdown = Policy.costs(f, group, ctx, st, models, personalCtrl, muS)

        val harm = costBreakdown.costShow - costBreakdown.costHold

        // Variance calculation for 80% range bar: Var = p * sigma^2 + p * (1 - p) * mu^2
        val sigmaShow = Math.sqrt(st.varPull)
        val varShow = costBreakdown.pShow * (sigmaShow * sigmaShow) +
                costBreakdown.pShow * (1.0 - costBreakdown.pShow) * (st.mu * st.mu)
        val sigmaHold = 3.0
        val varHold = costBreakdown.pCtrl * (sigmaHold * sigmaHold) +
                costBreakdown.pCtrl * (1.0 - costBreakdown.pCtrl) * (muS * muS)
        val harmSd = Math.sqrt(varShow + varHold)

        val resumePenalty = if (ctx.fgGroup == "productivity") 3.0 else 1.5

        return WhatIfResult(
            decision = d,
            channelStat = st,
            costShow = costBreakdown.costShow,
            costHold = costBreakdown.costHold,
            harm = harm,
            harmSd = harmSd,
            pEngageShow = costBreakdown.pShow,
            pullMinutesShow = st.mu,
            resumePenaltyShow = resumePenalty,
            pPickupHold = costBreakdown.pCtrl,
            pullMinutesHold = muS,
            tau = s.strictness.tau
        )
    }
}
