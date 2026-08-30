package com.aylis

import android.app.Application
import com.aylis.core.CrashHandler

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // Initialize global crash handler
        CrashHandler.init(this)
    }
}
