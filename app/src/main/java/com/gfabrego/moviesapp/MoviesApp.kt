package com.gfabrego.moviesapp

import android.app.Application
import com.squareup.leakcanary.LeakCanary

class MoviesApp : Application() {

    override fun onCreate() {
        super.onCreate()
        if (LeakCanary.isInAnalyzerProcess(this)) return
        LeakCanary.install(this)
    }
}
