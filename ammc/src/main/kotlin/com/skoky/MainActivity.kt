package com.skoky

import android.app.Dialog
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Bundle
import android.os.IBinder
import android.speech.tts.TextToSpeech
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import android.widget.Toast.LENGTH_LONG
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.internal.NavigationMenuItemView
import com.google.android.material.navigation.NavigationView
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.skoky.AmmcBridge.Companion.version
import com.skoky.Const.LAST_IP
import com.skoky.Const.badMsgK
import com.skoky.Const.defaultTimeToSpeechPattern
import com.skoky.Const.driversyncK
import com.skoky.Const.includeMinLapTimeK
import com.skoky.Const.minLapTimeK
import com.skoky.Const.raceDurationK
import com.skoky.Const.raceDurationValueK
import com.skoky.Const.startStopSoundK
import com.skoky.Const.startupDelayK
import com.skoky.Const.startupDelayValueK
import com.skoky.Const.timeToSpeech
import com.skoky.Const.timeToSpeechPattern
import com.skoky.Const.transponderSoundK
import com.skoky.fragment.ConsoleModeFragment
import com.skoky.fragment.ConsoleModeVostokFragment
import com.skoky.fragment.DriversFragment
import com.skoky.fragment.HelpFragment
import com.skoky.fragment.OptionsFragment
import com.skoky.fragment.RacingModeFragment
import com.skoky.fragment.StartupFragment
import com.skoky.fragment.TrainingModeFragment
import com.skoky.services.Decoder
import com.skoky.services.DecoderService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Locale

class MainActivity : AppCompatActivity() {


    private lateinit var app: MyApp

    private lateinit var prefs: DefaultPrefs

    //    private var mAdView: AdView? = null
    private var mDecoderServiceBound = false

    lateinit var toneGenerator: ToneGenerator
    lateinit var tts: TextToSpeech

    override fun onDestroy() {
        super.onDestroy()
        app.decoderService.disconnectAllDecoders()
        unbindService(decoderServiceConnection)
        mDecoderServiceBound = false
        tts.shutdown()
        toneGenerator.release()
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
        prefs = DefaultPrefs(applicationContext)

        val ammcVersion = version()
        Log.i(TAG, "AMMC lib version $ammcVersion")

        setContentView(R.layout.main)

        setSupportActionBar(findViewById(R.id.toolbar))
        val actionbar: ActionBar? = supportActionBar
        actionbar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setHomeAsUpIndicator(R.drawable.ic_menu_black_24dp)
        }
        val intent = Intent(this, DecoderService::class.java)
        bindService(intent, decoderServiceConnection, Context.BIND_AUTO_CREATE)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        lifecycleScope.launch(Dispatchers.IO) {
            app.firebaseAnalytics = FirebaseAnalytics.getInstance(app.applicationContext)

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

        navView.setNavigationItemSelectedListener { menuItem ->
            // (findViewById(R.id.menuItem) as MenuItem)
            findViewById<DrawerLayout>(R.id.drawer_layout).closeDrawers()

            when (menuItem.itemId) {
                R.id.nav_training -> menuItem.isChecked = switchFragment { openTrainingMode(null) }
                R.id.nav_racing -> menuItem.isChecked = switchFragment { openRacingMode(null) }
                R.id.nav_console -> menuItem.isChecked = switchFragment { openConsoleMode(null) }
                R.id.nav_drivers_editor -> menuItem.isChecked =
                    switchFragment { openDriversEditor(null) }

                R.id.nav_options -> menuItem.isChecked = switchFragment { openOptions(null) }
                R.id.nav_connection_help -> menuItem.isChecked = switchFragment { openHelp(null) }
                else -> {
                    menuItem.isChecked = false
                    Log.w(TAG, "Unknown mode $menuItem")
                }
            }
            true

        }

        lifecycleScope.launch(Dispatchers.IO) {
            tts = TextToSpeech(app, TextToSpeech.OnInitListener { status ->
                if (status != TextToSpeech.ERROR) {
                    try {  //if there is no error then set language
                        if (tts.availableLanguages.contains(Locale.getDefault()))
                            tts.language = Locale.getDefault()
                        else
                            tts.language = Locale.US
                    } catch (e: Exception) {
                        tts.language = Locale.US
                    }
                }
            })
        }

        toneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 100)

