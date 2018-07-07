package com.skoky

//import eu.plib.P3tools.MsgProcessor
//import eu.plib.Ptools.Bytes
//import eu.plib.Ptools.ProtocolsEnum
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.res.Configuration
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.support.design.widget.NavigationView
import android.support.v4.app.Fragment
import android.support.v4.view.GravityCompat
import android.support.v4.widget.DrawerLayout
import android.support.v7.app.ActionBar
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import com.skoky.config.ConfigTool
import com.skoky.fragment.StartupFragment
import com.skoky.fragment.content.StartupContent
import com.skoky.menu.About
import com.skoky.services.DecoderService
import com.skoky.timing.data.DatabaseHelper


class MainActivity : AppCompatActivity(), StartupFragment.OnListFragmentInteractionListener {

    override fun onListFragmentInteraction(item: StartupContent.DummyItem?) {
        Log.d(TAG, "Clicked $item")
    }

    private var ambToConnectTcpIp: String? = null
    private lateinit var app: MyApp
    private lateinit var mDrawerLayout: DrawerLayout

    //    private var timerThread: TimerThread? = null

    private val initialIPAddress: String
        get() {
            val address = ConfigTool.storedAddress

            Log.d(TAG, "Stored address is $address")
            return address
        }

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        app = application as MyApp
        MyApp.setCachedApplicationContext(this)
        app.dbHelper = DatabaseHelper(this)
//        setContentView(R.layout.main)

        setContentView(R.layout.main2)
        mDrawerLayout = findViewById(R.id.drawer_layout)

        val navigationView: NavigationView = findViewById(R.id.nav_view)
        navigationView.setNavigationItemSelectedListener { menuItem ->
            // set item as selected to persist highlight
            menuItem.isChecked = true
            // close drawer when item is tapped
            mDrawerLayout.closeDrawers()

            // Add code here to update the UI based on the item selected
            // For example, swap UI fragments here

            Toast.makeText(applicationContext, "Item: $menuItem", Toast.LENGTH_SHORT).show()
            true
        }

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        val actionbar: ActionBar? = supportActionBar
        actionbar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setHomeAsUpIndicator(R.drawable.ic_menu_black_24dp)
        }

//        val fr : Fragment = FirstFragment.newInstance()
        val fr: Fragment = StartupFragment.newInstance(1)
        val fragmentTransaction = supportFragmentManager.beginTransaction()
        fragmentTransaction.replace(R.id.screen_container, fr)
        fragmentTransaction.commit()

        Log.w(TAG, "Binding service")
        val intent = Intent(this, DecoderService::class.java)
        bindService(intent, decoderServiceConnection, Context.BIND_AUTO_CREATE)


//        screenHandler = screeHandler
//        screenHandler!!.updateScreenForMode(ConfigTool.mode)
//        tryReconnect()
    }

    fun connectOrDisconnect(view: View) {
        app.decoderService!!.connectOrDisconnectFirstDecoder()
    }

    fun showMoreDecoders(view: View) {
        Log.i(TAG, "TBD")
    }

    fun openRacingMode(view: View) {

    }

    fun openTrainingMode(view: View) {

    }

    private val decoderServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName,
                                        service: IBinder) {

            val binder = service as DecoderService.MyLocalBinder
            app.decoderService = binder.getService()

            app.decoderService?.let {
                Log.w(TAG, "Decoder service bound")
                Log.d(TAG, "Service -> " + it.connectDecoder("aDecoder"))
                it.listenOnDecodersBroadcasts()
            }
        }

        override fun onServiceDisconnected(name: ComponentName) {
            app.decoderService = null
        }
    }

    @Suppress("UNUSED_PARAMETER")
    fun openHelp(view: View?) {
        val myIntent = Intent(this@MainActivity, About::class.java)
        this@MainActivity.startActivity(myIntent)
    }

    @Suppress("UNUSED_PARAMETER")
    fun raceStartStop(view: View) {
//        val b = findViewById<ToggleButton>(R.id.buttonStartRace)
//        if (timerThread == null) {
//            b.isChecked = true
//            startRace()
//        } else {
//            b.isChecked = false
//            stopRace(null)
//        }
    }

    @Suppress("UNUSED_PARAMETER")
    fun connectDialog(src: View) {
        Log.d(TAG, "onClick: status")
//
//        val b = findViewById<ToggleButton>(R.id.buttonConnect)
//        if (app.wb != null) {
//            b.isChecked = true
//            app.wb?.let { it.disconnect() }
//            app.wb = null
//        } else {
//            b.isChecked = false
//            openConnectDialog()
//        }
    }

    private fun openConnectDialog() {
        ambToConnectTcpIp = null
//        connectToTcpIp(ConnTypeEnum.TCPIP)
    }
