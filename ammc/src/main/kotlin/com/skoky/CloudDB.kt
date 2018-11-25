package com.skoky

import android.util.Log

object CloudDB {

    fun badMessageReport(app: MyApp, key : String,  bytes: String) {

        val msg = hashMapOf(key to bytes) as Map<String,String>

        app.firestore.collection("badmsg")
                .add(msg)
                .addOnSuccessListener {
                    Log.d(DriversManager.TAG, "Bad msg added with ID: ${it.id}")
                }
                .addOnFailureListener {
                    Log.w(DriversManager.TAG, "Error adding bad msg", it) }
    }
}