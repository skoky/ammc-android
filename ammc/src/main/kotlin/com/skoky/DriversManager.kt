package com.skoky

import android.util.Log
import java.util.*


data class Driver(
        val name: String? = null,
        val transponder: String?,
        var uuid: String = UUID.randomUUID().toString())

class DriversManager(val app: MyApp) {

    fun saveNewTransponder(transponder: String) {
        Log.d(TAG, "Saving $transponder")

        val driversDb = app.firestore.collection("drivers")
        val driverQuery = driversDb.whereEqualTo("transponder", transponder)

        driverQuery.get().addOnCompleteListener { result ->
            if (result.isSuccessful && result.result!!.isEmpty) {      // transponder not found, save
                saveTransponder(transponder)
            }
        }
    }

    private fun saveTransponder(transponder: String) {
        val driver = Driver(transponder=transponder)
        app.firestore.collection("drivers")
                .add(driver)
                .addOnSuccessListener {
                    Log.d(DriversManager.TAG, "Bad msg added with ID: ${it.id}")
                }
                .addOnFailureListener {
                    Log.w(DriversManager.TAG, "Error adding bad msg", it)
                }
    }

    companion object {
        val TAG = DriversManager::class.simpleName
    }
}

