package com.skoky.timing.data

import android.app.ListFragment
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ListView
import com.shamanland.fab.FloatingActionButton
import com.skoky.MyApp
import com.skoky.R
import com.skoky.timing.DriverDatabaseActivity

class DriverListFragment : ListFragment() {
    override fun onListItemClick(l: ListView, v: View, position: Int, id: Long) {
        val d = l.adapter.getItem(position) as MyDriver
        (activity as DriverDatabaseActivity).openDriverEditor(d.id)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        Log.w(TAG, "Inflating")
        val view = inflater.inflate(R.layout.drivers_list, container, false)

        val app = activity.application as MyApp
        var allDrivers: List<MyDriver> = app.dbHelper.driverDao.queryForAll()

        Log.d(TAG, "Listing " + allDrivers.size + " drivers")

        val button = activity.findViewById<FloatingActionButton>(R.id.addDriverButton)
        button.setOnClickListener { v ->
            v.visibility = View.INVISIBLE
            val a = activity as DriverDatabaseActivity
            a.openDriverEditor(null)
        }

        val adapter = ArrayAdapter(inflater.context, R.layout.drivers_list_row, allDrivers)
        listAdapter = adapter
        return view
    }

    companion object {
        private const val TAG = "DriverListFragment"
    }

}
