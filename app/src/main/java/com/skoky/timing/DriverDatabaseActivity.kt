package com.skoky.timing

import android.app.Activity
import android.app.Fragment
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import com.shamanland.fab.FloatingActionButton
import com.skoky.MyApp
import com.skoky.R
import com.skoky.timing.data.DriverEditorFragment
import com.skoky.timing.data.DriverListFragment
import com.skoky.timing.data.MyDriver
import java.sql.SQLException
import java.util.*

class DriverDatabaseActivity : Activity() {
    private var lastFragment: Fragment? = null

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.driverdb)
        showList()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menu.add(10, 10, 10, "Add driver")
        return true
    }

    override fun onMenuItemSelected(featureId: Int, item: MenuItem): Boolean {
        if (item.itemId == 10) {
            openDriverEditor(null)
            return true
        }
        return false

    }

    fun openDriverEditor(id: Int?) {
        Log.d(TAG, "Opening Driver editor with id $id")
        val transaction = fragmentManager.beginTransaction()

        var editorFragment: DriverEditorFragment? = null
        if (id != null) {
            var driver: MyDriver? = null
            if (id != null) {
                try {
                    driver = (application as MyApp).dbHelper!!.driverDao!!.queryForId(id)
                    editorFragment = DriverEditorFragment()
                    editorFragment.setDriver(driver!!)
                } catch (e: SQLException) {
                    e.printStackTrace()
                }

            }
        }
        if (editorFragment == null)
            editorFragment = DriverEditorFragment()

        lastFragment?.let { transaction.remove(lastFragment) }
        transaction.add(R.id.driverContainer, editorFragment)
        lastFragment = editorFragment
        transaction.commit()
    }

    fun showList() {
        val transaction = fragmentManager.beginTransaction()
        val simpleFragment = DriverListFragment()
//         DriverEditorFragment simpleFragment = new DriverEditorFragment();
        if (lastFragment != null) transaction.remove(lastFragment)
        transaction.add(R.id.driverContainer, simpleFragment)
        transaction.commit()
        lastFragment = simpleFragment
        findViewById<FloatingActionButton>(R.id.addDriverButton).visibility = View.VISIBLE
    }


    fun addDriver(view: View) {
        val nameV = findViewById(R.id.driver_editor_name) as TextView
        val name = nameV.text.toString()

        var foundDriver: List<MyDriver> = ArrayList(0)
        try {
            foundDriver = (application as MyApp).dbHelper.driverDao.queryForEq("name", name)
        } catch (e: SQLException) {
            e.printStackTrace()  //To change body of catch statement use File | Settings | File Templates.
        }

        val tidV = findViewById(R.id.driver_editor_tid) as TextView
        val tid = tidV.text.toString()

        if (foundDriver.size > 0) {
            // update driver
            val driver = foundDriver[0]
            driver.transponderId = tid
            driver.name = name
            try {
                (application as MyApp).dbHelper.driverDao.update(driver)
            } catch (e: SQLException) {
                e.printStackTrace()  //To change body of catch statement use File | Settings | File Templates.
            }

        } else {   // create driver

            try {
                (application as MyApp).dbHelper.driverDao.create(MyDriver(name, tid))
            } catch (e: SQLException) {
                e.printStackTrace()  //To change body of catch statement use File | Settings | File Templates.
            }

        }
        showList()
    }

    fun deleteDriver(view: View) {
        val nameV = findViewById(R.id.driver_editor_name) as TextView
        val name = nameV.text.toString()

        var foundDriver: List<MyDriver> = ArrayList(0)
        try {
            foundDriver = (application as MyApp).dbHelper!!.driverDao!!.queryForEq("name", name)
        } catch (e: SQLException) {
            e.printStackTrace()  //To change body of catch statement use File | Settings | File Templates.
        }

        if (foundDriver.size == 0) Log.d(TAG, "No driver found")

        for (d in foundDriver) {
            try {
                (application as MyApp).dbHelper!!.driverDao!!.delete(d)
            } catch (e: SQLException) {
                e.printStackTrace()  //To change body of catch statement use File | Settings | File Templates.
            }

        }
        showList()
    }

    fun cancelEdit(view: View) {
        showList()
    }

    companion object {

        private val TAG = "DriverDatabaseActivity"
    }

}