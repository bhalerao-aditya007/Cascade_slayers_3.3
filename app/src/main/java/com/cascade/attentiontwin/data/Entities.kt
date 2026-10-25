package com.cascade.attentiontwin.data

import androidx.room.*

/* ── core notification event ──────────────────────────────────────── */

@Entity(tableName = "events", indices = [Index("post_ts"), Index("key")])
data class EventRow(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val key: String,
    val pkg: String,
    @ColumnInfo(name = "channel_id") val channelId: String?,
    @ColumnInfo(name = "channel_name") val channelName: String?,
    @ColumnInfo(name = "app_group") val appGroup: String,
    val category: String?,
    val importance: Int,
    @ColumnInfo(name = "is_conversation") val isConversation: Boolean,
    val ongoing: Boolean,
    @ColumnInfo(name = "post_ts") val postTs: Long,
    val source: String = "real"
)

/* ── decision taken by the gate logic ─────────────────────────────── */

@Entity(tableName = "decisions")
data class DecisionRow(
    @PrimaryKey @ColumnInfo(name = "event_id") val eventId: Long,
    val action: String,                // SHOW | HOLD
    val gate: String,
    val reason: String,
    val mode: String,                  // SHADOW | SNOOZE | DND_ONLY
    @ColumnInfo(name = "phone_state") val phoneState: String,
    @ColumnInfo(name = "p_imp_mean") val pImpMean: Double,
    @ColumnInfo(name = "p_imp_up") val pImpUp: Double,
    @ColumnInfo(name = "p_show") val pShow: Double,
    @ColumnInfo(name = "p_ctrl") val pCtrl: Double,
    @ColumnInfo(name = "cost_show") val costShow: Double,
    @ColumnInfo(name = "cost_hold") val costHold: Double,
    val benefit: Double,
    val randomized: Boolean,
    @ColumnInfo(name = "propensity_hold") val propensityHold: Double,
    @ColumnInfo(name = "policy_version") val policyVersion: String,
    val enforcement: String,           // applied | failed | none
    @ColumnInfo(name = "release_at") val releaseAt: Long?,
    @ColumnInfo(name = "released_ts") val releasedTs: Long?,
    @ColumnInfo(name = "removal_reason") val removalReason: Int?,
    @ColumnInfo(name = "removed_ts") val removedTs: Long?
)

/* ── outcome measured after the observation window ────────────────── */

@Entity(tableName = "outcomes")
data class OutcomeRow(
    @PrimaryKey @ColumnInfo(name = "event_id") val eventId: Long,
    @ColumnInfo(name = "window_end") val windowEnd: Long,
    val engaged: Boolean,
    val clicked: Boolean,
    val dismissed: Boolean,
    @ColumnInfo(name = "unlocks_10m") val unlocks10m: Int,
    @ColumnInfo(name = "phone_min_10m") val phoneMin10m: Double,
    @ColumnInfo(name = "clicked_within_2m") val clickedWithin2m: Boolean,
    @ColumnInfo(name = "digest_feedback") val digestFeedback: String? = null,
    val censored: Boolean = false
)

/* ── per-channel Beta + bandit stats ──────────────────────────────── */

@Entity(tableName = "channel_stats", primaryKeys = ["pkg", "channelId"])
data class ChannelStat(
    val pkg: String,
    val channelId: String,
    @ColumnInfo(name = "app_group") val group: String,
    var a: Double,
    var b: Double,
    @ColumnInfo(name = "n_seen") var nSeen: Int = 0,
    @ColumnInfo(name = "n_engaged") var nEngaged: Int = 0,
    var mu: Double,
    @ColumnInfo(name = "var_pull") var varPull: Double,
    @ColumnInfo(name = "n_pull") var nPull: Int = 0,
    @ColumnInfo(name = "b_show") var bShow: Double = 0.0,
    @ColumnInfo(name = "p_switch") var pSwitch: Double,
    var prot: Boolean = false,
    var never: Boolean = false,
    @ColumnInfo(name = "updated_ts") var updatedTs: Long = 0
)

/* ── screen-on intervals ──────────────────────────────────────────── */

@Entity(tableName = "screen_intervals")
data class ScreenInterval(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "start_ts") val startTs: Long,
    @ColumnInfo(name = "end_ts") var endTs: Long?
)

/* ── unlock timestamps ────────────────────────────────────────────── */

@Entity(tableName = "unlock_events", indices = [Index("ts")])
data class UnlockEvent(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val ts: Long
)

/* ── control-group probes ─────────────────────────────────────────── */

@Entity(tableName = "ctrl_probes")
data class CtrlProbe(
    @PrimaryKey val ts: Long,
    val block: Int,
    val y: Boolean,
    @ColumnInfo(name = "phone_min") val phoneMin: Double
)

/* ══════════════════════════════════════════════════════════════════
   View / result data classes (not Room entities)
   ══════════════════════════════════════════════════════════════════ */

/** Joined view across events + decisions + outcomes. */
data class ReceiptView(
    val id: Long,
    val key: String,
    val pkg: String,
    val channelId: String?,
    val channelName: String?,
    val appGroup: String,
    val category: String?,
    val importance: Int,
    val postTs: Long,
    val source: String,
    val action: String,
    val gate: String,
    val reason: String,
    val mode: String,
    val phoneState: String,
    val pImpMean: Double,
    val pImpUp: Double,
    val pShow: Double,
    val pCtrl: Double,
    val costShow: Double,
    val costHold: Double,
    val benefit: Double,
    val randomized: Boolean,
    val propensityHold: Double,
    val policyVersion: String,
    val enforcement: String,
    val releaseAt: Long?,
    val releasedTs: Long?,
    val removalReason: Int?,
    val removedTs: Long?,
    // outcome fields – nullable because outcome may not exist yet
    val windowEnd: Long?,
    val engaged: Boolean?,
    val clicked: Boolean?,
    val dismissed: Boolean?,
    val unlocks10m: Int?,
    val phoneMin10m: Double?,
    val clickedWithin2m: Boolean?,
    val digestFeedback: String?,
    val censored: Boolean?
)

/** Aggregated control-probe counts per block. */
data class CtrlCount(
    val block: Int,
    val n: Int,
    val k: Int
)

/** Average phone-minutes and count for engaged control probes. */
data class MuSample(
    val avgMin: Double,
    val n: Int
)

/** Daily summary counters. */
data class TodayCounts(
    val total: Int,
    val protected_: Int,
    val shown: Int,
    val held: Int
)

/** Per-hour notification count for insights chart. */
data class HourCount(
    val hour: Int,
    val count: Int
)

/** Average phoneMin10m per channel for shown-engaged rows. */
data class ChannelPhoneMin(
    val pkg: String,
    val channelId: String?,
    val avgPhoneMin: Double,
    val count: Int
)

/** Aggregate pilot/evidence statistics. */
data class PilotStats(
    val seen: Int,
    val held: Int,
    val shown: Int,
    val protected_: Int,
    val overrides: Int,
    val enforcementSuccess: Int,
    val enforcementTotal: Int,
    val missedProtected: Int,
    val medianHoldMin: Double?,
    val medianPhoneMinShow: Double?,
    val medianPhoneMinHold: Double?
)
