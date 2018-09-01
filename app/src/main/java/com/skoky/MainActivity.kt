package com.skoky

import android.app.Dialog
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.res.Configuration
import android.os.Bundle
import android.os.IBinder
import android.support.v4.app.Fragment
import android.support.v4.view.GravityCompat
import android.support.v7.app.ActionBar
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.*
import com.skoky.fragment.StartupFragment
import com.skoky.fragment.TrainingModeFragment
import com.skoky.fragment.content.Lap
import com.skoky.services.Decoder
import com.skoky.services.DecoderService
import kotlinx.android.synthetic.main.fragment_trainingmode_list.*
import kotlinx.android.synthetic.main.main.*
import kotlinx.android.synthetic.main.select_decoder.*
import kotlinx.android.synthetic.main.select_decoder.view.*


class MainActivity : AppCompatActivity(), TrainingModeFragment.OnListFragmentInteractionListener {

    override fun onListFragmentInteraction(item: Lap?) {
        Log.w(TAG, "Interaction $item")
    }

    private lateinit var app: MyApp
//    private lateinit var mDrawerLayout: DrawerLayout

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        app = application as MyApp
        MyApp.setCachedApplicationContext(this)

        setContentView(R.layout.main)

        nav_view.setNavigationItemSelectedListener { menuItem ->
            drawer_layout.closeDrawers()

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
        val fragmentTransaction = supportFragmentManager.beginTransaction()
        fragmentTransaction.replace(R.id.screen_container, StartupFragment())
        fragmentTransaction.commit()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    fun connectOrDisconnect(view: View) {
        app.decoderService?.let { it.connectOrDisconnectFirstDecoder() }
    }

    fun showMoreDecoders(view: View) {

        val d = Dialog(this)
        d.setContentView(R.layout.select_decoder)

        val decodersCopy = app.decoderService!!.getDecoders()

        val decoderText = d.decoder_address_edittext
        val dd = d.known_decoders

        decodersCopy.forEach {
            if (it.ipAddress != null) {
                val b = RadioButton(this)
                b.text = MainActivity.decoderLabel(it)
                b.isChecked = false
                b.id = it.id.hashCode()
                b.setOnCheckedChangeListener { view, checked ->
                    Log.d(TAG, "Checked $view $checked")
                    decoderText.text = null
                }
                dd.addView(b)
            }
        }


        decoderText.addTextChangedListener(SimpleTextWatcher(dd))

        d.decoder_select_ok_button.setOnClickListener {
            val checkDecoder = d.known_decoders.checkedRadioButtonId
            val foundDecoder = decodersCopy.find { it.id.hashCode() == checkDecoder }
            Log.i(TAG, "decoder $foundDecoder")

            if (decoderText.text.isNotEmpty()) {
                app.decoderService?.let { it.connectDecoder(decoderText.text.toString()) }
            } else
                foundDecoder?.let { app.decoderService?.let{ s -> s.connectDecoder2(it) } }

            d.cancel()
        }


        d.setCancelable(true)
        d.setOnCancelListener { it.cancel() }
        d.show()
    }

    class SimpleTextWatcher(dd: RadioGroup) : TextWatcher {
        val ddd = dd
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            ddd.clearCheck()
        }

        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
        }

        override fun afterTextChanged(s: Editable?) {
        }

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
            trainingFragment.openTransponderDialog(false)
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
                // FIXME connect last decoder Log.d(TAG, "Service -> " + it.connectDecoder("aDecoder"))
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
                drawer_layout.openDrawer(GravityCompat.START)
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


    companion object {
        private const val TAG = "MainActivity"

        fun decoderLabel(d: Decoder): String {
            val type: String = d.decoderType ?: d.id
            return if (d.ipAddress != null) "$type / ${d.ipAddress}" else type
        }
    }
}