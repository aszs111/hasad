package com.hasad.app

import android.app.Application
import com.hasad.app.td.Td
import com.hasad.app.work.Jobs

class HasadApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Td.start(this)
        Jobs.scheduleDaily(this)
        Jobs.startDownloads(this)
    }
}
