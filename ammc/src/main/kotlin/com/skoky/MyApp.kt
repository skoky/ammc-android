package com.skoky

import android.app.Application
import android.os.PowerManager
import com.google.firebase.analytics.FirebaseAnalytics
import com.skoky.services.DecoderService
import java.lang.ref.WeakReference

/**
 * Called automatically when you base your application on the AngApplication in your AndroidManifest.xml
 */
class MyApp : Application() {

    var decoderService: DecoderService? = null
    lateinit var firebaseAnalytics: FirebaseAnalytics

    companion object {
        var wakeLock: PowerManager.WakeLock? = null
    }
}