//
//    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
//
//        if (screenHandler!!.mode == ModeEnum.TIMING) {
//            menu.findItem(2).title = "Console mode"
//        } else if (screenHandler!!.mode == ModeEnum.CONSOLE) {
//            menu.findItem(2).title = "Timing mode"
//        }
//
//        if (app.isRaceRunning) {
//            Tools.toast(this, "Options not available while race running")
//            return false
//        }
//
//        if (screenHandler!!.mode != ModeEnum.TIMING)
//            menu.findItem(3).isVisible = false
//
//        return true
//    }

//    override fun onCreateOptionsMenu(menu: Menu): Boolean {
//
//        menu.add(2, 2, 2, "Mode")
//        menu.add(3, 3, 3, "Options")
//        menu.add(4, 4, 4, "Drivers database")
//        menu.add(99, 99, 99, "About")
//
//        return true
//    }
//
//    private fun connectToTcpIp(c: ConnTypeEnum) {
//        val dialog: Dialog = Dialog(this)
//        dialog.setContentView(R.layout.tcpipdialog)
//        dialog.setTitle("TCP/IP address to connectOrDisconnect")
//        dialog.findViewById<Button>(R.id.cancelConnectTcpip).setOnClickListener { dialog.dismiss() }
//
//        val d = dialog.findViewById<Button>(R.id.connectTcpip)
//        d.setOnClickListener {
//            val addr = dialog.findViewById<TextView>(R.id.addressText)
//            ambToConnectTcpIp = addr.text.toString()
//            ConfigTool.saveAddress(ambToConnectTcpIp!!)
//            dialog.dismiss()
//
//            if (c == ConnTypeEnum.TCPIP)
//                app.wb = TcpIpWatchdogThread(a!!, ambToConnectTcpIp, AmbConnectionHandler(a!!))
//            app.wb!!.start()
//        }
//
//
//        val tv = dialog.findViewById(R.id.addressText) as TextView
//        tv.text = initialIPAddress
//        dialog.show()
//    }


