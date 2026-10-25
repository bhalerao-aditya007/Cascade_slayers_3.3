package com.cascade.attentiontwin

import android.app.Application
import com.cascade.attentiontwin.engine.Simulator

class TwinApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Graph.init(this)
        Simulator.setupChannels(this)
    }
}
