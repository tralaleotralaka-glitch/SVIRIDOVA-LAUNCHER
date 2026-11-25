package com.sviridova.nexus

import android.app.Application

class NexusApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Prefs.init(this)
    }
}
