package com.skoky

import android.app.Dialog
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.res.Configuration
import android.os.Bundle
import android.os.IBinder
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
import android.widget.RadioButton
import android.widget.RadioGroup
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.firebase.analytics.FirebaseAnalytics
import com.skoky.fragment.*
import com.skoky.fragment.content.ConsoleModel
import com.skoky.fragment.content.Racer
import com.skoky.fragment.content.TrainingLap
import com.skoky.services.Decoder
import com.skoky.services.DecoderService
import kotlinx.android.synthetic.main.main.*
import kotlinx.android.synthetic.main.select_decoder.*
import kotlinx.android.synthetic.main.startup_content.*


class MainActivity : AppCompatActivity(),
        TrainingModeFragment.OnListFragmentInteractionListener,
        RacingModeFragment.OnListFragmentInteractionListener,
        ConsoleModeFragment.OnListFragmentInteractionListener,
        ConsoleModeVostokFragment.OnListFragmentInteractionListener {

    override fun onListFragmentInteraction(item: ConsoleModel?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onListFragmentInteraction(item: Racer?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onListFragmentInteraction(item: TrainingLap?) {
        Log.w(TAG, "Interaction $item")
    }

    private lateinit var app: MyApp
    private var mAdView: AdView? = null

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        app = application as MyApp
        MyApp.setCachedApplicationContext(this)

        app.firebaseAnalytics = FirebaseAnalytics.getInstance(this)

        setContentView(R.layout.main)

        nav_view.setNavigationItemSelectedListener { menuItem ->
            drawer_layout.closeDrawers()

            when (menuItem.itemId) {
                R.id.nav_training -> menuItem.isChecked = openTrainingMode(null)
                R.id.nav_racing -> menuItem.isChecked = openRacingMode(null)
                R.id.nav_console -> menuItem.isChecked = openConsoleMode(null)
                R.id.nav_connection_help -> menuItem.isChecked = openHelp(null)
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

        mAdView = findViewById<View>(R.id.adView) as AdView?
        val adRequest = AdRequest.Builder().build()
        mAdView?.loadAd(adRequest)
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
        app.decoderService?.let { ds ->
            if (ds.isDecoderConnected()) {
                ds.disconnectAllDecoders()
//                ds.disconnectDecoderByIpUUID(firstDecoderId.tag as String)
            } else {
                ds.connectDecoderByUUID(firstDecoderId.tag as String)
            }

        }
    }

    fun doMakeAWish(view: View) {

        AlertDialog.Builder(this).setMessage(getString(R.string.well))
                .setPositiveButton(getString(R.string.send_email)) { _, _ ->

                    val intent = Intent(Intent.ACTION_SEND)
                    intent.type = "plain/text"
                    intent.putExtra(android.content.Intent.EXTRA_EMAIL, arrayOf<String>("skokys@gmail.com"))
                    intent.putExtra(android.content.Intent.EXTRA_SUBJECT, "AMM wish from Android")
                    intent.putExtra(android.content.Intent.EXTRA_TEXT, "I wish....")
                    startActivity(Intent.createChooser(intent, "Send"))
                }.setCancelable(true).create().show()

    }

    val AMMC_PREFS = "ammcprefs"
    val LAST_IP = "lastip"

    fun showMoreDecoders(view: View) {

        val dialog = Dialog(this)
        dialog.setContentView(R.layout.select_decoder)

        val decodersCopy = app.decoderService!!.getDecoders()

        val decoderText = dialog.decoder_address_edittext
        decoderText.setText(getSharedPreferences(AMMC_PREFS, 0).getString(LAST_IP, ""))

        val radioButton = dialog.known_decoders

        decodersCopy.forEach { d2 ->
            if (d2.ipAddress != null) {
                val b = RadioButton(this)
                b.text = MainActivity.decoderLabel(d2)
                b.isChecked = false
                b.id = d2.hashCode()
                b.setOnCheckedChangeListener { view, checked ->
                    Log.d(TAG, "Checked $view $checked")
                    decoderText.text = null
                }
                radioButton.addView(b)
            }
        }

        decoderText.addTextChangedListener(SimpleTextWatcher(radioButton))

        dialog.decoder_select_ok_button.setOnClickListener {
            val checkDecoderHashCode = dialog.known_decoders.checkedRadioButtonId
            val foundDecoder = decodersCopy.find { decoder -> decoder.hashCode() == checkDecoderHashCode }
            Log.i(TAG, "decoder $foundDecoder")

            if (decoderText.text.isNotEmpty()) {
                val dd = decoderText.text.toString().trim()
                getSharedPreferences(AMMC_PREFS, 0).edit().putString(LAST_IP, dd).commit()
                app.decoderService?.let { s -> s.connectDecoder(dd) }
            } else
                foundDecoder?.let { d3 -> app.decoderService?.let { s -> s.connectDecoder2(d3) } }

            dialog.cancel()
        }

        dialog.setCancelable(true)
        dialog.setOnCancelListener { it.cancel() }
        dialog.show()
    }

    class SimpleTextWatcher(dd: RadioGroup) : TextWatcher {
        private val ddd = dd
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            ddd.clearCheck()
        }

        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
        }

        override fun afterTextChanged(s: Editable?) {
        }

    }

    private lateinit var helpFragment: HelpFragment
    private fun openHelp(view: View?): Boolean {
        helpFragment = HelpFragment.newInstance(1)
        val fragmentTransaction = supportFragmentManager.beginTransaction()
        fragmentTransaction.replace(R.id.screen_container, helpFragment)
        fragmentTransaction.commit()

        return true
    }

    private lateinit var consoleFragment: ConsoleModeFragment
    private lateinit var consoleVostokFragment: ConsoleModeVostokFragment
    fun openConsoleMode(view: View?): Boolean {
        val decoderUUID = firstDecoderId.tag as? String

        if (decoderUUID == null) {
            AlertDialog.Builder(this).setMessage(getString(R.string.decoder_not_connected))
                    .setCancelable(true).create().show()
            return true
        } else {
            app.decoderService?.let {
                return if (it.isDecoderConnected()) {
                    val fragmentTransaction = supportFragmentManager.beginTransaction()
                    if (it.isConnectedDecoderVostok()) {
                        consoleVostokFragment = ConsoleModeVostokFragment.newInstance(1)
                        fragmentTransaction.replace(R.id.screen_container, consoleVostokFragment)
                    } else {
                        fragmentTransaction.replace(R.id.screen_container, consoleFragment)
                        consoleFragment = ConsoleModeFragment.newInstance(1)
                    }
                    fragmentTransaction.commit()

                    true

                } else {
                    AlertDialog.Builder(this).setMessage(getString(R.string.decoder_not_connected)).setCancelable(true).create().show()
                    false
                }
            }
            return false
        }
    }


    fun openTransponderDialog(view: View?) {
        if (!trainingFragment.running) {
            trainingFragment.openTransponderDialog(false)
        }
    }

    private lateinit var racingFragment: RacingModeFragment
    fun openRacingMode(view: View?): Boolean {
        app.decoderService?.let {
            return if (it.isDecoderConnected()) {
                racingFragment = RacingModeFragment.newInstance(1)
                val fragmentTransaction = supportFragmentManager.beginTransaction()
                fragmentTransaction.replace(R.id.screen_container, racingFragment)
                fragmentTransaction.commit()
                true
            } else {
                AlertDialog.Builder(this).setMessage(getString(R.string.decoder_not_connected)).setCancelable(true).create().show()
                false
            }
        }
        return false

    }

    private lateinit var trainingFragment: TrainingModeFragment
    fun openTrainingMode(view: View?): Boolean {

        app.decoderService?.let {
            return if (it.isDecoderConnected()) {
                trainingFragment = TrainingModeFragment.newInstance(1)
                val fragmentTransaction = supportFragmentManager.beginTransaction()
                fragmentTransaction.replace(R.id.screen_container, trainingFragment)
                fragmentTransaction.commit()
                true
            } else {
                AlertDialog.Builder(this).setMessage(getString(R.string.decoder_not_connected)).setCancelable(true).create().show()
                false
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
            //          Toast.makeText(this, "landscape", Toast.LENGTH_SHORT).show()
        } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
//            Toast.makeText(this, "portrait", Toast.LENGTH_SHORT).show()
        }
        // Checks whether a hardware keyboard is available
        if (newConfig.hardKeyboardHidden == Configuration.HARDKEYBOARDHIDDEN_NO) {
            //        Toast.makeText(this, "keyboard visible", Toast.LENGTH_SHORT).show()
        } else if (newConfig.hardKeyboardHidden == Configuration.HARDKEYBOARDHIDDEN_YES) {
            //      Toast.makeText(this, "keyboard hidden", Toast.LENGTH_SHORT).show()
        }

        Log.d(TAG, "Layout:" + newConfig.screenLayout)
    }

// FIXME disconnect service once finishing
//    override fun onDestroy() {
//        super.onDestroy()
//        app.decoderService?.let { unbindService(it... connection) }
//    }


    companion object {
        private const val TAG = "MainActivity"

        fun decoderLabel(d: Decoder): String {

            if (d.decoderType != null && d.ipAddress != null) return "${d.decoderType} / ${d.ipAddress}"

            d.ipAddress?.let { return it }

            d.decoderId?.let { return it }

            return d.uuid.toString()
        }
    }
}