        app.badMsgReport = getBadMsgFlag()
    }


    private fun currentFragment() = supportFragmentManager.findFragmentById(R.id.nav_host_fragment)

    private fun isFragmentWithRaceOpen(): Boolean {
        return when (val fr = currentFragment()) {
            is TrainingModeFragment -> fr.isRaceRunning()
            is RacingModeFragment -> fr.isRaceRunning()
            else -> false
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        super.onBackPressed()
        if (currentFragment() is StartupFragment) {
            exitWithConfirm()
            app.decoderService.disconnectAllDecoders()
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
        fragmentTransaction.replace(R.id.nav_host_fragment, StartupFragment())
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
                intent.putExtra(
                    android.content.Intent.EXTRA_EMAIL,
                    arrayOf<String>("skokys@gmail.com")
                )
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

        val decoderAddressEt = dialog.findViewById<EditText>(R.id.decoder_address_edittext)

        decoderAddressEt.setText(prefs.getString(LAST_IP, ""))

        val kd = dialog.findViewById<RadioGroup>(R.id.known_decoders)

        decoderAddressEt.addTextChangedListener(SimpleTextWatcher(kd))

        decodersCopy.forEach { d2 ->
            if (d2.ipAddress != null) {
                val b = RadioButton(this)
                b.text = MainActivity.decoderLabel(d2)
                b.isChecked = false
                b.id = d2.hashCode()
                b.setOnCheckedChangeListener { view, checked ->
                    Log.d(TAG, "Checked $view $checked")
                    decoderAddressEt.text = null
                }
                kd.addView(b)
            }
        }

        val d = dialog.findViewById<Button>(R.id.decoder_select_ok_button)
        d.setOnClickListener {
            val checkDecoderHashCode = kd.checkedRadioButtonId
            val foundDecoderOpt =
                decodersCopy.find { decoder -> decoder.hashCode() == checkDecoderHashCode }
            Log.i(TAG, "decoder $foundDecoderOpt")

            if (decoderAddressEt.text.isNotEmpty()) {
                val decoderFromForm = decoderAddressEt.text.toString().trim()
                if (decoderFromForm.isNotBlank()) {
                    prefs.putString(LAST_IP, decoderFromForm)
                    app.decoderService.connectDecoder(decoderFromForm)
                }
            } else
                foundDecoderOpt?.let { foundDecoder -> app.decoderService.connectDecoderParsed(foundDecoder) }

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

    @Suppress("UNUSED_PARAMETER")
    private fun openHelp(view: View?): Boolean {
        helpFragment = HelpFragment.newInstance()
        val fragmentTransaction = supportFragmentManager.beginTransaction()
        fragmentTransaction.replace(R.id.nav_host_fragment, helpFragment)
        fragmentTransaction.commit()

        return true
    }

    private lateinit var driversEditorFragment: DriversFragment

    @Suppress("UNUSED_PARAMETER")
    fun openDriversEditor(view: View?) {
        driversEditorFragment = DriversFragment.newInstance()
        val fragmentTransaction = supportFragmentManager.beginTransaction()
        fragmentTransaction.replace(R.id.nav_host_fragment, driversEditorFragment)
        fragmentTransaction.commit()
        return
    }

    //    private val optionsFragment: OptionsFragment? = null
    @Suppress("UNUSED_PARAMETER")
    private fun openOptions(view: View?): Boolean {
        val optionsFragment = OptionsFragment.newInstance()
        val fragmentTransaction = supportFragmentManager.beginTransaction()
        fragmentTransaction.replace(R.id.nav_host_fragment, optionsFragment)
        fragmentTransaction.commit()

        return true
    }

    fun optionsDisableBadMsgReporting(view: View) {
        val c = view as CheckBox
        val reportBadMsg = c.isChecked
        prefs.putBoolean(badMsgK, reportBadMsg)
        app.badMsgReport = reportBadMsg
    }

    fun optionsDriversSync(view: View) {
        val c = view as CheckBox
        prefs.putBoolean(driversyncK, c.isChecked)
    }

    fun optionsTransponderSound(view: View) {
        val c = view as CheckBox
        prefs.putBoolean(transponderSoundK, c.isChecked)
    }

    fun optionsTimeToSpeech(view: View) {
        val c = view as CheckBox
        prefs.putBoolean(timeToSpeech, c.isChecked)
        showHideTimeToSpeechPattern(c.isChecked)
    }

    @Suppress("UNUSED_PARAMETER")
    fun testSpeech(view: View) {
        val toSay = Tools.timeToTextSpeech(23383, getTimeToSpeechPattern())
        Log.d(TrainingModeFragment.TAG, "To sayLastTime $toSay pattern ${getTimeToSpeechPattern()}")
        sayTimeText(toSay)
    }

    fun optionsStartStopSound(view: View) {
        val c = view as CheckBox
        prefs.putBoolean(startStopSoundK, c.isChecked)
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

    private fun saveStartupDelay(checkbox: CheckBox) {
        prefs.putBoolean(startupDelayK, checkbox.isChecked)
    }

    private fun saveRaceDuration(checkbox: CheckBox) {
        prefs.putBoolean(raceDurationK, checkbox.isChecked)
    }

    private fun saveIncludeMinLapTime(checkbox: CheckBox) {
        prefs.putBoolean(includeMinLapTimeK, checkbox.isChecked)
    }

    fun saveIntValue(delayText: EditText, key: String) = try {
        val delay = (Integer.valueOf(delayText.text.toString()))
        prefs.putInt(key, delay)
    } catch (e: Exception) {
        Log.e(TAG, "Value not saved! $key", e)
    }

    fun saveStringValue(text: EditText, key: String) = try {
        val t = text.text.toString()
        prefs.putString(key, t)
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
            findViewById<TextView>(R.id.textRaceDurationOption2).visibility = View.GONE
            findViewById<CheckBox>(R.id.checkIncludeMinLapTime).visibility = View.GONE
        }
    }

    fun showHideTimeToSpeechPattern(show: Boolean) {
        if (!show) {
            findViewById<TextView>(R.id.timeToSpeechPatternLabel).visibility = View.GONE
            findViewById<EditText>(R.id.timeToSpeech).visibility = View.GONE
            findViewById<Button>(R.id.timeToSpeechTestButton).visibility = View.GONE
        } else {
            findViewById<TextView>(R.id.timeToSpeechPatternLabel).visibility = View.VISIBLE
            findViewById<EditText>(R.id.timeToSpeech).visibility = View.VISIBLE
            findViewById<Button>(R.id.timeToSpeechTestButton).visibility = View.VISIBLE
        }
    }

    private lateinit var consoleFragment: ConsoleModeFragment
    private lateinit var consoleVostokFragment: ConsoleModeVostokFragment

    @Suppress("UNUSED_PARAMETER")
    fun openConsoleMode(view: View?) {
        app.decoderService.let { ds ->
            if (!ds.isDecoderConnected()) {
                AlertDialog.Builder(this).setMessage(getString(R.string.decoder_not_connected))
                    .setCancelable(true).create().show()
            } else {
                return if (ds.isDecoderConnected()) {
                    val fragmentTransaction = supportFragmentManager.beginTransaction()
                    if (ds.isConnectedDecoderVostok()) {
                        consoleVostokFragment = ConsoleModeVostokFragment.newInstance()
                        fragmentTransaction.replace(R.id.nav_host_fragment, consoleVostokFragment)
                    } else {
                        consoleFragment = ConsoleModeFragment.newInstance()
                        fragmentTransaction.replace(R.id.nav_host_fragment, consoleFragment)
                    }
                    fragmentTransaction.commit()
                    return

                } else {
                    AlertDialog.Builder(this).setMessage(getString(R.string.decoder_not_connected))
                        .setCancelable(true).create().show()
                }
            }
        }
    }

    @Suppress("UNUSED_PARAMETER")
    fun openTransponderDialog(view: View?) {
        if (!trainingFragment.trainingRunning) {
            trainingFragment.openTransponderDialog(false)
        }
    }

    private lateinit var racingFragment: RacingModeFragment

    @Suppress("UNUSED_PARAMETER")
    fun openRacingMode(view: View?) {
        app.decoderService.let {
            return if (it.isDecoderConnected()) {
                racingFragment = RacingModeFragment.newInstance()
                val fragmentTransaction = supportFragmentManager.beginTransaction()
                fragmentTransaction.replace(R.id.nav_host_fragment, racingFragment as Fragment)
                fragmentTransaction.commit()
                return
            } else {
                AlertDialog.Builder(this).setMessage(getString(R.string.decoder_not_connected))
                    .setCancelable(true).create().show()
            }
        }
    }

    private lateinit var trainingFragment: TrainingModeFragment

    @Suppress("UNUSED_PARAMETER")
    fun openTrainingMode(view: View?) {

        return if (app.decoderService.isDecoderConnected()) {
            trainingFragment = TrainingModeFragment.newInstance()
            val fragmentTransaction = supportFragmentManager.beginTransaction()
            fragmentTransaction.replace(R.id.nav_host_fragment, trainingFragment)
            fragmentTransaction.commit()
            return
        } else {
            AlertDialog.Builder(this).setMessage(getString(R.string.decoder_not_connected))
                .setCancelable(true).create().show()
        }

    }

    fun getBadMsgFlag() =
        prefs.getBoolean(badMsgK, false)

    fun getDriverSyncFlag() =
        prefs.getBoolean(driversyncK, true)

    fun getStartupDelayFlag() =
        prefs.getBoolean(startupDelayK, false)

    fun getStartupDelayValueFlag() =
        prefs.getInt(startupDelayValueK, 3)

    fun getRaceDurationFlag() =
        prefs.getBoolean(raceDurationK, false)

    fun getRaceDurationValueFlag() =
        prefs.getInt(raceDurationValueK, 5)

    fun getIncludeMinLapTimeFlag() =
        prefs.getBoolean(includeMinLapTimeK, false)

    fun getMinLapTimeFlag() =
        prefs.getInt(minLapTimeK, 20)

    fun getTransponderSoundFlag() =
        prefs.getBoolean(transponderSoundK, true)

    fun getStartStopSoundFlag() =
        prefs.getBoolean(startStopSoundK, true)

    fun getTimeToSpeechFlag() =
        prefs.getBoolean(timeToSpeech, true)

    fun getTimeToSpeechPattern() =
        prefs.getString(timeToSpeechPattern, defaultTimeToSpeechPattern)

    private val decoderServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(
            className: ComponentName,
            service: IBinder
        ) {

            val binder = service as DecoderService.MyLocalBinder
            app.decoderService = binder.getService()

            Log.w(TAG, "Decoder service bound")
            // FIXME connect last decoder Log.d(TAG, "Service -> " + it.connectDecoder("aDecoder"))
            mDecoderServiceBound = true

            openStartupFragment()
        }

        override fun onServiceDisconnected(name: ComponentName) {
            Toast.makeText(
                applicationContext,
                "Service disconnected? This is unexpected stop. Restart the app",
                LENGTH_LONG
            ).show()
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
                val nl = dl.findViewById<NavigationMenuItemView>(R.id.nav_version)
//                nl.setTitle("Version ${BuildConfig.VERSION_NAME}/${BuildConfig.VERSION_CODE}")
                dl.openDrawer(GravityCompat.START)
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    fun sayTimeText(toSay: String) {
        tts.speak(toSay, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    companion object {
        private const val TAG = "MainActivity"

        init {
            System.loadLibrary("ammc")
        }

        fun decoderLabel(d: Decoder): String {

            if (d.decoderType != null && d.ipAddress != null) return "${d.decoderType} / ${d.ipAddress}"

            d.ipAddress?.let { return it }

            d.decoderId?.let { return it }

            return d.uuid.toString()
        }
    }

}