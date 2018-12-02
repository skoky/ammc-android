package com.skoky

import android.app.AlertDialog
import android.content.Context
import android.util.Log
import com.google.firebase.firestore.Query.Direction.ASCENDING
import com.skoky.MyApp.Companion.suffix
import java.util.*

data class Driver(
        val name: String? = null,
        val transponder: String?,
        var uuid: String = UUID.randomUUID().toString(),
        var lastUpdate: Long)

class DriversManager(val app: MyApp) {

    fun delete(transponder: String, context: Context, doneHandler: (String) -> Unit) {
        if (transponder.isEmpty()) {
            doneHandler(transponder)
            return
        }
        AlertDialog.Builder(context)
                .setTitle("Are you sure to delete name to transponder $transponder?")
                .setPositiveButton(R.string.yes) { _, _ ->
                    deleteAfterYes(transponder, doneHandler)
                }
                .setNegativeButton(R.string.no) { dialog, _ ->
                    dialog.dismiss()
                }.create().show()
    }

    private fun deleteAfterYes(transponder: String,doneHandler: (String) -> Unit) {

        app.firestore.collection("drivers$suffix").whereEqualTo("transponder", transponder).get()
                .addOnSuccessListener { qry ->
                    if (qry.documents.isNotEmpty()) {
                        val d = qry.documents.first()!!.id
                        app.firestore.collection("drivers$suffix").document(d).delete().addOnSuccessListener {
                            Log.i(TAG, "Driver $transponder deleted")
                            doneHandler(transponder)
                        }
                    } else {
                        Log.i(TAG, "Driver not found for trans $transponder")
                    }
                }
                .addOnFailureListener { Log.i(TAG, "Driver delete not possible $transponder $it") }
    }

    fun saveTransponder(transponder: String, driverName: String, handler: (String) -> Unit) {

        app.firestore.collection("drivers$suffix").whereEqualTo("transponder", transponder).get()
                .addOnSuccessListener { qry ->

                    if (qry.documents.isNotEmpty()) {
                        val d = qry.documents.first()!!.id
                        app.firestore.collection("drivers$suffix").document(d)
                                .update("name", driverName, "lastUpdate", System.currentTimeMillis())
                                .addOnSuccessListener { Log.d(TAG, "Driver update $transponder"); handler("") }
                                .addOnFailureListener { e -> Log.w(TAG, "Driver not updated $transponder $e"); handler(e.toString()) }
                    } else {
                        val driver = Driver(transponder = transponder, name = driverName, lastUpdate = System.currentTimeMillis())
                        app.firestore.collection("drivers$suffix")
                                .add(driver)
                                .addOnSuccessListener {
                                    Log.d(DriversManager.TAG, "Driver added")
                                    handler("")
                                }
                                .addOnFailureListener {
                                    Log.w(DriversManager.TAG, "Driver adding error $it")
                                    handler("")
                                }
                    }
                }
    }

    fun getDriverForTransponderLastByDate(transponder: String, handler: (String) -> Unit) {
        val q = app.firestore.collection("drivers$suffix").whereEqualTo("transponder", transponder)

        q.get().addOnSuccessListener { qry ->

            if (qry.documents.isNotEmpty()) {
                Log.d(TAG, "Found names for transponder $transponder ${qry.documents.size}")

                val last = qry.documents.maxBy { it.getLong("lastUpdate")!! }
                val d = last!!.getString("name")

                d?.let {
                    handler(it)
                }
            }
        }
    }

    fun driversList(handler: (String, String) -> Unit) {
        app.firestore.collection("drivers$suffix").orderBy("lastUpdate", ASCENDING)
                .limit(1000).get()
                .addOnSuccessListener {
                    it.documents.map { doc ->
                        handler(doc.getString("transponder")!!, doc.getString("name")!!)
                    }
                }.addOnCompleteListener {
                    Log.d(TAG,"Driver done")
                    handler("","")
                }
    }

    companion object {
        val TAG = DriversManager::class.simpleName
    }
}

