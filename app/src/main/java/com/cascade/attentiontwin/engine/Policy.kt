package com.cascade.attentiontwin.engine

import com.cascade.attentiontwin.data.ChannelStat
import com.cascade.attentiontwin.data.Mode
import com.cascade.attentiontwin.data.Settings
import java.util.Locale
import java.util.Random

data class NotifFeatures(
    val key: String,
    val pkg: String,
    val postTime: Long,
    val channelId: String?,
    val channelName: String?,
    val category: String?,
    val importance: Int,
    val bypassDnd: Boolean,
    val isConversation: Boolean,
    val ongoing: Boolean,
    val isGroupSummary: Boolean,
    val isForegroundService: Boolean,
    val hasReplyAction: Boolean,
    val isDefaultSms: Boolean = false,
    val isAuthenticator: Boolean = false
)

data class Beta(var a: Double, var b: Double) {
    val mean get() = a / (a + b)
    val sd get() = Math.sqrt(a * b / ((a + b) * (a + b) * (a + b + 1.0)))
    val upper get() = mean + 1.28 * sd
}

data class Decision(
    val hold: Boolean,
    val gate: String,
    val reason: String,
    val pImpMean: Double,
    val pImpUp: Double,
    val pShow: Double,
    val pCtrl: Double,
    val costShow: Double,
    val costHold: Double,
    val benefit: Double,
    val randomized: Boolean,
    val propensityHold: Double,
    val phoneState: PhoneState
)

val PROTECTED_CATEGORIES = setOf(
    "call", "missed_call", "voicemail", "alarm", "reminder", "event", "navigation", "transport",
    "err", "sys", "status", "service", "progress", "workout", "location_sharing", "stopwatch"
)

val PROTECT_KEYWORDS = listOf(
    "otp", "verification", "verify", "security", "secure", "login", "sign-in", "signin", "2fa",
    "authenticator", "transaction", "payment", "debit", "credit", "bank", "upi", "fraud", "account",
    "balance", "delivery", "ride", "cab", "booking", "ticket", "boarding", "appointment", "emergency",
    "sos", "missed call", "calendar", "alarm"
)

val PROMO_KEYWORDS = listOf(
    "promo", "offer", "deal", "discount", "sale", "coupon", "recommend", "suggest", "marketing",
    "newsletter", "like", "comment", "follow", "reaction", "trending"
)

val POOL_PRIOR = mapOf(
    "messaging" to (1.5 to 1.5),
    "social" to (5.0 to 5.0),
    "video" to (8.0 to 7.0),
    "shopping" to (4.0 to 4.0),
    "finance" to (1.0 to 1.0),
    "email" to (2.0 to 2.0),
    "news" to (4.0 to 4.0),
    "productivity" to (3.0 to 3.0),
    "other" to (3.0 to 3.0)
)

object Policy {

    fun keywordHit(f: NotifFeatures, list: List<String>): Boolean {
        val chId = f.channelId?.lowercase(Locale.US) ?: ""
        val chName = f.channelName?.lowercase(Locale.US) ?: ""
        return list.any { kw -> chId.contains(kw) || chName.contains(kw) }
    }

    fun priorBeta(f: NotifFeatures, group: String): Beta {
        val beta = when {
            keywordHit(f, PROTECT_KEYWORDS) -> Beta(8.0, 2.0)
            looksConversational(f) -> Beta(6.0, 4.0)
            f.category in setOf("promo", "recommendation") || keywordHit(f, PROMO_KEYWORDS) -> Beta(1.0, 9.0)
            group in setOf("social", "video", "news") -> Beta(2.0, 8.0)
            else -> Beta(3.0, 3.0)
        }
        if (f.importance >= 4) {
            beta.a += 2.0
        }
        return beta
    }

    fun looksConversational(f: NotifFeatures): Boolean {
        return f.isConversation || (f.category == "msg" && f.hasReplyAction)
    }

    fun protectedReason(
        f: NotifFeatures,
        group: String,
        st: ChannelStat,
        s: Settings,
        ctx: Ctx
    ): String? {
        if (f.ongoing || f.isForegroundService || f.bypassDnd) {
            return "Protected: ongoing or system-critical notification."
        }
        if (f.category != null && f.category in PROTECTED_CATEGORIES) {
            return "Protected: ${f.category} category."
        }
        if (st.never || st.prot) {
            return "Protected: channel marked as protected."
        }
        if (f.pkg in s.neverHoldPkgs) {
            return "Protected: app is in never-hold list."
        }
        if (f.isDefaultSms || f.isAuthenticator) {
            return "Protected: default SMS or authenticator app."
        }
        if (keywordHit(f, PROTECT_KEYWORDS)) {
            return "Protected: security channel."
        }
        if (looksConversational(f) && !s.allowHoldChats) {
            return "Protected: conversation (chats are protected)."
        }
        if (ctx.state == PhoneState.IN_SOURCE_APP) {
            return "Protected: you are in the sending app."
        }
        if (ctx.waitingForCode && (f.isDefaultSms || group == "email")) {
            return "Protected: waiting for a verification code."
        }
        return null
    }

