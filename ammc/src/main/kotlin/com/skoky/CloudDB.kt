package com.skoky

import android.util.Log
import com.skoky.MyApp.Companion.suffix
import java.text.SimpleDateFormat
import java.util.*

object CloudDB {

    fun badMessageReport(app: MyApp, key : String,  bytes: String) {

        val enabled = app.options["badmsg"] as Boolean
        if (!enabled) return

        val formatter = SimpleDateFormat("yy/MM/dd")
        val today = formatter.format(Date())

        val msg = hashMapOf(key to bytes, "len" to bytes.length, "date" to today) as Map<String,String>

        app.firestore.collection("badmsg$suffix")
                .add(msg)
                .addOnSuccessListener {
                    Log.d(DriversManager.TAG, "Bad msg added with ID: ${it.id}")
                }
                .addOnFailureListener {
                    Log.w(DriversManager.TAG, "Error adding bad msg", it) }
    }
}