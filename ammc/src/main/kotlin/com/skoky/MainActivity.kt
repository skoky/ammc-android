package com.skoky

import android.app.Dialog
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.support.design.widget.NavigationView
import android.support.v4.app.Fragment
import android.support.v4.view.GravityCompat
import android.support.v4.widget.DrawerLayout
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
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.skoky.Const.LAST_IP
import com.skoky.Const.badMsgK
import com.skoky.Const.driversyncK
import com.skoky.Const.includeMinLapTimeK
import com.skoky.Const.minLapTimeK
import com.skoky.Const.raceDurationK
import com.skoky.Const.raceDurationValueK
import com.skoky.Const.startStopSoundK
import com.skoky.Const.startupDelayK
import com.skoky.Const.startupDelayValueK
import com.skoky.Const.transponderSoundK
import com.skoky.fragment.*
import com.skoky.services.Decoder
import com.skoky.services.DecoderService
import org.jetbrains.anko.defaultSharedPreferences
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.toast


class MainActivity : AppCompatActivity() {

    private lateinit var app: MyApp
    //    private var mAdView: AdView? = null
    private var mDecoderServiceBound = false

    override fun onDestroy() {
        super.onDestroy()
        unbindService(decoderServiceConnection)
        mDecoderServiceBound = false
    }

    private fun switchFragment(toCallback: () -> Unit): Boolean {
        return if (isFragmentWithRaceOpen()) {
            switchFragmentWithConfirmation(toCallback)
        } else {
            toCallback()
            true
        }
    }

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        app = application as MyApp

        setContentView(R.layout.main)

