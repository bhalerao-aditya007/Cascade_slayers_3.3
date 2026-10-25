package com.cascade.attentiontwin

import com.cascade.attentiontwin.data.ChannelStat
import com.cascade.attentiontwin.data.Mode
import com.cascade.attentiontwin.data.Settings
import com.cascade.attentiontwin.data.Strictness
import com.cascade.attentiontwin.engine.Ctx
import com.cascade.attentiontwin.engine.Logit
import com.cascade.attentiontwin.engine.ModelStore
import com.cascade.attentiontwin.engine.NotifFeatures
import com.cascade.attentiontwin.engine.PhoneState
import com.cascade.attentiontwin.engine.Policy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.util.Calendar
import java.util.Random

class PolicyTest {

    private fun testModels(): ModelStore {
        val showCoef = doubleArrayOf(
            0.0, 0.0, 0.0, 0.15, 0.45, -0.25, 0.6, 0.3,
            0.8, 0.2, 0.0, -0.2, 0.3, 0.1, -0.2, -0.3
        )
        val ctrlCoef = doubleArrayOf(
            0.0, 0.0, 0.0, 0.05, 0.5, -0.3, 0.5, 0.0,
            0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0
        )
        val mean = doubleArrayOf(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0)
        val std = doubleArrayOf(1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0)
        val show = Logit(showCoef, -0.9, mean, std)
        val ctrl = Logit(ctrlCoef, -1.2, mean, std)
        return ModelStore.createForTest(show, ctrl, true)
    }

    private fun testCtx(
        state: PhoneState = PhoneState.LOCKED,
        hour: Int = 15,
        usageKnown: Boolean = true,
        waitingForCode: Boolean = false,
        fgPkg: String? = null,
        fgGroup: String? = null
    ) = Ctx(
        nowMs = System.currentTimeMillis(),
        hour = hour,
        weekend = false,
        notifs30 = 2,
        unlocks30 = 1,
        minsSinceUnlock = 20.0,
        screenOn = false,
        state = state,
        fgPkg = fgPkg,
        fgGroup = fgGroup,
        usageKnown = usageKnown,
        waitingForCode = waitingForCode
    )

    private fun testSettings(
        strictness: Strictness = Strictness.BALANCED,
        mode: Mode = Mode.SNOOZE
    ) = Settings(strictness = strictness, mode = mode)

    private fun testStat(
        a: Double = 3.0,
        b: Double = 3.0,
        nSeen: Int = 10,
        mu: Double = 3.0,
        pSwitch: Double = 0.3,
        prot: Boolean = false,
        never: Boolean = false
    ) = ChannelStat(
        pkg = "com.test.app",
        channelId = "test_channel",
        group = "other",
        a = a,
        b = b,
        nSeen = nSeen,
        nEngaged = 0,
        mu = mu,
        varPull = 4.0,
        nPull = 5,
        bShow = 0.0,
        pSwitch = pSwitch,
        prot = prot,
        never = never
    )

    private fun testNotif(
        channelId: String? = "general",
        channelName: String? = "General",
        category: String? = null,
        importance: Int = 3,
        isConversation: Boolean = false,
        ongoing: Boolean = false,
        isForegroundService: Boolean = false,
        hasReplyAction: Boolean = false,
        bypassDnd: Boolean = false,
        isDefaultSms: Boolean = false,
        isAuthenticator: Boolean = false
    ) = NotifFeatures(
        key = "test_key",
        pkg = "com.test.app",
        postTime = System.currentTimeMillis(),
        channelId = channelId,
        channelName = channelName,
        category = category,
        importance = importance,
        bypassDnd = bypassDnd,
        isConversation = isConversation,
        ongoing = ongoing,
        isGroupSummary = false,
        isForegroundService = isForegroundService,
        hasReplyAction = hasReplyAction,
        isDefaultSms = isDefaultSms,
        isAuthenticator = isAuthenticator
    )

    @Test
    fun securityChannelIsProtected() {
        val f = testNotif(channelName = "Security alerts")
        val d = Policy.decide(f, "other", testCtx(), testStat(), testSettings(), testModels(), null, 3.0, Random(42))
        assertFalse("Security channel should not be held", d.hold)
        assertEquals("PROTECTED", d.gate)
    }

    @Test
    fun promoHeldWhenLockedStrict() {
        val f = testNotif(channelId = "promotions", channelName = "Promotions", category = "promo", importance = 2)
        val st = testStat(a = 1.0, b = 9.0, nSeen = 10, mu = 4.0)
        val s = testSettings(strictness = Strictness.STRICT)
        val d = Policy.decide(f, "shopping", testCtx(), st, s, testModels(), null, 3.0, Random(42))
        assertTrue("Promo should be held in strict mode when locked", d.hold)
        assertTrue("Benefit should be > 0.10", d.benefit > 0.10)
    }

    @Test
    fun coldStartShows() {
        val f = testNotif()
        val st = testStat(nSeen = 0)
        val d = Policy.decide(f, "other", testCtx(), st, testSettings(), testModels(), null, 3.0, Random(42))
        assertFalse("Cold start should show", d.hold)
        assertEquals("COLD_START", d.gate)
    }

    @Test
    fun uncertainChannelNeverHeld() {
        val f = testNotif()
        val st = testStat(a = 3.0, b = 3.0, nSeen = 10)
        val d = Policy.decide(f, "other", testCtx(), st, testSettings(), testModels(), null, 3.0, Random(42))
        assertFalse("Uncertain channel (Beta(3,3)) should not be held", d.hold)
    }

    @Test
    fun inSourceAppProtected() {
        val f = testNotif()
        val ctx = testCtx(state = PhoneState.IN_SOURCE_APP, fgPkg = "com.test.app")
        val d = Policy.decide(f, "other", ctx, testStat(), testSettings(), testModels(), null, 3.0, Random(42))
        assertFalse("In source app should be protected", d.hold)
        assertEquals("PROTECTED", d.gate)
    }

    @Test
    fun digestReleaseAlignsToSlot() {
        val cal = Calendar.getInstance().apply {
            set(2026, Calendar.JANUARY, 1, 10, 7, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val now = cal.timeInMillis
        val (releaseAt, duration) = Policy.digestRelease(now, 15, 15)
        val releaseCal = Calendar.getInstance().apply { timeInMillis = releaseAt }
        assertEquals("Should release at 10:15", 15, releaseCal.get(Calendar.MINUTE))
        assertEquals("Duration should be 8 minutes", 8 * 60_000L, duration)
    }

    @Test
    fun noInternetNoContent() {
        val banned = listOf(
            "EXTRA_TITLE", "EXTRA_TEXT", "EXTRA_BIG_TEXT", "EXTRA_MESSAGES", "EXTRA_SUB_TEXT",
            "EXTRA_INFO_TEXT", "EXTRA_SUMMARY_TEXT", "EXTRA_TEXT_LINES", "tickerText"
        )
        val srcDir = File("src/main")
        if (srcDir.exists()) {
            val hits = srcDir.walkTopDown()
                .filter { it.extension == "kt" || it.extension == "java" }
                .flatMap { f ->
                    f.readLines().mapIndexedNotNull { i, l ->
                        if (banned.any { b -> l.contains(b) }) "${f.name}:${i + 1}: $l" else null
                    }
                }
                .toList()
            assertTrue("Source should not contain banned content constants:\n${hits.joinToString("\n")}", hits.isEmpty())
        }
    }
}
