package com.skoky

import android.util.Log
import com.skoky.MyApp.Companion.suffix
import java.text.SimpleDateFormat
import java.util.*

object CloudDB {

    fun badMessageReport(app: MyApp, key: String, bytes: String) {


        if (!app.badMsgReport) return

        val formatter = SimpleDateFormat("yy/MM/dd", Locale.US)
        val today = formatter.format(Date())

        val msg = hashMapOf<String, Any>(key to bytes, "len" to bytes.length, "date" to today)

        app.firestore.collection("badmsg$suffix")
                .add(msg)
                .addOnSuccessListener {
                    Log.d(DriversManager.TAG, "Bad msg added with ID: ${it.id}")
                }
                .addOnFailureListener {
                    Log.w(DriversManager.TAG, "Error adding bad msg", it)
                }
    }

}