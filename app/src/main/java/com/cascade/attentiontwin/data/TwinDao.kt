package com.cascade.attentiontwin.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TwinDao {

    /* ── inserts ──────────────────────────────────────────────────── */

    @Insert
    suspend fun insertEvent(event: EventRow): Long

    @Insert
    suspend fun insertDecision(decision: DecisionRow)

    @Insert
    suspend fun insertOutcome(outcome: OutcomeRow)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChannelStat(stat: ChannelStat)

    @Insert
    suspend fun insertScreenInterval(interval: ScreenInterval): Long

    @Insert
    suspend fun insertUnlock(event: UnlockEvent)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCtrlProbe(probe: CtrlProbe)

    /* ── updates ──────────────────────────────────────────────────── */

    @Update
    suspend fun updateDecision(decision: DecisionRow)

    @Update
    suspend fun updateChannelStat(stat: ChannelStat)

    @Update
    suspend fun updateScreenInterval(interval: ScreenInterval)

    /* ── event queries ────────────────────────────────────────────── */

    @Query("SELECT * FROM events WHERE post_ts >= :ts ORDER BY post_ts ASC")
    suspend fun eventsSince(ts: Long): List<EventRow>

    @Query("SELECT * FROM events WHERE id = :id")
    suspend fun eventById(id: Long): EventRow?

    @Query("SELECT * FROM events WHERE `key` = :key ORDER BY post_ts DESC LIMIT 1")
    suspend fun getEventByKey(key: String): EventRow?

    /* ── receipt queries (events + decisions + outcomes joined) ──── */

    @Query(
        """
        SELECT
            e.id, e.`key`, e.pkg,
            e.channel_id   AS channelId,
            e.channel_name AS channelName,
            e.app_group    AS appGroup,
            e.category, e.importance,
            e.post_ts      AS postTs,
            e.source,
            d.action, d.gate, d.reason, d.mode,
            d.phone_state  AS phoneState,
            d.p_imp_mean   AS pImpMean,
            d.p_imp_up     AS pImpUp,
            d.p_show       AS pShow,
            d.p_ctrl       AS pCtrl,
            d.cost_show    AS costShow,
            d.cost_hold    AS costHold,
            d.benefit,
            d.randomized,
            d.propensity_hold AS propensityHold,
            d.policy_version  AS policyVersion,
            d.enforcement,
            d.release_at   AS releaseAt,
            d.released_ts  AS releasedTs,
            d.removal_reason AS removalReason,
            d.removed_ts   AS removedTs,
            o.window_end   AS windowEnd,
            o.engaged, o.clicked, o.dismissed,
            o.unlocks_10m  AS unlocks10m,
            o.phone_min_10m AS phoneMin10m,
            o.clicked_within_2m AS clickedWithin2m,
            o.digest_feedback   AS digestFeedback,
            o.censored
        FROM events e
        INNER JOIN decisions d ON d.event_id = e.id
        LEFT  JOIN outcomes  o ON o.event_id = e.id
        WHERE (:filter IS NULL OR :filter = 'All'
               OR d.action = :filter
               OR d.gate = :filter
               OR d.enforcement = :filter)
        ORDER BY e.post_ts DESC
        LIMIT :limit OFFSET :offset
        """
    )
    suspend fun receipts(limit: Int, offset: Int, filter: String?): List<ReceiptView>

    @Query(
        """
        SELECT
            e.id, e.`key`, e.pkg,
            e.channel_id   AS channelId,
            e.channel_name AS channelName,
            e.app_group    AS appGroup,
            e.category, e.importance,
            e.post_ts      AS postTs,
            e.source,
            d.action, d.gate, d.reason, d.mode,
            d.phone_state  AS phoneState,
            d.p_imp_mean   AS pImpMean,
            d.p_imp_up     AS pImpUp,
            d.p_show       AS pShow,
            d.p_ctrl       AS pCtrl,
            d.cost_show    AS costShow,
            d.cost_hold    AS costHold,
            d.benefit,
            d.randomized,
            d.propensity_hold AS propensityHold,
            d.policy_version  AS policyVersion,
            d.enforcement,
            d.release_at   AS releaseAt,
            d.released_ts  AS releasedTs,
            d.removal_reason AS removalReason,
            d.removed_ts   AS removedTs,
            o.window_end   AS windowEnd,
            o.engaged, o.clicked, o.dismissed,
            o.unlocks_10m  AS unlocks10m,
            o.phone_min_10m AS phoneMin10m,
            o.clicked_within_2m AS clickedWithin2m,
            o.digest_feedback   AS digestFeedback,
            o.censored
        FROM events e
        INNER JOIN decisions d ON d.event_id = e.id
        LEFT  JOIN outcomes  o ON o.event_id = e.id
        WHERE (:filter IS NULL OR :filter = 'All'
               OR d.action = :filter
               OR d.gate = :filter
               OR d.enforcement = :filter)
        ORDER BY e.post_ts DESC
        """
    )
    fun receiptsFlow(filter: String?): Flow<List<ReceiptView>>

    /* ── held-now queries ─────────────────────────────────────────── */

    @Query(
        """
        SELECT * FROM decisions
        WHERE action = 'HOLD'
          AND released_ts IS NULL
          AND release_at > :now - 60000
        """
    )
    suspend fun heldNow(now: Long): List<DecisionRow>

    @Query(
        """
        SELECT * FROM decisions
        WHERE action = 'HOLD'
          AND released_ts IS NULL
          AND release_at > :now - 60000
        """
    )
    fun heldNowDecisionFlow(now: Long): Flow<List<DecisionRow>>

    @Query(
        """
        SELECT
            e.id, e.`key`, e.pkg,
            e.channel_id   AS channelId,
            e.channel_name AS channelName,
            e.app_group    AS appGroup,
            e.category, e.importance,
            e.post_ts      AS postTs,
            e.source,
            d.action, d.gate, d.reason, d.mode,
            d.phone_state  AS phoneState,
            d.p_imp_mean   AS pImpMean,
            d.p_imp_up     AS pImpUp,
            d.p_show       AS pShow,
            d.p_ctrl       AS pCtrl,
            d.cost_show    AS costShow,
            d.cost_hold    AS costHold,
            d.benefit,
            d.randomized,
            d.propensity_hold AS propensityHold,
            d.policy_version  AS policyVersion,
            d.enforcement,
            d.release_at   AS releaseAt,
            d.released_ts  AS releasedTs,
            d.removal_reason AS removalReason,
            d.removed_ts   AS removedTs,
            o.window_end   AS windowEnd,
            o.engaged, o.clicked, o.dismissed,
            o.unlocks_10m  AS unlocks10m,
            o.phone_min_10m AS phoneMin10m,
            o.clicked_within_2m AS clickedWithin2m,
            o.digest_feedback   AS digestFeedback,
            o.censored
        FROM events e
        INNER JOIN decisions d ON d.event_id = e.id
        LEFT  JOIN outcomes  o ON o.event_id = e.id
        WHERE d.action = 'HOLD'
          AND d.released_ts IS NULL
          AND d.release_at > :now - 60000
        ORDER BY e.post_ts DESC
        """
    )
    fun heldNowFlow(now: Long): Flow<List<ReceiptView>>

    @Query(
        """
        SELECT * FROM decisions
        WHERE action = 'HOLD'
          AND released_ts IS NULL
        """
    )
    suspend fun unreleasedHolds(): List<DecisionRow>

    /* ── decision mutations ───────────────────────────────────────── */

    @Query(
        """
        UPDATE decisions
        SET released_ts = :ts
        WHERE event_id IN (SELECT id FROM events WHERE `key` = :key)
          AND released_ts IS NULL
        """
    )
    suspend fun markReleased(key: String, ts: Long)

    @Query(
        """
        UPDATE decisions
        SET removal_reason = :reason, removed_ts = :ts
        WHERE event_id = :eventId
        """
    )
    suspend fun setRemoval(eventId: Long, reason: Int, ts: Long)

    @Query("SELECT * FROM decisions WHERE event_id = :eventId")
    suspend fun decisionByEventId(eventId: Long): DecisionRow?

    /* ── unlock / screen queries ──────────────────────────────────── */

    @Query("SELECT COUNT(*) FROM unlock_events WHERE ts BETWEEN :t0 AND :t1")
    suspend fun unlocksBetween(t0: Long, t1: Long): Int

    @Query(
        """
        SELECT * FROM screen_intervals
        WHERE start_ts < :t1 AND (end_ts IS NULL OR end_ts > :t0)
        """
    )
    suspend fun intervalsOverlapping(t0: Long, t1: Long): List<ScreenInterval>

    @Query("SELECT * FROM screen_intervals ORDER BY start_ts DESC LIMIT 1")
    suspend fun latestScreenInterval(): ScreenInterval?

    @Query("SELECT * FROM screen_intervals WHERE start_ts >= :since ORDER BY start_ts ASC")
    suspend fun recentScreenIntervals(since: Long): List<ScreenInterval>

    @Query("SELECT * FROM unlock_events WHERE ts >= :since ORDER BY ts ASC")
    suspend fun recentUnlocks(since: Long): List<UnlockEvent>

    /* ── outcome window helpers ───────────────────────────────────── */

    @Query(
        """
        SELECT e.id FROM events e
        INNER JOIN decisions d ON d.event_id = e.id
        LEFT  JOIN outcomes  o ON o.event_id = e.id
        WHERE o.event_id IS NULL
          AND e.post_ts + 600000 < :now
        """
    )
    suspend fun pendingWindows(now: Long): List<Long>

    /* ── control-probe queries ────────────────────────────────────── */

    @Query(
        """
        SELECT block,
               COUNT(*)                              AS n,
               SUM(CASE WHEN y THEN 1 ELSE 0 END)   AS k
        FROM ctrl_probes
        GROUP BY block
        """
    )
    suspend fun ctrlCounts(): List<CtrlCount>

    @Query(
        """
        SELECT COALESCE(AVG(phone_min), 0.0) AS avgMin,
               COUNT(*)                       AS n
        FROM ctrl_probes
        WHERE y = 1
        """
    )
    suspend fun muSamples(): MuSample

    @Query("SELECT * FROM ctrl_probes ORDER BY ts ASC")
    suspend fun allCtrlProbes(): List<CtrlProbe>

    /* ── channel-stat queries ─────────────────────────────────────── */

    @Query("SELECT * FROM channel_stats WHERE pkg = :pkg AND channelId = :channelId LIMIT 1")
    suspend fun getChannelStat(pkg: String, channelId: String): ChannelStat?

    @Query("SELECT * FROM channel_stats ORDER BY pkg, channelId")
    suspend fun allChannelStats(): List<ChannelStat>

    @Query("SELECT * FROM channel_stats ORDER BY pkg, channelId")
    fun allChannelStatsFlow(): Flow<List<ChannelStat>>

    /* ── today counts ─────────────────────────────────────────────── */

    @Query(
        """
        SELECT
            COUNT(*)                                                                   AS total,
            SUM(CASE WHEN d.action = 'SHOW' AND d.gate = 'PROTECTED' THEN 1 ELSE 0 END) AS protected_,
            SUM(CASE WHEN d.action = 'SHOW' AND d.gate != 'PROTECTED' THEN 1 ELSE 0 END) AS shown,
            SUM(CASE WHEN d.action = 'HOLD' THEN 1 ELSE 0 END)                         AS held
        FROM events e
        INNER JOIN decisions d ON d.event_id = e.id
        WHERE e.post_ts >= :startOfDay
        """
    )
    suspend fun todayCounts(startOfDay: Long): TodayCounts

    /* ── insights queries ─────────────────────────────────────────── */

    @Query(
        """
        SELECT CAST((post_ts / 3600000) % 24 AS INTEGER) AS hour,
               COUNT(*) AS count
        FROM events
        WHERE post_ts >= :since
        GROUP BY hour
        ORDER BY hour
        """
    )
    suspend fun hourlyNotifCounts(since: Long): List<HourCount>

    @Query(
        """
        SELECT e.pkg,
               e.channel_id AS channelId,
               AVG(o.phone_min_10m) AS avgPhoneMin,
               COUNT(*) AS count
        FROM events e
        INNER JOIN decisions d ON d.event_id = e.id
        INNER JOIN outcomes  o ON o.event_id = e.id
        WHERE d.action = 'SHOW' AND o.engaged = 1
        GROUP BY e.pkg, e.channel_id
        ORDER BY avgPhoneMin DESC
        LIMIT :limit
        """
    )
    suspend fun topChannelsByPhoneMin(limit: Int): List<ChannelPhoneMin>

    /* ── pilot / evidence stats ───────────────────────────────────── */

    @Query(
        """
        SELECT
            COUNT(*)                                                                        AS seen,
            SUM(CASE WHEN d.action = 'HOLD' THEN 1 ELSE 0 END)                              AS held,
            SUM(CASE WHEN d.action = 'SHOW' AND d.gate != 'PROTECTED' THEN 1 ELSE 0 END)    AS shown,
            SUM(CASE WHEN d.action = 'SHOW' AND d.gate  = 'PROTECTED' THEN 1 ELSE 0 END)    AS protected_,
            SUM(CASE WHEN d.gate = 'OVERRIDE' THEN 1 ELSE 0 END)                            AS overrides,
            SUM(CASE WHEN d.enforcement = 'applied' THEN 1 ELSE 0 END)                      AS enforcementSuccess,
            SUM(CASE WHEN d.enforcement != 'none' THEN 1 ELSE 0 END)                        AS enforcementTotal,
            SUM(CASE WHEN d.action = 'SHOW' AND d.gate = 'PROTECTED'
                      AND d.enforcement = 'failed' THEN 1 ELSE 0 END)                       AS missedProtected,
            0.0 AS medianHoldMin,
            0.0 AS medianPhoneMinShow,
            0.0 AS medianPhoneMinHold
        FROM events e
        INNER JOIN decisions d ON d.event_id = e.id
        WHERE e.source = :source
        """
    )
    suspend fun pilotStats(source: String): PilotStats

    /* ── bulk deletes ─────────────────────────────────────────────── */

    @Query("DELETE FROM events")
    suspend fun deleteAllEvents()

    @Query("DELETE FROM decisions")
    suspend fun deleteAllDecisions()

    @Query("DELETE FROM outcomes")
    suspend fun deleteAllOutcomes()

    @Query("DELETE FROM channel_stats")
    suspend fun deleteAllChannelStats()

    @Query("DELETE FROM screen_intervals")
    suspend fun deleteAllScreenIntervals()

    @Query("DELETE FROM unlock_events")
    suspend fun deleteAllUnlocks()

    @Query("DELETE FROM ctrl_probes")
    suspend fun deleteAllCtrlProbes()
}
