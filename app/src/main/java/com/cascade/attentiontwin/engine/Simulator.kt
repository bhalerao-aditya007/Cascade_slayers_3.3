package com.cascade.attentiontwin.engine

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.RemoteInput
import com.cascade.attentiontwin.Graph
import com.cascade.attentiontwin.MainActivity
import com.cascade.attentiontwin.R
import com.cascade.attentiontwin.data.ChannelStat
import com.cascade.attentiontwin.data.DecisionRow
import com.cascade.attentiontwin.data.EventRow
import com.cascade.attentiontwin.data.OutcomeRow
import kotlinx.coroutines.launch
import java.util.Random

object Simulator {

    const val CH_MESSAGES = "messages"
    const val CH_PROMOTIONS = "promotions"
    const val CH_LIKES = "likes"
    const val CH_SECURITY = "security"
    const val CH_NEWS = "news"
    const val CH_ORDERS = "orders"

    private val mainHandler = Handler(Looper.getMainLooper())

    fun setupChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channels = listOf(
                NotificationChannel(CH_MESSAGES, "Messages", NotificationManager.IMPORTANCE_DEFAULT),
                NotificationChannel(CH_PROMOTIONS, "Promotions", NotificationManager.IMPORTANCE_DEFAULT),
                NotificationChannel(CH_LIKES, "Likes and comments", NotificationManager.IMPORTANCE_DEFAULT),
                NotificationChannel(CH_SECURITY, "Security alerts", NotificationManager.IMPORTANCE_HIGH),
                NotificationChannel(CH_NEWS, "News", NotificationManager.IMPORTANCE_DEFAULT),
                NotificationChannel(CH_ORDERS, "Order updates", NotificationManager.IMPORTANCE_DEFAULT)
            )
            nm.createNotificationChannels(channels)
        }

        // Pre-seed channel stats so simulator channels do not sit in cold start
        Graph.ioScope.launch {
            val ownPkg = context.packageName
            val pairs = listOf(
                CH_MESSAGES to "messaging",
                CH_PROMOTIONS to "shopping",
                CH_LIKES to "social",
                CH_SECURITY to "other",
                CH_NEWS to "news",
                CH_ORDERS to "shopping"
            )
            for ((chId, group) in pairs) {
                val existing = Graph.db.dao().getChannelStat(ownPkg, chId)
                if (existing == null) {
                    val notif = NotifFeatures(
                        key = "seed", pkg = ownPkg, postTime = 0,
                        channelId = chId, channelName = chId,
                        category = null, importance = 3, bypassDnd = false,
                        isConversation = (chId == CH_MESSAGES), ongoing = false,
                        isGroupSummary = false, isForegroundService = false,
                        hasReplyAction = (chId == CH_MESSAGES)
                    )
                    val beta = Policy.priorBeta(notif, group)
                    val pool = POOL_PRIOR[group] ?: (3.0 to 3.0)
                    val stat = ChannelStat(
                        pkg = ownPkg,
                        channelId = chId,
                        group = group,
                        a = beta.a,
                        b = beta.b,
                        nSeen = 10,
                        nEngaged = 3,
                        mu = pool.first,
                        varPull = pool.second * pool.second,
                        nPull = 3,
                        bShow = 0.0,
                        pSwitch = if (group == "messaging") 0.5 else 0.2,
                        prot = (chId == CH_SECURITY),
                        never = false,
                        updatedTs = System.currentTimeMillis()
                    )
                    Graph.db.dao().insertChannelStat(stat)
                    Graph.statCache["$ownPkg|$chId"] = stat
                }
            }
        }
    }

    fun post(context: Context, kind: String, delaySeconds: Int = 0) {
        if (delaySeconds > 0) {
            mainHandler.postDelayed({
                doPost(context, kind)
            }, delaySeconds * 1000L)
        } else {
            doPost(context, kind)
        }
    }

    fun postBurstPromotions(context: Context) {
        repeat(8) { idx ->
            mainHandler.postDelayed({
                doPost(context, CH_PROMOTIONS, notifId = 200 + idx)
            }, idx * 150L)
        }
    }

    private fun doPost(context: Context, kind: String, notifId: Int = (100..999).random()) {
        try {
            val builder = NotificationCompat.Builder(context, kind)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("Attention Twin Demo")
                .setContentText("Simulated $kind notification")
                .setAutoCancel(true)

            val openIntent = Intent(context, MainActivity::class.java)
            val pi = PendingIntent.getActivity(
                context, notifId, openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.setContentIntent(pi)

            when (kind) {
                CH_MESSAGES -> {
                    builder.setCategory(NotificationCompat.CATEGORY_MESSAGE)
                    val remoteInput = RemoteInput.Builder("quick_reply")
                        .setLabel("Reply")
                        .build()
                    val action = NotificationCompat.Action.Builder(
                        R.drawable.ic_launcher_foreground,
                        "Reply",
                        pi
                    ).addRemoteInput(remoteInput).build()
                    builder.addAction(action)
                }
                CH_PROMOTIONS -> {
                    builder.setCategory(NotificationCompat.CATEGORY_PROMO)
                }
                CH_LIKES -> {
                    builder.setCategory(NotificationCompat.CATEGORY_SOCIAL)
                }
                CH_SECURITY -> {
                    builder.setCategory(NotificationCompat.CATEGORY_STATUS)
                    builder.setPriority(NotificationCompat.PRIORITY_HIGH)
                }
                CH_NEWS -> {
                    builder.setCategory(NotificationCompat.CATEGORY_RECOMMENDATION)
                }
                CH_ORDERS -> {
                    builder.setCategory(NotificationCompat.CATEGORY_STATUS)
                }
            }

            val nmc = NotificationManagerCompat.from(context)
            nmc.notify(notifId, builder.build())
        } catch (_: SecurityException) {
            // Notification permission might not be granted yet
        }
    }

    fun seedDemoData(context: Context) {
        Graph.ioScope.launch {
            val now = System.currentTimeMillis()
            val ownPkg = context.packageName
            val rnd = Random(42)

            val channels = listOf(
                CH_PROMOTIONS to "shopping",
                CH_LIKES to "social",
                CH_SECURITY to "other",
                CH_MESSAGES to "messaging",
                CH_NEWS to "news",
                CH_ORDERS to "shopping"
            )

            // Seed 40 diverse past notifications across the last 24 hours
            for (i in 0 until 40) {
                val pastTs = now - (i * 35 * 60_000L)
                val (chId, group) = channels[i % channels.size]

                val isProt = chId == CH_SECURITY || chId == CH_MESSAGES
                val isHeld = !isProt && (chId == CH_PROMOTIONS || chId == CH_LIKES) && (i % 3 != 0)

                val ev = EventRow(
                    key = "sim_seed_$i",
                    pkg = ownPkg,
                    channelId = chId,
                    channelName = chId.replaceFirstChar { it.uppercase() },
                    appGroup = group,
                    category = if (isHeld) "promo" else "status",
                    importance = if (isProt) 4 else 2,
                    isConversation = (chId == CH_MESSAGES),
                    ongoing = false,
                    postTs = pastTs,
                    source = "sim"
                )
                val evId = Graph.db.dao().insertEvent(ev)

                val dec = DecisionRow(
                    eventId = evId,
                    action = if (isHeld) "HOLD" else "SHOW",
                    gate = if (isProt) "PROTECTED" else if (isHeld) "HOLD" else "COST",
                    reason = if (isProt) "Protected: security channel."
                    else if (isHeld) "Low importance (≤ 0.18 optimistic); showing now ≈ +2.1 min phone time vs waiting."
                    else "Low expected cost (0.3 min); not worth holding.",
                    mode = "SNOOZE",
                    phoneState = if (i % 2 == 0) "LOCKED" else "IN_OTHER_APP",
                    pImpMean = if (isHeld) 0.1 else 0.5,
                    pImpUp = if (isHeld) 0.18 else 0.65,
                    pShow = 0.25,
                    pCtrl = 0.12,
                    costShow = 2.4,
                    costHold = 0.6,
                    benefit = 1.8,
                    randomized = (i % 5 == 0),
                    propensityHold = 0.85,
                    policyVersion = "1.0",
                    enforcement = if (isHeld) "applied" else "none",
                    releaseAt = if (isHeld) pastTs + 15 * 60_000L else null,
                    releasedTs = if (isHeld) pastTs + 15 * 60_000L else null,
                    removalReason = if (isHeld) 18 else 1,
                    removedTs = pastTs + 16 * 60_000L
                )
                Graph.db.dao().insertDecision(dec)

                val outcome = OutcomeRow(
                    eventId = evId,
                    windowEnd = pastTs + 10 * 60_000L,
                    engaged = (i % 2 == 0),
                    clicked = (i % 3 == 0),
                    dismissed = (i % 3 == 1),
                    unlocks10m = if (i % 2 == 0) 2 else 0,
                    phoneMin10m = if (i % 2 == 0) (1.0 + rnd.nextDouble() * 4.0) else 0.0,
                    clickedWithin2m = (i % 4 == 0),
                    digestFeedback = if (i == 1) "sooner" else null,
                    censored = false
                )
                Graph.db.dao().insertOutcome(outcome)
            }
        }
    }
}