    fun mCtx(c: Ctx): Double = when (c.fgGroup) {
        "productivity" -> 1.5
        "video" -> 1.2
        "social" -> 0.8
        else -> if (c.fgPkg != null && (c.fgPkg.contains("launcher") || c.fgPkg.contains("home"))) 0.7 else 1.0
    }

    data class CostBreakdown(
        val costShow: Double,
        val costHold: Double,
        val pShow: Double,
        val pCtrl: Double
    )

    fun costs(
        f: NotifFeatures,
        group: String,
        c: Ctx,
        st: ChannelStat,
        models: ModelStore,
        personalCtrl: Double?,
        muS: Double
    ): CostBreakdown {
        return if (c.state == PhoneState.LOCKED) {
            val x = featureVector(c, f.importance >= 4, group, false)
            val pShow = models.show.p(x, st.bShow)
            val pCtrl = personalCtrl ?: models.ctrl.p(featureVector(c, false, null, true))
            val costShow = pShow * st.mu
            val costHold = pCtrl * muS
            CostBreakdown(costShow, costHold, pShow, pCtrl)
        } else {
            val m = mCtx(c)
            val r = if (c.fgGroup == "productivity") 3.0 else 1.5
            val costShow = st.pSwitch * (st.mu + r) * m
            val costHold = 0.15 * muS
            val pShow = st.pSwitch
            val pCtrl = 0.15
            CostBreakdown(costShow, costHold, pShow, pCtrl)
        }
    }

    fun decide(
        f: NotifFeatures,
        group: String,
        c: Ctx,
        st: ChannelStat,
        s: Settings,
        models: ModelStore,
        personalCtrl: Double?,
        muS: Double,
        rnd: Random = Random()
    ): Decision {
        val protReason = protectedReason(f, group, st, s, c)
        if (protReason != null) {
            return Decision(
                hold = false,
                gate = "PROTECTED",
                reason = protReason,
                pImpMean = 0.0,
                pImpUp = 0.0,
                pShow = 0.0,
                pCtrl = 0.0,
                costShow = 0.0,
                costHold = 0.0,
                benefit = 0.0,
                randomized = false,
                propensityHold = 0.0,
                phoneState = c.state
            )
        }

        if (st.nSeen < 3 && !s.demoMode) {
            return Decision(
                hold = false,
                gate = "COLD_START",
                reason = "New channel, not enough history; showing while learning.",
                pImpMean = 0.0,
                pImpUp = 0.0,
                pShow = 0.0,
                pCtrl = 0.0,
                costShow = 0.0,
                costHold = 0.0,
                benefit = 0.0,
                randomized = false,
                propensityHold = 0.0,
                phoneState = c.state
            )
        }

        val imp = Beta(st.a, st.b)
        var tau = s.strictness.tau
        val delta = s.strictness.delta
        if (!c.usageKnown) {
            tau = minOf(tau, 0.25)
        }

        val cost = costs(f, group, c, st, models, personalCtrl, muS)
        val benefit = cost.costShow - cost.costHold
        val policyHold = (imp.upper < tau) && (benefit > delta)

        var hold = policyHold
        var randomized = false
        val propensity: Double
        val eligibleGroups = setOf("social", "shopping", "news", "video", "other")

        if (s.experiments && s.mode == Mode.SNOOZE && imp.upper < tau && group in eligibleGroups) {
            val eps = s.epsilon
            propensity = if (policyHold) 1.0 - eps else eps
            hold = rnd.nextDouble() < propensity
            randomized = true
        } else {
            propensity = if (policyHold) 1.0 else 0.0
        }

        val reason = if (hold) {
            String.format(
                Locale.US,
                "Low importance (≤ %.2f optimistic); showing now ≈ +%.1f min phone time vs waiting.",
                imp.upper,
                benefit
            )
        } else if (benefit <= delta) {
            String.format(
                Locale.US,
                "Low expected cost (%.1f min); not worth holding.",
                benefit
            )
        } else {
            String.format(
                Locale.US,
                "Might matter (importance up to %.2f); showing.",
                imp.upper
            )
        }

        val gate = if (hold) "HOLD" else if (imp.upper >= tau) "IMPORTANCE" else "COST"

        return Decision(
            hold = hold,
            gate = gate,
            reason = reason,
            pImpMean = imp.mean,
            pImpUp = imp.upper,
            pShow = cost.pShow,
            pCtrl = cost.pCtrl,
            costShow = cost.costShow,
            costHold = cost.costHold,
            benefit = benefit,
            randomized = randomized,
            propensityHold = propensity,
            phoneState = c.state
        )
    }

    fun digestRelease(now: Long, maxHoldMin: Int, digestMin: Int): Pair<Long, Long> {
        val digestMs = digestMin * 60_000L
        var next = Math.ceil(now.toDouble() / digestMs.toDouble()).toLong() * digestMs
        if (next <= now) {
            next += digestMs
        }
        val maxRelease = now + maxHoldMin * 60_000L
        val releaseAt = minOf(maxRelease, next)
        val duration = maxOf(releaseAt - now, 60_000L)
        return Pair(releaseAt, duration)
    }
}
