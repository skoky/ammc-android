package com.skoky.timing.data

import android.app.Fragment
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import com.skoky.MyApp
import com.skoky.R
import com.skoky.timing.DriverDatabaseActivity

class DriverEditorFragment : Fragment() {
    private var driver: MyDriver? = null
    private var app: MyApp? = null
    private var a: DriverDatabaseActivity? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        app = activity.application as MyApp
        a = activity as DriverDatabaseActivity
        // Inflate the layout for this fragment

        val view = inflater.inflate(R.layout.drivers_editor, container, false)
        if (driver != null) {
            val nameV = view.findViewById(R.id.driver_editor_name) as EditText
            nameV.setText(driver!!.name)
            val tidV = view.findViewById(R.id.driver_editor_tid) as EditText
            tidV.setText(driver!!.transponderId)
            val b = view.findViewById(R.id.editOkButton) as Button
            b.text = "Update driver"
        }
        return view
    }

    fun setDriver(driver: MyDriver) {
        this.driver = driver
    }

    companion object {
        private val TAG = "DriverEditorFragment"
    }
}
