package com.wizt.common.base

import android.app.Application
import com.squareup.leakcanary.LeakCanary
import com.wizt.utils.PreferenceUtils

class BaseApplication : Application {

    constructor() : super()

    companion object {
        lateinit var instance: Application
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
//        initLeakCanary()

        // Init Shared Preference
        PreferenceUtils.init(applicationContext)
    }

    /**
     * LeakCanary to detect memory leak
     */
    fun initLeakCanary() {
        if (LeakCanary.isInAnalyzerProcess(this)) {
            return
        }
        LeakCanary.install(this)
    }
}