        setSupportActionBar(findViewById(R.id.toolbar))
        val actionbar: ActionBar? = supportActionBar
        actionbar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setHomeAsUpIndicator(R.drawable.ic_menu_black_24dp)
        }
        val intent = Intent(this, DecoderService::class.java)
        bindService(intent, decoderServiceConnection, Context.BIND_AUTO_CREATE)


        doAsync {
            app.firebaseAnalytics = FirebaseAnalytics.getInstance(app.applicationContext)
            //MobileAds.initialize(app.applicationContext, "ca-app-pub-7655373768605194~7466307464")

            app.firestore = FirebaseFirestore.getInstance()
            val settings = FirebaseFirestoreSettings.Builder()
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
                        Log.w("Cloud login issue", it)
                    }
        }

        val navView = findViewById<NavigationView>(R.id.nav_view)

        navView.setNavigationItemSelectedListener {  menuItem ->
            // (findViewById(R.id.menuItem) as MenuItem)
            findViewById<DrawerLayout>(R.id.drawer_layout).closeDrawers()

            when (menuItem.itemId) {
                R.id.nav_training -> menuItem.isChecked = switchFragment { openTrainingMode(null) }
                R.id.nav_racing -> menuItem.isChecked = switchFragment { openRacingMode(null) }
                R.id.nav_console -> menuItem.isChecked = switchFragment { openConsoleMode(null) }
                R.id.nav_drivers_editor -> menuItem.isChecked = switchFragment { openDriversEditor(null) }
                R.id.nav_options -> menuItem.isChecked = switchFragment { openOptions(null) }
                R.id.nav_connection_help -> menuItem.isChecked = switchFragment { openHelp(null) }
                else -> {
                    menuItem.isChecked = false
                    Log.w(TAG, "Unknown mode $menuItem")
                }
            }
            true
        }

        app.badMsgReport = getBadMsgFlag()
    }


    private fun currentFragment() = supportFragmentManager.findFragmentById(R.id.screen_container)

    private fun confirmLeave(): Boolean {

        val fr = currentFragment()
        return when (fr) {
            is StartupFragment -> false
            is OptionsFragment -> false
            is TrainingModeFragment -> fr.isRaceRunning()
            is RacingModeFragment -> fr.isRaceRunning()
            is HelpFragment -> false
            is DriversFragment -> false
            else -> false
        }
    }

    private fun isFragmentWithRaceOpen(): Boolean {
        val fr = currentFragment()
        return when (fr) {
            is TrainingModeFragment -> fr.isRaceRunning()
            is RacingModeFragment -> fr.isRaceRunning()
            else -> false
        }
    }

    override fun onBackPressed() {
        if (currentFragment() is StartupFragment) {
            exitWithConfirm()
        } else {

            if (isFragmentWithRaceOpen())
                switchFragmentWithConfirmation { openStartupFragment() }
            else
                openStartupFragment()
        }
    }

    private fun exitWithConfirm() {
        AlertDialog.Builder(this).setIcon(android.R.drawable.ic_dialog_alert).setTitle("Exit")
                .setMessage(R.string.exit_message)
                .setPositiveButton(R.string.yes) { _, _ ->
                    finish()
                }
                .setNegativeButton(R.string.no) { _, _ -> }.show()
    }

    private fun switchFragmentWithConfirmation(next: () -> Unit): Boolean {

        AlertDialog.Builder(this).setIcon(android.R.drawable.ic_dialog_alert).setTitle("Exit")
                .setMessage(R.string.finish_message)
                .setPositiveButton(R.string.yes) { _, _ ->
                    next()
                }
                .setNegativeButton(R.string.no) { _, _ -> }.show()
        return true
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        Log.d(TAG, "Focus $hasFocus")
    }

    fun openStartupFragment() {
        val fragmentTransaction = supportFragmentManager.beginTransaction()
        //val optionsFragment { fragmentTransaction.detach(it) }
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
            app.decoderService.connectDecoderByUUID(findViewById<TextView>(R.id.firstDecoderId).tag as String)
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

        val et = dialog.findViewById<EditText>(R.id.decoder_address_edittext)
        et.setText(defaultSharedPreferences.getString(LAST_IP, ""))

        val kd = dialog.findViewById<RadioGroup>(R.id.known_decoders)

        et.addTextChangedListener(SimpleTextWatcher(kd))

        decodersCopy.forEach { d2 ->
            if (d2.ipAddress != null) {
                val b = RadioButton(this)
                b.text = MainActivity.decoderLabel(d2)
                b.isChecked = false
                b.id = d2.hashCode()
                b.setOnCheckedChangeListener { view, checked ->
                    Log.d(TAG, "Checked $view $checked")
                    et.text = null
                }
                kd.addView(b)
            }
        }

        val d = dialog.findViewById<Button>(R.id.decoder_select_ok_button)
        d.setOnClickListener {
            val checkDecoderHashCode = kd.checkedRadioButtonId
            val foundDecoder = decodersCopy.find { decoder -> decoder.hashCode() == checkDecoderHashCode }
            Log.i(TAG, "decoder $foundDecoder")

            if (et.text.isNotEmpty()) {
                val dd = et.text.toString().trim()
                if (!dd.isBlank()) defaultSharedPreferences.edit().putString(LAST_IP, dd).apply()
                app.decoderService.connectDecoder(dd)
            } else
                foundDecoder?.let { d3 -> app.decoderService.connectDecoder2(d3) }

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
        helpFragment = HelpFragment.newInstance()
        val fragmentTransaction = supportFragmentManager.beginTransaction()
        fragmentTransaction.replace(R.id.screen_container, helpFragment)
        fragmentTransaction.commit()

        return true
    }

    private lateinit var driversEditorFragment: DriversFragment
    fun openDriversEditor(view: View?): Boolean {
        driversEditorFragment = DriversFragment.newInstance()
        val fragmentTransaction = supportFragmentManager.beginTransaction()
        fragmentTransaction.replace(R.id.screen_container, driversEditorFragment)
        fragmentTransaction.commit()

        return true
    }

    //    private val optionsFragment: OptionsFragment? = null
    private fun openOptions(view: View?): Boolean {
        val optionsFragment = OptionsFragment.newInstance()
        val fragmentTransaction = supportFragmentManager.beginTransaction()
        fragmentTransaction.replace(R.id.screen_container, optionsFragment)
        fragmentTransaction.commit()

        return true
    }

    fun optionsDisableBadMsgReporting(view: View) {
        val c = view as CheckBox
        val reportBadMsg = c.isChecked
        defaultSharedPreferences.edit().putBoolean(badMsgK, reportBadMsg).apply()
        app.badMsgReport = reportBadMsg
    }

    fun optionsDriversSync(view: View) {
        val c = view as CheckBox
        defaultSharedPreferences.edit().putBoolean(driversyncK, c.isChecked).apply()
    }

    fun optionsTransponderSound(view: View) {
        val c = view as CheckBox
        defaultSharedPreferences.edit().putBoolean(transponderSoundK, c.isChecked).apply()
    }

    fun optionsStartStopSound(view: View) {
        val c = view as CheckBox
        defaultSharedPreferences.edit().putBoolean(startStopSoundK, c.isChecked).apply()
    }

    fun optionsRaceDuration(view: View) {
        val c = view as CheckBox
        saveRaceDuration(c)
        showHideRaceDurationValue(c.isChecked)
        val std = findViewById<EditText>(R.id.startupDelayValue)
        saveIntValue(std, raceDurationValueK)
    }

    fun optionsIncludeMinLapTime(view: View) {
        val c = view as CheckBox
        saveIncludeMinLapTime(c)
    }

    fun optionsStartupDelay(view: View) {
        val c = view as CheckBox
        saveStartupDelay(c)
        showHideStartupDelayValue(c.isChecked)
        val std = findViewById<EditText>(R.id.startupDelayValue)
        saveIntValue(std, startupDelayValueK)
    }

    private fun saveStartupDelay(checkbox: CheckBox) = defaultSharedPreferences.edit().putBoolean(startupDelayK, checkbox.isChecked).apply()
    private fun saveRaceDuration(checkbox: CheckBox) = defaultSharedPreferences.edit().putBoolean(raceDurationK, checkbox.isChecked).apply()
    private fun saveIncludeMinLapTime(checkbox: CheckBox) = defaultSharedPreferences.edit().putBoolean(includeMinLapTimeK, checkbox.isChecked).apply()

    fun saveIntValue(delayText: EditText, key: String) = try {
        val delay = (Integer.valueOf(delayText.text.toString()))
        defaultSharedPreferences.edit().putInt(key, delay).apply()
    } catch (e: Exception) {
        Log.e(TAG, "Value not saved! $key", e)
    }

    fun showHideStartupDelayValue(show: Boolean) {
        if (show) {
            findViewById<EditText>(R.id.startupDelayValue).visibility = View.VISIBLE
            findViewById<TextView>(R.id.textStartupDelay2).visibility = View.VISIBLE
        } else {
            findViewById<EditText>(R.id.startupDelayValue).visibility = View.INVISIBLE
            findViewById<TextView>(R.id.textStartupDelay2).visibility = View.INVISIBLE
        }
    }

    fun showHideRaceDurationValue(show: Boolean) {
        if (show) {
            findViewById<EditText>(R.id.raceDurationValue).visibility = View.VISIBLE
            findViewById<TextView>(R.id.textRaceDuration2).visibility = View.VISIBLE
            findViewById<TextView>(R.id.textRaceDurationOption2).visibility = View.VISIBLE
            findViewById<CheckBox>(R.id.checkIncludeMinLapTime).visibility = View.VISIBLE
        } else {
            findViewById<EditText>(R.id.raceDurationValue).visibility = View.INVISIBLE
            findViewById<TextView>(R.id.textRaceDuration2).visibility = View.INVISIBLE
            findViewById<TextView>(R.id.textRaceDurationOption2).visibility = View.INVISIBLE
            findViewById<CheckBox>(R.id.checkIncludeMinLapTime).visibility = View.INVISIBLE
        }

    }

    private lateinit var consoleFragment: ConsoleModeFragment
    private lateinit var consoleVostokFragment: ConsoleModeVostokFragment
    fun openConsoleMode(view: View?): Boolean {
        app.decoderService.let { ds ->
            if (!ds.isDecoderConnected()) {
                AlertDialog.Builder(this).setMessage(getString(R.string.decoder_not_connected))
                        .setCancelable(true).create().show()
                return true
            } else {
                return if (ds.isDecoderConnected()) {
                    val fragmentTransaction = supportFragmentManager.beginTransaction()
                    if (ds.isConnectedDecoderVostok()) {
                        consoleVostokFragment = ConsoleModeVostokFragment.newInstance()
                        fragmentTransaction.replace(R.id.screen_container, consoleVostokFragment)
                    } else {
                        consoleFragment = ConsoleModeFragment.newInstance()
                        fragmentTransaction.replace(R.id.screen_container, consoleFragment)
                    }
                    fragmentTransaction.commit()

                    true

                } else {
                    AlertDialog.Builder(this).setMessage(getString(R.string.decoder_not_connected)).setCancelable(true).create().show()
                    false
                }
            }
        }
    }

    fun openTransponderDialog(view: View?) {
        if (!trainingFragment.trainingRunning) {
            trainingFragment.openTransponderDialog(false)
        }
    }

    private lateinit var racingFragment: RacingModeFragment
    fun openRacingMode(view: View?): Boolean {
        app.decoderService.let {
            return if (it.isDecoderConnected()) {
                racingFragment = RacingModeFragment.newInstance()
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
            trainingFragment = TrainingModeFragment.newInstance()
            val fragmentTransaction = supportFragmentManager.beginTransaction()
            fragmentTransaction.replace(R.id.screen_container, trainingFragment)
            fragmentTransaction.commit()
            true
        } else {
            AlertDialog.Builder(this).setMessage(getString(R.string.decoder_not_connected)).setCancelable(true).create().show()
            false
        }

    }

    fun getBadMsgFlag() = defaultSharedPreferences.getBoolean(badMsgK, false)
    fun getDriverSyncFlag() = defaultSharedPreferences.getBoolean(driversyncK, true)
    fun getStartupDelayFlag() = defaultSharedPreferences.getBoolean(startupDelayK, false)
    fun getStartupDelayValueFlag() = defaultSharedPreferences.getInt(startupDelayValueK, 3)
    fun getRaceDurationFlag() = defaultSharedPreferences.getBoolean(raceDurationK, false)
    fun getRaceDurationValueFlag() = defaultSharedPreferences.getInt(raceDurationValueK, 5)
    fun getIncludeMinLapTimeFlag() = defaultSharedPreferences.getBoolean(includeMinLapTimeK, false)
    fun getMinLapTimeFlag() = defaultSharedPreferences.getInt(minLapTimeK, 20)
    fun getTransponderSoundFlag() = defaultSharedPreferences.getBoolean(transponderSoundK, true)
    fun getStartStopSoundFlag() = defaultSharedPreferences.getBoolean(startStopSoundK, true)

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
                if (isFragmentWithRaceOpen())
                    switchFragmentWithConfirmation { openStartupFragment() }
                else
                    openStartupFragment()
                true
            }
            android.R.id.home -> {
                val dl = findViewById<DrawerLayout>(R.id.drawer_layout)
                dl.openDrawer(GravityCompat.START)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

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