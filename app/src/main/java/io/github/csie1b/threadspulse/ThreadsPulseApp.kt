package io.github.csie1b.threadspulse

import android.app.Application
import io.github.csie1b.threadspulse.data.PrefsManager

class ThreadsPulseApp : Application() {
    override fun onCreate() {
        super.onCreate()
        PrefsManager.init(this)
    }
}
