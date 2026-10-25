package com.cascade.attentiontwin

import android.content.Context
import com.cascade.attentiontwin.data.AppDb
import com.cascade.attentiontwin.data.ChannelStat
import com.cascade.attentiontwin.data.Prefs
import com.cascade.attentiontwin.engine.AppGroups
import com.cascade.attentiontwin.engine.ContextTracker
import com.cascade.attentiontwin.engine.CtrlTracker
import com.cascade.attentiontwin.engine.Engine
import com.cascade.attentiontwin.engine.ModelStore
import com.cascade.attentiontwin.engine.OutcomeTracker
import com.cascade.attentiontwin.engine.UsageHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

object Graph {
    val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    val listenerConnected = MutableStateFlow(false)
    val statCache = ConcurrentHashMap<String, ChannelStat>()

    lateinit var db: AppDb
        private set
    lateinit var prefs: Prefs
        private set
    lateinit var models: ModelStore
        private set
    lateinit var groups: AppGroups
        private set
    lateinit var usageHelper: UsageHelper
        private set
    lateinit var contextTracker: ContextTracker
        private set
    lateinit var ctrlTracker: CtrlTracker
        private set
    lateinit var outcomes: OutcomeTracker
        private set
    lateinit var engine: Engine
        private set

    fun init(appContext: Context) {
        db = AppDb.get(appContext)
        prefs = Prefs(appContext)
        models = ModelStore.load(appContext.assets)
        groups = AppGroups(appContext)
        usageHelper = UsageHelper(appContext, groups)
        contextTracker = ContextTracker(appContext, db, prefs, usageHelper, groups, ioScope)
        ctrlTracker = CtrlTracker(db, contextTracker, ioScope)
        outcomes = OutcomeTracker(db, prefs, contextTracker, statCache, ioScope)
        engine = Engine(
            context = appContext,
            db = db,
            prefs = prefs,
            models = models,
            groups = groups,
            contextTracker = contextTracker,
            ctrlTracker = ctrlTracker,
            outcomes = outcomes,
            statCache = statCache,
            ioScope = ioScope
        )

        // Load channel stats from DB into cache
        ioScope.launch {
            val stats = db.dao().allChannelStats()
            for (st in stats) {
                statCache["${st.pkg}|${st.channelId}"] = st
            }
        }
    }

    fun onConnected(listener: AttentionListener) {
        listenerConnected.value = true
        contextTracker.onConnected()
        engine.reloadHeldKeys()
        ctrlTracker.init()
        outcomes.sweepPending()
    }

    fun onDisconnected() {
        listenerConnected.value = false
        contextTracker.onDisconnected()
    }
}
