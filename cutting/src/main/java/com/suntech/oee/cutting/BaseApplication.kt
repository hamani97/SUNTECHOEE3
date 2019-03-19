package com.suntech.oee.cutting

import android.app.Application
import com.bugfender.sdk.Bugfender

/**
 * Created by rightsna on 2018. 1. 9..
 */
class BaseApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        Bugfender.init(this, "NJGPNgfndcI6hDHGT42goJGgVmTnU1tG", BuildConfig.DEBUG);
        Bugfender.enableLogcatLogging()
        Bugfender.enableUIEventLogging(this)
        Bugfender.enableCrashReporting()
    }
}