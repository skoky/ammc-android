package com.skoky

import android.util.Log
import java.util.*

data class Driver(
        val name: String? = null,
        val transponder: String?,
        var uuid: String = UUID.randomUUID().toString(),
        var lastUpdate: Long)

class DriversManager(val app: MyApp) {

    fun saveNewTransponder(transponder: String, driverName: String) {
        Log.d(TAG, "Saving $transponder")

        val driversDb = app.firestore.collection("drivers")
        val driverQuery = driversDb.whereEqualTo("transponder", transponder)

        driverQuery.get().addOnCompleteListener { result ->
            if (result.isSuccessful && result.result!!.isEmpty) {      // transponder not found, save
                saveTransponder(transponder, driverName)
            }
        }
    }

    private fun saveTransponder(transponder: String, driverName: String) {
        val driver = Driver(transponder = transponder, name = driverName, lastUpdate = System.currentTimeMillis())

        app.firestore.collection("drivers")
                .add(driver)
                .addOnSuccessListener {
                    Log.d(DriversManager.TAG, "Bad msg added")
                }
                .addOnFailureListener {
                    Log.w(DriversManager.TAG, "Error adding bad msg")
                }
    }

    fun getDriverForTransponder(transponder: String, handler: (String) -> Unit): String? {
        val q = app.firestore.collection("drivers").whereEqualTo("transponder", transponder)

        val g = q.get().addOnSuccessListener { qry ->

            if (qry.documents.isNotEmpty()) {
                val d = qry.documents.first().getString("name")
                d?.let {
                    handler(it)
                }
            }
        }

        return null
    }

    companion object {
        val TAG = DriversManager::class.simpleName
    }
}

