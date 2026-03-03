package com.dc.checkinbb

import android.app.Application
import com.dc.checkinbb.widget.UpdateWidgetWorker
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class CheckInBBApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Inicia actualización periódica del widget cada 15 minutos
        UpdateWidgetWorker.enqueuePeriodic(this)
    }
}
