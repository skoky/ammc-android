package com.skoky

import android.app.Application
import android.os.PowerManager
import com.skoky.MyApp.Companion.cachedApplicationContext
import com.skoky.timing.data.DatabaseHelper
import com.skoky.watchdogs.Watchdog

import java.lang.ref.WeakReference

/**
 * Called automatically when you base your application on the AngApplication in your AndroidManifest.xml
 */
class MyApp : Application() {
    var wb: Watchdog? = null
    var isRaceRunning = false
    lateinit var dbHelper: DatabaseHelper

    companion object {
        var wakeLock: PowerManager.WakeLock? = null

        private var cachedApplicationContext: WeakReference<MainActivity>? = null

        @Synchronized
        fun setCachedApplicationContext(app: MainActivity?) {
            if (app == null)
                throw IllegalArgumentException("Context is null")
            cachedApplicationContext = WeakReference(app)
        }

        /**
         * @return Refference to the ApplicationContext cached in the singleton instance
         */
        @Synchronized
        fun getCachedApplicationContext(): MainActivity {
            if (cachedApplicationContext == null)
                throw IllegalStateException("Application context not available. Initialize the AngApplication using init(Context)")
            return cachedApplicationContext!!.get()
                    ?: throw IllegalStateException("Context reference was garbage collected")
        }
    }


}
