package com.skoky

import android.app.Application
import android.os.PowerManager
import com.skoky.services.DecoderService
import java.lang.ref.WeakReference

/**
 * Called automatically when you base your application on the AngApplication in your AndroidManifest.xml
 */
class MyApp : Application() {

    var decoderService : DecoderService? = null

    companion object {
        var wakeLock: PowerManager.WakeLock? = null

        private var cachedApplicationContext: WeakReference<MainActivity>? = null

        @Synchronized
        fun setCachedApplicationContext(app: MainActivity?) {
            if (app == null)
                throw IllegalArgumentException("Context is null")
            cachedApplicationContext = WeakReference(app)
        }

    }


}
