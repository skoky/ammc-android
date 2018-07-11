package com.skoky

//import eu.plib.P3tools.MsgProcessor
//import eu.plib.Ptools.Bytes
//import eu.plib.Ptools.ProtocolsEnum
import android.app.Dialog
import android.content.*
import android.content.res.Configuration
import android.os.Bundle
import android.os.IBinder
import android.support.design.widget.NavigationView
import android.support.v4.app.DialogFragment
import android.support.v4.app.Fragment
import android.support.v4.view.GravityCompat
import android.support.v4.widget.DrawerLayout
import android.support.v7.app.ActionBar
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Toast
import com.skoky.config.ConfigTool
import com.skoky.fragment.StartupFragment
import com.skoky.fragment.TrainingModeFragment
import com.skoky.fragment.content.Lap
import com.skoky.services.DecoderService
import com.skoky.timing.data.DatabaseHelper
import kotlinx.android.synthetic.main.fragment_trainingmode_list.*
import kotlinx.android.synthetic.main.select_decoder.*


class MainActivity : AppCompatActivity(), TrainingModeFragment.OnListFragmentInteractionListener {

    override fun onListFragmentInteraction(item: Lap?) {
        Log.w(TAG, "Interaction $item")
    }

    private lateinit var app: MyApp
    private lateinit var mDrawerLayout: DrawerLayout

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        app = application as MyApp
        MyApp.setCachedApplicationContext(this)
        app.dbHelper = DatabaseHelper(this)

        setContentView(R.layout.main)
        mDrawerLayout = findViewById(R.id.drawer_layout)

        val navigationView: NavigationView = this.findViewById(R.id.nav_view)
        navigationView.setNavigationItemSelectedListener { menuItem ->
            mDrawerLayout.closeDrawers()

            when (menuItem.itemId) {
                R.id.nav_training -> menuItem.isChecked = openTrainingMode(null)
                R.id.nav_racing -> menuItem.isChecked = openRacingMode(null)
                R.id.nav_console -> menuItem.isChecked = openConsoleMode(null)
                else -> {
                    menuItem.isChecked = false
                    Log.w(TAG, "Unknown mode $menuItem")
                }
            }
            true
        }

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        val actionbar: ActionBar? = supportActionBar
        actionbar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setHomeAsUpIndicator(R.drawable.ic_menu_black_24dp)
        }

        openStartupFragment()

        Log.w(TAG, "Binding service")
        val intent = Intent(this, DecoderService::class.java)
        bindService(intent, decoderServiceConnection, Context.BIND_AUTO_CREATE)

    }

    private fun openStartupFragment() {
        val fr: Fragment = StartupFragment.newInstance(1)
        val fragmentTransaction = supportFragmentManager.beginTransaction()
        fragmentTransaction.replace(R.id.screen_container, fr)
        fragmentTransaction.commit()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    fun connectOrDisconnect(view: View) {
        app.decoderService!!.connectOrDisconnectFirstDecoder()
    }

    fun showMoreDecoders(view: View) {

        val d = Dialog(this)
        d.setContentView(R.layout.select_decoder)

        val decodersCopy = app.decoderService!!.getDecoders()

        decodersCopy.forEach {
            if (it.ipAddress != null) {
                val b = RadioButton(this)
                b.text = "${it.decoderType} / ${it.ipAddress}"
                b.isChecked = false
                b.id = it.id.hashCode()
                d.findViewById<RadioGroup>(R.id.known_decoders).addView(b)
            }
        }

        d.findViewById<Button>(R.id.decoder_select_ok_button).setOnClickListener {
            val checkDecoder = d.findViewById<RadioGroup>(R.id.known_decoders).checkedRadioButtonId
            val foundDecoder = decodersCopy.find { it.id.hashCode() == checkDecoder }
            Log.i(TAG, "decoder $foundDecoder")
            foundDecoder?.let { app.decoderService!!.connectDecoder(it.ipAddress!!) }

            d.cancel()
        }
        d.setCancelable(true)
        d.setOnCancelListener { it.cancel() }
        d.show()
    }

    fun openRacingMode(view: View?): Boolean {
        Log.i(TAG, "TBD")
        return false
    }

    fun openConsoleMode(view: View?): Boolean {
        Log.i(TAG, "TBD")
        return false
    }


    fun openTransponderDialog(view: View?) {
        if (!trainingFragment.running) {
            val trs = trainingFragment.transponders.toTypedArray()

            val b = AlertDialog.Builder(this@MainActivity)
                    .setTitle(getString(R.string.select_label))
            if (trs.isEmpty()) {
                b.setMessage(getString(R.string.no_transponder))
            } else {
                b.setSingleChoiceItems(trs, 0) { dialog, i ->
                    Log.w(TAG, "Selected $i")
                    trainingFragment.setSelectedTransponder(trs[i])
                    decoderIdSelector.text = trs[i]
                    dialog.cancel()
                }
            }
            b.create().show()
        }
    }

    private lateinit var trainingFragment: TrainingModeFragment
    fun openTrainingMode(view: View?): Boolean {

        app.decoderService?.let {
            if (it.isDecoderConnected()) {
                trainingFragment = TrainingModeFragment.newInstance(1)
                val fragmentTransaction = supportFragmentManager.beginTransaction()
                fragmentTransaction.replace(R.id.screen_container, trainingFragment)
                fragmentTransaction.commit()
                return true
            } else {
                AlertDialog.Builder(this).setMessage(getString(R.string.decoder_not_connected)).setCancelable(true).create().show()
                return false
            }
        }
        return false
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
            Log.w(TAG, "Service disconnected?")
            finish()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        Log.i(TAG, "Option menu ${item.itemId}")
        return when (item.itemId) {
            com.skoky.R.id.miHome -> {
                openStartupFragment()
                true
            }
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


    override fun onDestroy() {
        super.onDestroy()

    }

    companion object {
        private val TAG = "MainActivity"
    }


}