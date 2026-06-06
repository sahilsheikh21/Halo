package com.halo

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class HaloApplication : Application() {
    override fun onCreate() {
        super.onCreate()
    }
}
