package com.skoky.watchdogs

import android.os.Handler
import android.os.Message
import android.util.Log

import com.skoky.MainActivity
import com.skoky.config.ConnTypeEnum

import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.Socket
import java.net.SocketException
import java.net.SocketTimeoutException
import java.net.UnknownHostException


class TcpIpWatchdogThread(var a: MainActivity, addressMaybeLong: String?, private val handler: Handler) : WatchdogCommon(), Watchdog {
    private val TAG = "TcpIpWatchdogThread"

    var connected = false
    var canRun = true
    private lateinit var address: String
    private var port = 5403
    private var os: OutputStream? = null

    init {

        if (addressMaybeLong != null) {
            if (addressMaybeLong.contains(":")) {
                val addressAndPort = addressMaybeLong.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                if (addressAndPort.size > 1) {
                    address = addressAndPort[0]
                    val portS = addressAndPort[1]
                    try {
                        port = Integer.parseInt(portS)
                    } catch (e: Exception) {
                    }

                }
            } else
                address = addressMaybeLong
        }
    }

    override fun getType(): ConnTypeEnum {
        return ConnTypeEnum.TCPIP
    }

    @Throws(IOException::class)
    private fun initConnection() {
        Log.d(TAG, "Connecting to $address:$port")
        s = Socket()
        val adr = InetSocketAddress(address, port)
        if (s != null) {
            Log.d(TAG, "Creating new connection")
            s!!.connect(adr, 3000)
            s!!.soTimeout = 30000
            Log.d(TAG, "Socket opened")
            connected = true
            // a.updateScreeForConnectionStatus(true);
        }
    }

    override fun run() {

        Log.d(TAG, "thread fired")

//        var buffer: Bytes?
//
//        var msg: Message
//        try {
//            msg = Message.obtain(handler, Watchdog.CONNECTING)
//            handler.dispatchMessage(msg)
//            if (s == null || s!!.isClosed) initConnection()
//            msg = Message.obtain(handler, Watchdog.CONNECTED)
//            handler.dispatchMessage(msg)
//            connected = true
//            val inputS = s!!.getInputStream()
//            os = s!!.getOutputStream()
//
//            while (canRun) {
//                buffer = readRecord(inputS)
//                if (buffer!!.size() == 0) {
//                    Log.w("W", "Buffer 0")
//                    break
//                }
//
//                if (MsgDetector.isComplete(buffer) != null) {  // TODO copy data from SOR to EOR
//                    Log.v(TAG, "Record pre-parse $buffer")
//                    msg = Message.obtain(handler, Watchdog.RECORD, buffer)
//                    handler.dispatchMessage(msg)
//                    // buffer.clear();
//                } else {
//                    Log.v(TAG, "Unrecognized data: $buffer")
//                }
//            }
//            Log.d(TAG, "Reading stopped")
//
//        } catch (ai: ArrayIndexOutOfBoundsException) {
//            Log.d(TAG, "AI $ai")
//        } catch (se: SocketException) {
//            Log.d(TAG, "Socket exception $se")
//            msg = Message.obtain(handler, Watchdog.CONNECTION_ERROR, "Unable to connectOrDisconnect")
//            if (canRun) handler.dispatchMessage(msg)
//        } catch (x: SocketTimeoutException) {
//            Log.d(TAG, "Disconnected")
//            msg = Message.obtain(handler, Watchdog.CONNECTION_TIMEOUT, "Connection timeout")
//            handler.dispatchMessage(msg)
//        } catch (uhe: UnknownHostException) {
//            msg = Message.obtain(handler, Watchdog.INVALID_ADDRESS)
//            handler.dispatchMessage(msg)
//        } catch (e: Exception) {
//            Log.e(TAG, "Unable to read connection", e)
//            msg = Message.obtain(handler, Watchdog.UNEXPECTED_ERROR, e.message)
//            handler.dispatchMessage(msg)
//        } finally {
//            msg = Message.obtain(handler, Watchdog.CONNECTION_CLOSED)
//            handler.dispatchMessage(msg)
//            connected = false
//            if (s != null)
//                try {
//                    s!!.close()
//                } catch (e: IOException) {
//                    Log.e(TAG, "Error closing socket")
//                } finally {
//                    s = null
//                }
//        }
    }

    override fun disconnect() {
        Log.d(TAG, "Stopping...")
        canRun = false
        if (s != null)
            try {
                s!!.close()
            } catch (e: IOException) {
                e.printStackTrace()  //To change body of catch statement use File | Settings | File Templates.
            }

        interrupt()
    }

    override fun send(msg: String): Boolean {

        if (os != null) {

            return try {
// FIXME                os!!.write(MsgProcessor().build(msg))
                true
            } catch (e: IOException) {
                Log.e(TAG, "Unable to send bytes. E:" + e.message)
                false
            }

        }
        return false
    }

    companion object {
        private var s: Socket? = null
        var lock = "I am the lock"
    }


}

