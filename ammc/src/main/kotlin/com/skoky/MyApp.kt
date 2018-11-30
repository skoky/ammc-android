package com.skoky

import android.app.Application
import android.os.PowerManager
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.skoky.services.DecoderService

class MyApp : Application() {

    var decoderService: DecoderService? = null
    lateinit var firebaseAnalytics: FirebaseAnalytics
    lateinit var drivers: DriversManager
    var user: FirebaseUser? = null
    lateinit var firestore: FirebaseFirestore
    val options = mutableMapOf<String,Any>("badmsg" to true, "driversync" to true)

    val recentTransponders = hashSetOf<String>()

    companion object {
        var wakeLock: PowerManager.WakeLock? = null
    }
}
