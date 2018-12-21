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
import android.widget.CheckBox
import android.widget.EditText
import android.widget.RadioButton
import android.widget.RadioGroup
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.skoky.fragment.*
import com.skoky.fragment.content.ConsoleModel
import com.skoky.fragment.content.Racer
import com.skoky.fragment.content.TrainingLap
import com.skoky.services.Decoder
import com.skoky.services.DecoderService
import kotlinx.android.synthetic.main.fragment_options.*
import kotlinx.android.synthetic.main.main.*
import kotlinx.android.synthetic.main.select_decoder.*
import kotlinx.android.synthetic.main.startup_content.*
import org.jetbrains.anko.defaultSharedPreferences
import org.jetbrains.anko.toast


class MainActivity : AppCompatActivity(),
        TrainingModeFragment.OnListFragmentInteractionListener,
        RacingModeFragment.OnListFragmentInteractionListener,
        DriversFragment.OnListFragmentInteractionListener,
        ConsoleModeFragment.OnListFragmentInteractionListener,
        ConsoleModeVostokFragment.OnListFragmentInteractionListener {

    override fun onListFragmentInteraction(item: ConsoleModel?) {}
    override fun onListFragmentInteraction(item: Racer?) {}
    override fun onListFragmentInteraction(item: TrainingLap?) {}

    private lateinit var app: MyApp
    private var mAdView: AdView? = null
    private var mDecoderServiceBound = false

    override fun onStart() {
        super.onStart()
        Log.w(TAG, "Binding service")
        val intent = Intent(this, DecoderService::class.java)
        bindService(intent, decoderServiceConnection, Context.BIND_AUTO_CREATE)
    }

    override fun onStop() {
        super.onStop()
        unbindService(decoderServiceConnection)
        mDecoderServiceBound = false
    }

    override fun onPostResume() {
        super.onPostResume()

        app.firebaseAnalytics = FirebaseAnalytics.getInstance(this)
        MobileAds.initialize(this, "ca-app-pub-7655373768605194~7466307464")

        app.firestore = FirebaseFirestore.getInstance()
        val settings = FirebaseFirestoreSettings.Builder()
                .setTimestampsInSnapshotsEnabled(true)
                .setPersistenceEnabled(true)
                .build()
        app.firestore.firestoreSettings = settings

        val auth = FirebaseAuth.getInstance()
        app.drivers = DriversManager(app)

        auth.signInWithEmailAndPassword("skokys@gmail.com", "sfsadfhads8923jhkwdKJGJKHDKJl!")
                .addOnSuccessListener { result ->
                    Log.d(TAG, "Saved login $result")
                    app.user = auth.currentUser
                }.addOnFailureListener {
                    toast("Cloud login issue. Update and restart app")
                    finish()
                }

        nav_view.setNavigationItemSelectedListener { menuItem ->
            drawer_layout.closeDrawers()

            when (menuItem.itemId) {
                R.id.nav_training -> menuItem.isChecked = openTrainingMode(null)
                R.id.nav_racing -> menuItem.isChecked = openRacingMode(null)
                R.id.nav_console -> menuItem.isChecked = openConsoleMode(null)
                R.id.nav_drivers_editor -> menuItem.isChecked = openDriversEditor(null)
                R.id.nav_options -> menuItem.isChecked = openOptions(null)
                R.id.nav_connection_help -> menuItem.isChecked = openHelp(null)
                else -> {
                    menuItem.isChecked = false
                    Log.w(TAG, "Unknown mode $menuItem")
                }
            }
            true
        }

        mAdView = findViewById<View>(R.id.adView) as AdView?
        val adRequest = AdRequest.Builder().build()
        mAdView?.loadAd(adRequest)

        app.options["badmsg"] = defaultSharedPreferences.getBoolean("badmsg", true)
        app.options["driversync"] = defaultSharedPreferences.getBoolean("driversync", true)
        app.options["startupDelay"] = defaultSharedPreferences.getBoolean("startupDelay", false)
        app.options["startupDelayValue"] = defaultSharedPreferences.getInt("startupDelayValue", 3)

    }

    private var serviceBound: Boolean = false


    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        app = application as MyApp

        setContentView(R.layout.main)

        setSupportActionBar(toolbar)
        val actionbar: ActionBar? = supportActionBar
        actionbar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setHomeAsUpIndicator(R.drawable.ic_menu_black_24dp)
        }
    }

    override fun onBackPressed() {
        AlertDialog.Builder(this).setIcon(android.R.drawable.ic_dialog_alert).setTitle("Exit")
                .setMessage("Are you sure you want to exit?")
                .setPositiveButton("Yes") { _, _ ->
                    finish()
                }
                .setNegativeButton("No") { _, _ -> }.show()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        Log.d(TAG, "Focus $hasFocus")
        Tools.wakeLock(this, hasFocus)
    }

    fun openStartupFragment() {
        val fragmentTransaction = supportFragmentManager.beginTransaction()
        fragmentTransaction.replace(R.id.screen_container, StartupFragment())
        fragmentTransaction.commit()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    @Suppress("UNUSED_PARAMETER")
    fun connectOrDisconnect(view: View) {
        if (app.decoderService.isDecoderConnected()) {
            app.decoderService.disconnectAllDecoders()
        } else {
            app.decoderService.connectDecoderByUUID(firstDecoderId.tag as String)
        }
    }

    @Suppress("UNUSED_PARAMETER")
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

    @Suppress("UNUSED_PARAMETER")
    fun showMoreDecoders(view: View) {

        val dialog = Dialog(this)
        dialog.setContentView(R.layout.select_decoder)

        val decodersCopy = app.decoderService.getDecoders()

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
                if (!dd.isBlank()) getSharedPreferences(AMMC_PREFS, 0).edit().putString(LAST_IP, dd).commit()
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

    private lateinit var driversEditorFragment: DriversFragment
    fun openDriversEditor(view: View?): Boolean {
        driversEditorFragment = DriversFragment.newInstance(1)
        val fragmentTransaction = supportFragmentManager.beginTransaction()
        fragmentTransaction.replace(R.id.screen_container, driversEditorFragment)
        fragmentTransaction.commit()

        return true
    }

    private lateinit var optionsFragment: OptionsFragment
    private fun openOptions(view: View?): Boolean {
        optionsFragment = OptionsFragment.newInstance(1)
        val fragmentTransaction = supportFragmentManager.beginTransaction()
        fragmentTransaction.replace(R.id.screen_container, optionsFragment)
        fragmentTransaction.commit()

        return true
    }

    fun optionsDisableBadMsgReporting(view: View) {
        val c = view as CheckBox
        app.options["badmsg"] = c.isChecked
        defaultSharedPreferences.edit().putBoolean("badmsg", c.isChecked).apply()
    }

    fun optionsDriversSync(view: View) {
        val c = view as CheckBox
        app.options["driversync"] = c.isChecked
        defaultSharedPreferences.edit().putBoolean("driversync", c.isChecked).apply()
    }

    fun optionsRaceDuration(view: View) {
        val c = view as CheckBox
        saveStartupDelay(c)
        showHideStartupDelayValue(c.isChecked)
        saveStartupDelayValue(startupDelayValue)
    }

    fun optionsStartupDelay(view: View) {
        val c = view as CheckBox
        saveStartupDelay(c)
        showHideStartupDelayValue(c.isChecked)
        saveStartupDelayValue(startupDelayValue)
    }

    fun saveStartupDelay(checkbox: CheckBox) {
        app.options["startupDelay"] = checkbox.isChecked
        defaultSharedPreferences.edit().putBoolean("startupDelay", checkbox.isChecked).apply()
    }

    fun saveStartupDelayValue(delayText: EditText) {
        val delay = Integer.valueOf(delayText.text.toString())
        app.options["startupDelayValue"] = delay
        defaultSharedPreferences.edit().putInt("startupDelayValue", delay).apply()
    }

    fun showHideStartupDelayValue(show: Boolean) {
        if (show) {
            startupDelayValue.visibility = View.VISIBLE
            textStartupDelay2.visibility = View.VISIBLE
        } else {
            startupDelayValue.visibility = View.GONE
            textStartupDelay2.visibility = View.GONE
        }
    }

    private lateinit var consoleFragment: ConsoleModeFragment
    private lateinit var consoleVostokFragment: ConsoleModeVostokFragment
    fun openConsoleMode(view: View?): Boolean {
        app.decoderService?.let { ds ->
            if (!ds.isDecoderConnected()) {
                AlertDialog.Builder(this).setMessage(getString(R.string.decoder_not_connected))
                        .setCancelable(true).create().show()
                return true
            } else {
                return if (ds.isDecoderConnected()) {
                    val fragmentTransaction = supportFragmentManager.beginTransaction()
                    if (ds.isConnectedDecoderVostok()) {
                        consoleVostokFragment = ConsoleModeVostokFragment.newInstance(1)
                        fragmentTransaction.replace(R.id.screen_container, consoleVostokFragment)
                    } else {
                        consoleFragment = ConsoleModeFragment.newInstance(1)
                        fragmentTransaction.replace(R.id.screen_container, consoleFragment)
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
                fragmentTransaction.replace(R.id.screen_container, racingFragment as Fragment)
                fragmentTransaction.commit()
                true
            } else {
                AlertDialog.Builder(this).setMessage(getString(R.string.decoder_not_connected)).setCancelable(true).create().show()
                false
            }
        }
    }

    private lateinit var trainingFragment: TrainingModeFragment

    @Suppress("UNUSED_PARAMETER")
    fun openTrainingMode(view: View?): Boolean {

        return if (app.decoderService.isDecoderConnected()) {
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

    private val decoderServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName,
                                        service: IBinder) {

            val binder = service as DecoderService.MyLocalBinder
            app.decoderService = binder.getService()

            Log.w(TAG, "Decoder service bound")
            // FIXME connect last decoder Log.d(TAG, "Service -> " + it.connectDecoder("aDecoder"))
            mDecoderServiceBound = true

            openStartupFragment()
        }

        override fun onServiceDisconnected(name: ComponentName) {
            toast("Service disconnected? This is unexpected stop. Restart the app")
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

    companion object {
        private const val TAG = "MainActivity"
        private const val AMMC_PREFS = "ammcprefs"
        private const val LAST_IP = "lastip"

        fun decoderLabel(d: Decoder): String {

            if (d.decoderType != null && d.ipAddress != null) return "${d.decoderType} / ${d.ipAddress}"

            d.ipAddress?.let { return it }

            d.decoderId?.let { return it }

            return d.uuid.toString()
        }
    }
}