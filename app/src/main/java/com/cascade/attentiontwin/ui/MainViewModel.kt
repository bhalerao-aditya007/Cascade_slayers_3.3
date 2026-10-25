package com.cascade.attentiontwin.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.cascade.attentiontwin.Graph
import com.cascade.attentiontwin.data.ChannelStat
import com.cascade.attentiontwin.data.DecisionRow
import com.cascade.attentiontwin.data.Mode
import com.cascade.attentiontwin.data.ReceiptView
import com.cascade.attentiontwin.data.Settings
import com.cascade.attentiontwin.data.TodayCounts
import com.cascade.attentiontwin.engine.Ctx
import com.cascade.attentiontwin.engine.Effect
import com.cascade.attentiontwin.engine.PhoneState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

data class TwinStatus(
    val listenerConnected: Boolean = false,
    val usageGranted: Boolean = false,
    val policyGranted: Boolean = false,
    val focusActive: Boolean = false,
    val mode: Mode = Mode.SNOOZE,
    val totalSeen: Int = 0
)

data class InsightsData(
    val hourlyCounts: List<Pair<Int, Int>> = emptyList(),
    val topChannels: List<Triple<String, String, Float>> = emptyList(),
    val fragmentationRanking: List<Triple<String, String, Float>> = emptyList(),
    val bestFocusHours: List<Int> = emptyList(),
    val maxScreenMinToday: Double = 0.0,
    val unlocksPerHour: Double = 0.0,
    val isLateHour: Boolean = false,
    val totalObservations: Int = 0
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val context = application.applicationContext
    val prefs = Graph.prefs
    val settingsFlow: StateFlow<Settings> = prefs.flow

    private val _status = MutableStateFlow(TwinStatus())
    val status: StateFlow<TwinStatus> = _status.asStateFlow()

    private val _todayCounts = MutableStateFlow(TodayCounts(0, 0, 0, 0))
    val todayCounts: StateFlow<TodayCounts> = _todayCounts.asStateFlow()

    private val _currentCtx = MutableStateFlow(
        Ctx(
            nowMs = System.currentTimeMillis(),
            hour = 12,
            weekend = false,
            notifs30 = 0,
            unlocks30 = 0,
            minsSinceUnlock = 600.0,
            screenOn = true,
            state = PhoneState.LOCKED,
            fgPkg = null,
            fgGroup = null,
            usageKnown = false,
            waitingForCode = false
        )
    )
    val currentCtx: StateFlow<Ctx> = _currentCtx.asStateFlow()

    private val _insightsData = MutableStateFlow(InsightsData())
    val insightsData: StateFlow<InsightsData> = _insightsData.asStateFlow()

    val receiptsFlow = Graph.db.dao().receiptsFlow(null).stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    val heldListFlow = Graph.db.dao().heldNowFlow(System.currentTimeMillis()).stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    val channelStatsFlow = Graph.db.dao().allChannelStatsFlow().stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    init {
        startPolling()
    }

    private fun startPolling() {
        viewModelScope.launch {
            while (true) {
                refreshStatus()
                refreshTodayCounts()
                refreshCtx()
                refreshInsights()
                delay(2000L)
            }
        }
    }

    fun toggleFocus() {
        prefs.update { copy(focusActive = !focusActive) }
    }

    fun submitFeedback(eventId: Long, kind: String) {
        Graph.outcomes.submitFeedback(eventId, kind)
    }

    fun deleteAllData() {
        viewModelScope.launch {
            Graph.db.clearAllTables()
            Graph.statCache.clear()
            Graph.engine.heldKeys.clear()
            Graph.engine.idByKey.clear()
            prefs.reset()
            refreshStatus()
            refreshTodayCounts()
        }
    }

    private fun refreshStatus() {
        val listenerOk = Graph.listenerConnected.value
        val usageOk = Graph.usageHelper.usageGranted()
        val s = prefs.current

        viewModelScope.launch {
            val stats = Graph.db.dao().allChannelStats()
            val seen = stats.sumOf { it.nSeen }
            _status.value = TwinStatus(
                listenerConnected = listenerOk,
                usageGranted = usageOk,
                policyGranted = false,
                focusActive = s.focusActive,
                mode = s.mode,
                totalSeen = seen
            )
        }
    }

    private suspend fun refreshTodayCounts() {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startOfDay = cal.timeInMillis
        val counts = Graph.db.dao().todayCounts(startOfDay)
        _todayCounts.value = counts
    }

    private fun refreshCtx() {
        val now = System.currentTimeMillis()
        val ctx = Graph.contextTracker.build(now, context.packageName, context.packageName)
        _currentCtx.value = ctx
    }

    private suspend fun refreshInsights() {
        val now = System.currentTimeMillis()
        val since24h = now - 24 * 60 * 60_000L
        val hourly = Graph.db.dao().hourlyNotifCounts(since24h)
        val topCh = Graph.db.dao().topChannelsByPhoneMin(5)
        val stats = Graph.db.dao().allChannelStats()

        // Fragmentation: app group -> sum of (nEngaged * mu)
        val fragMap = mutableMapOf<String, Float>()
        for (st in stats) {
            val pull = (st.nEngaged * st.mu).toFloat()
            val key = st.pkg.substringAfterLast('.').replaceFirstChar { it.uppercase() }
            fragMap[key] = (fragMap[key] ?: 0f) + pull
        }
        val fragList = fragMap.map { Triple(it.key, "engaged × pull", it.value) }
            .sortedByDescending { it.third }
            .take(5)

        val cal = Calendar.getInstance().apply { timeInMillis = now }
        val hour = cal.get(Calendar.HOUR_OF_DAY)
        val isLate = hour >= 22

        // Best focus hours
        val unlocks = Graph.db.dao().recentUnlocks(now - 7 * 24 * 60 * 60_000L)
        val bestHours = if (unlocks.size >= 10) listOf(9, 10, 14) else emptyList()

        _insightsData.value = InsightsData(
            hourlyCounts = hourly.map { it.hour to it.count },
            topChannels = topCh.map { Triple(it.pkg.substringAfterLast('.'), it.channelId ?: "default", it.avgPhoneMin.toFloat()) },
            fragmentationRanking = fragList,
            bestFocusHours = bestHours,
            maxScreenMinToday = 45.0,
            unlocksPerHour = (unlocks.size.toDouble() / 24.0).coerceAtLeast(0.0),
            isLateHour = isLate,
            totalObservations = stats.sumOf { it.nSeen }
        )
    }
}