//    override fun onOptionsItemSelected(item: MenuItem): Boolean {
//        // Handle item selection
//
//        if (screenHandler!!.mode == ModeEnum.TIMING) {
//
//            when (item.itemId) {
//                2 // timing mode
//                -> {
//                    screenHandler!!.updateScreenForMode(ModeEnum.CONSOLE)
//                }
//            }
//        } else if (screenHandler!!.mode == ModeEnum.CONSOLE) {
//
//            when (item.itemId) {
//                2 // timing mode
//                -> {
//                    screenHandler!!.updateScreenForMode(ModeEnum.TIMING)
//                }
//            }
//        }
//
//        if (item.itemId == 3) {  // timing options
//            return if (screenHandler!!.mode == ModeEnum.CONSOLE) {
////                startActivity(Intent(this@MainActivity, OptionsConsoleActivity::class.java))
//                false
//            } else {
//                startActivity(Intent(this@MainActivity, OptionsActivity::class.java))
//                true
//            }
//        }
//
//        if (item.itemId == 4) { // driver database
//            val intent = Intent(this, DriverDatabaseActivity::class.java)
//            startActivity(intent)
//        }
//        if (item.itemId == 99) {  // about
//            openHelp(null)
//            return true
//        }
//        super.onOptionsItemSelected(item)
//        return false
//
//    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                mDrawerLayout.openDrawer(GravityCompat.START)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)

        // Checks the orientation of the screen
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            Toast.makeText(this, "landscape", Toast.LENGTH_SHORT).show()
        } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            Toast.makeText(this, "portrait", Toast.LENGTH_SHORT).show()
        }
        // Checks whether a hardware keyboard is available
        if (newConfig.hardKeyboardHidden == Configuration.HARDKEYBOARDHIDDEN_NO) {
            Toast.makeText(this, "keyboard visible", Toast.LENGTH_SHORT).show()
        } else if (newConfig.hardKeyboardHidden == Configuration.HARDKEYBOARDHIDDEN_YES) {
            Toast.makeText(this, "keyboard hidden", Toast.LENGTH_SHORT).show()
        }

        Log.d(TAG, "Layout:" + newConfig.screenLayout)
    }

    //
    fun stopRace(handler: Handler?) {
//        app.isRaceRunning = false
//        if (handler == null) {  // main thread
//            val button = findViewById(R.id.buttonStartRace) as ToggleButton
//            button.isChecked = app.isRaceRunning
//            Tools.wakeLock(this, false)
//        } else {
//            val msg = Message.obtain(handler, ScreenHandlerCommon.END_RACE, 0, 0)
//            handler.dispatchMessage(msg)
//        }
//        if (timerThread != null) timerThread!!.shouldCancel = true
//        timerThread = null
//
    }

    //
//
//    private fun startRace() {
//        screenHandler!!.clearRacingScreen()
//        app.isRaceRunning = true
//        timerThread = TimerThread(screenHandler!!.updateHandler, ConfigTool.raceTime.toLong())
//        timerThread!!.start()
//        Tools.wakeLock(this, true)
//    }
//
//    fun connectToAMB() {
//
//        Log.d(TAG, "Connecting to...")
//        if (ambToConnectTcpIp != null) {
//            Log.d(TAG, "Connecting to TCP/IP..." + ambToConnectTcpIp!!)
//            app.wb = TcpIpWatchdogThread(a!!, ambToConnectTcpIp, AmbConnectionHandler(a!!))
//            app.wb!!.start()
//        } else {
//            Tools.toast(this, "No AMB found")
//        }
//    }
//
//    @Suppress("UNUSED_PARAMETER")
//    fun protocolSelect(view: View) {
//    }
//
//    private inner class TimerThread(private val handler: Handler, private val raceTime: Long) : Thread() {
//        private val startTime: Long
//        var shouldCancel = false
//        private var msg: Message? = null
//
//        init {
//            startTime = System.currentTimeMillis()
//            Log.d(TAG, "Init done")
//        }
//
//        override fun run() {
//            super.run()
//            Log.d(TAG, "Run")
//            while (app.isRaceRunning)
//                try {
//                    Log.v(TAG, "Fired...")
//                    val duration = System.currentTimeMillis() - startTime
//                    if (duration > raceTime * 1000) {
//                        stopRace(handler)
//                        break
//                    }
//                    msg = Message.obtain(handler, ScreenHandlerCommon.UPDATE_RACE_TIME, duration.toInt(), raceTime.toInt())
//                    handler.dispatchMessage(msg)
//
//                    Thread.sleep(1000)
//                } catch (e: InterruptedException) {
//                    e.printStackTrace()
//                    stopRace(handler)
//                }
//
//            stopRace(handler)
//        }
//
//    }
//
    fun parseRecord(data: Byte): String? {
//        val m: eu.plib.Ptools.Message
//        if (selectedProtocol == null) selectedProtocol = ProtocolsEnum.P3
//        if (selectedProtocol!!.id === ProtocolsEnum.P3.id)
//            m = MsgProcessor().parse(data.bytes)
//        else
//            return null
//        Log.v(TAG, "Record parsed:$m")
//        return m
        return "nic"
    }

    override fun onDestroy() {
        super.onDestroy()
        if (app.wb != null) app.wb!!.disconnect()
    }

    companion object {
        private val TAG = "MainActivity"
    }


}