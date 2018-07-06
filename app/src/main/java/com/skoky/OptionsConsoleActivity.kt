package com.skoky

import android.app.Activity
import android.app.AlertDialog
import android.app.ProgressDialog
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast

class OptionsConsoleActivity : Activity() {
    private var alert: AlertDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.console_options)
        val dialog = ProgressDialog.show(this, "", "Getting info...", false)
        getAMBSettings()
        dialog.dismiss()

    }

    private fun getAMBSettings() {

        val frequency: Short
        val duration: Short
        val holdOff: Short
        val aux: Boolean

        Log.d(TAG, "Signals sent:" + send("nic"))
        /*
        EditText t = (EditText) findViewById(R.id.beepFrequencyText);
        frequency = (short) Tools.textToInt(t);
        if (frequency == -1) t.setText("");

        t = (EditText) findViewById(R.id.beepDurationText);
        duration = (short) Tools.textToInt(t);
        if (frequency == -1) t.setText("");

        t = (EditText) findViewById(R.id.beepHoldOffText);
        holdOff = (short) Tools.textToInt(t);
        if (frequency == -1) t.setText("");

        CheckBox cb = (CheckBox) findViewById(R.id.auxiliaryOutputText);
        aux = cb.isChecked();

        boolean sent = send(new Signals(frequency, duration, holdOff, aux));
        if (sent) Toast.makeText(getApplicationContext(), "Beep setting sent",
                Toast.LENGTH_SHORT).show();
        else
            Toast.makeText(getApplicationContext(), "Send error",
                    Toast.LENGTH_SHORT).show();
  */
    }


    fun sendCommandAction(v: View) {
//          send command
//                val myIntent2 =  Intent(OptionsConsoleActivity, Command.class);
//                this.startActivity(myIntent2);
        createDialog()

    }

    fun sendGetInfoAction(v: View) {
//        send(Version())
//        send(Time())
//        send(GPS())
//        send(LoopTrigger())
//        send(NetworkSettings())
//        send(ServerSettings())
//        send(GeneralSettings())
//        send(Signals())
        Tools.toast(this, "Message to AMB sent. See console log for response")
    }

    private fun send(cmd: String): Boolean {
        val wb = (application as MyApp).wb
        return wb?.send(cmd) ?: false
    }

    private fun createDialog() {
        val items = arrayOf<CharSequence>("Reset", "Ping", "Clear passing")
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Send command to AMB")
        builder.setItems(items) { dialog, item ->
            var sent = false
            if (item == 0) {
                sent = send("nic")
                if (sent)
                    Toast.makeText(applicationContext, "Reset sent. AMB will disconnect",
                            Toast.LENGTH_SHORT).show()
            } else if (item == 1) {
                sent = send("Ping Hello AMB")
                if (sent)
                    Toast.makeText(applicationContext, "Ping sent",
                            Toast.LENGTH_SHORT).show()
            } else if (item == 2) {
                sent = send("nic = CleanPassings")
                if (sent)
                    Toast.makeText(applicationContext, "Passing will start from beginning",
                            Toast.LENGTH_SHORT).show()
            }
            if (!sent)
                Toast.makeText(applicationContext, "AMB disconnected",
                        Toast.LENGTH_SHORT).show()
        }
        alert = builder.create() as AlertDialog
        alert!!.show()
    }

    companion object {

        private const val TAG = "OptionsConsoleActivity"
    }


}
