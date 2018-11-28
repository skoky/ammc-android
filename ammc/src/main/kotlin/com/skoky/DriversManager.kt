package com.skoky

import android.util.Log
import com.google.firebase.firestore.Query.Direction.DESCENDING
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
            if (result.isSuccessful) {
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

    fun getDriverForTransponderLastByDate(transponder: String, handler: (String) -> Unit): String? {
        val q = app.firestore.collection("drivers").whereEqualTo("transponder", transponder)

        val g = q.get().addOnSuccessListener { qry ->

            if (qry.documents.isNotEmpty()) {
                Log.d(TAG, "Found names for transponder $transponder ${qry.documents.size}")

                val last = qry.documents.maxBy { it.getLong("lastUpdate")!! }
                val d = last!!.getString("name")

                d?.let {
                    handler(it)
                }
            }
        }

        return null
    }

    fun driversList(handler: (String,String)->Unit) {
        app.firestore.collection("drivers").orderBy("lastUpdate", DESCENDING).get()
                .addOnSuccessListener {
                    it.documents.map {doc ->
                        handler(doc.getString("transponder")!!, doc.getString("name")!!)
                    }

        }
    }

    companion object {
        val TAG = DriversManager::class.simpleName
    }
}

