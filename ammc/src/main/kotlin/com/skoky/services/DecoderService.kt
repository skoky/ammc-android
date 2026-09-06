package com.skoky.services

import android.app.Service
import android.content.Intent
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Binder
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import com.skoky.*
import com.skoky.Tools.P3_DEF_PORT
import com.skoky.Tools.toHexString
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.lang.Thread.sleep
import java.net.*
import java.util.*
import kotlin.concurrent.schedule

const val VOSTOK_DEFAULT_IP = "10.10.100.254"
const val VOSTOK_DEFAULT_PORT = 8899

// The library encodes VERSION, NETWORKSETTINGS and PING only. Every other request
// type was dropped, and the decoder records answering them (settings, GPS, timeline,
// signals, loop triggers) are logged inside the library instead of being returned.
const val VERSION_REQUEST = """{"msg":"VERSION","empty_fields":["decoderType"]}"""
const val NETWORK_REQUEST = """{"msg":"NETWORKSETTINGS","empty_fields":["activeIPAddress"]}"""
const val PING_REQUEST = """{"msg":"PING"}"""

data class Decoder(
    val uuid: UUID, var decoderId: String? = null, var ipAddress: String? = null,
    var port: Int? = null,
    var decoderType: String? = null, var connection: Socket? = null, var lastSeen: Long
) {
    override fun equals(other: Any?): Boolean {
        return uuid == (other as? Decoder)?.uuid
    }

    override fun hashCode(): Int {
        return uuid.hashCode()
    }

    companion object {
        fun newDecoder(
            ipAddress: String? = null,
            port: Int? = null,
            decoderId: String? = null,
            decoderType: String? = null
        ): Decoder {
            val fixedPort = port ?: Tools.P3_DEF_PORT
            return Decoder(
                UUID.randomUUID(), ipAddress = ipAddress, port = fixedPort,
                decoderId = decoderId, decoderType = decoderType,
                lastSeen = System.currentTimeMillis()
            )
        }
    }
}

fun MutableList<Decoder>.addOrUpdate(decoder: Decoder) {

    var found = false

    this.forEach { d ->
        if (d.uuid == decoder.uuid) {
            if (decoder.decoderId != d.decoderId)
                Log.i("D", "DecodersX: decoder ID to update ${decoder.decoderId} -> ${d.decoderId}")
            decoder.decoderId?.let { d.decoderId = it }
            decoder.ipAddress?.let { d.ipAddress = it }
            decoder.port?.let { d.port = it }
            decoder.decoderType?.let { d.decoderType = it }
            decoder.connection?.let { d.connection = it }
            if (decoder.lastSeen > d.lastSeen) d.lastSeen = decoder.lastSeen
            found = true
        }
    }
    if (!found) {
        decoder.lastSeen = System.currentTimeMillis()
        this.add(decoder)
    }
}

class DecoderService : Service() {

    // Decoder(id = "1111", decoderType = "TranX", ipAddress = "10.0.0.10"))
    private var decoders = mutableListOf<Decoder>()

    private var keepAliveJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Created")

        decoders.sortedWith(compareBy({ it.uuid }, { it.uuid }))

        decoderAutoCleanup()

        CoroutineScope(Dispatchers.IO).launch {
            NetworkBroadcastHandler.receiveBroadcastData { processUdpMsg(it) }
        }

        CoroutineScope(Dispatchers.IO).launch {
            while (true) {

                val connectedDecoder = getConnectedDecoder()
                val alreadyHaveVostok = decoders.find { isVostok(it) }
                if (alreadyHaveVostok == null) {
                    if (connectedDecoder == null) {
                        Log.d(TAG, "Vostok default connecting....")
                        val validSocket = checkTcpSocket(VOSTOK_DEFAULT_IP, VOSTOK_DEFAULT_PORT)
                        Log.d(TAG, "Vostok socket $validSocket")
                        if (validSocket) {      // default vostok decoder found
                            val newDecoder = Decoder.newDecoder(
                                VOSTOK_DEFAULT_IP, VOSTOK_DEFAULT_PORT,
                                vostokDecoderId(VOSTOK_DEFAULT_IP, VOSTOK_DEFAULT_PORT),
                                VOSTOK_NAME
                            )
                            decoders.addOrUpdate(newDecoder)
                            sendBroadcastDecodersUpdate()
                        }
                    } else Log.d(TAG, "Vostok: Already connected another decoder")
                } else Log.d(TAG, "Already have vostok")
                sleep(5000)
            }
        }
    }

    private fun checkTcpSocket(ipaddress: String, port: Int): Boolean {
        Socket().use { socket ->
            return try {
                socket.connect(InetSocketAddress(ipaddress, port), 5000)
                socket.isConnected
            } catch (e: java.lang.Exception) {
                false
            }
        }
    }

    //    @RequiresApi(Build.VERSION_CODES.N)
    private fun decoderAutoCleanup() {
        Timer().schedule(1000, 1000) {
            // removes inactive decoders
            decoders.removeIf { d ->
                Log.i(TAG, "Decoder $d diff: ${System.currentTimeMillis() - d.lastSeen}")
                if ((System.currentTimeMillis() - d.lastSeen) > INACTIVE_DECODER_TIMEOUT) {
                    Log.i(TAG, "Removing decoder $d, current decoders $decoders")
                    if (d.connection != null) {
                        d.connection?.close()
                        sendBroadcastDisconnected(d)
                    }
                    sendBroadcastDecodersUpdate()
                    true
                } else {
                    false
                }
            }
        }
    }

    fun getBestFreeDecoder(): Decoder? = decoders.maxByOrNull { it.lastSeen }

    fun getDecoders() = decoders.toList()

    fun isDecoderConnected() = decoders.any { it.connection != null }


    fun getConnectedDecoder() =
        decoders.find { d -> d.connection != null && d.connection!!.isConnected }

    fun connectDecoderByUUID(decoderUUIDString: String) {
        val uuid = UUID.fromString(decoderUUIDString)
        val found = decoders.find { it.uuid == uuid }
        found?.let { connectDecoderParsed(it) }
    }

    fun connectDecoderParsed(decoder: Decoder, notifyError: Boolean = true) {

        if (decoder.connection == null || (!decoder.connection!!.isConnected)) {
            CoroutineScope(Dispatchers.IO).launch {
                val socket = Socket()
                try {
                    socket.connect(InetSocketAddress(decoder.ipAddress, decoder.port ?: 5403), 5000)

                    updateDecoder(decoder, socket)

                    Log.i(TAG, "Decoder $decoder connected")
                    sendBroadcastConnect(decoder)

                    CoroutineScope(Dispatchers.IO).launch {
                        listenOnSocketConnection(socket, decoder)
                    }

                    sendInitialVersionRequest(decoder, socket)
                    startKeepAlive(decoder, socket)
                } catch (e: Exception) {

                    socket.close()
                    if (notifyError) {
                        Log.e(TAG, "Error connecting decoder", e)
                        withContext(Dispatchers.Main) {
                            Toast.makeText(
                                applicationContext,
                                "Connection not possible to ${decoder.ipAddress}:${decoder.port}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            }

        } else {
            Log.e(TAG, "Decoder already connected $decoder")
        }
    }

    /**
     * Pings the decoder every second for as long as it stays connected.
     *
     * A silent socket is the problem: WiFi power saving puts the radio to sleep, the
     * decoder or an access point drops an idle TCP connection, and [decoderAutoCleanup]
     * discards a decoder that has said nothing for [INACTIVE_DECODER_TIMEOUT] even
     * though its socket is still open. The ping keeps traffic on the link both ways -
     * the decoder's answer is a normal message, so it also refreshes `lastSeen`.
     *
     * P3 decoders only; the Vostok text protocol has no ping.
     */
    private fun startKeepAlive(decoder: Decoder, socket: Socket) {
        stopKeepAlive()
        if (!isP3Decoder(decoder)) return

        val ping = AmmcBridge.encodeRequest(PING_REQUEST)
        if (ping == null) {
            Log.w(TAG, "Library did not encode a ping, keep-alive disabled")
            return
        }

        keepAliveJob = CoroutineScope(Dispatchers.IO).launch {
            Log.i(TAG, "Keep-alive ping started for $decoder")
            while (isActive && socket.isConnected && !socket.isClosed) {
                delay(KEEP_ALIVE_INTERVAL)
                try {
                    socket.getOutputStream().write(ping)
                } catch (e: Exception) {
                    // the read loop notices the same failure and reports the disconnect
                    Log.i(TAG, "Keep-alive ping failed, stopping: $e")
                    return@launch
                }
            }
            Log.i(TAG, "Keep-alive ping stopped")
        }
    }

    private fun stopKeepAlive() {
        keepAliveJob?.cancel()
        keepAliveJob = null
    }

    private fun sendInitialVersionRequest(decoder: Decoder, socket: Socket) {
        if (isP3Decoder(decoder)) {
            AmmcBridge.encodeRequest(VERSION_REQUEST)?.let {
                socket.getOutputStream().write(it)
            }
        }
    }

    private fun updateDecoder(decoder: Decoder, socket: Socket) {

        if (!isP3Decoder(decoder)) {
            decoders.addOrUpdate(
                decoder.copy(
                    decoderId = vostokDecoderId(
                        decoder.ipAddress,
                        decoder.port
                    ), connection = socket, lastSeen = System.currentTimeMillis()
                )
            )
        } else if (isP3Decoder(decoder)) {
            decoders.addOrUpdate(
                decoder.copy(
                    connection = socket,
                    lastSeen = System.currentTimeMillis()
                )
            )
        }
    }

    private fun isVostok(d: Decoder): Boolean {
        return d.decoderType == VOSTOK_NAME || d.decoderId == VOSTOK_NAME
    }

    fun isConnectedDecoderVostok(): Boolean {
        val connectedDecoder = getConnectedDecoder()
        connectedDecoder?.let {
            return isVostok(it)
        }
        return false
    }

    private fun isP3Decoder(d: Decoder): Boolean {
        return d.port == P3_DEF_PORT
    }

    // Console mode asks the decoder about itself. Only these two requests survive in
    // the library; the settings / GPS / timeline / signals / loop-trigger requests it
    // used to encode are gone, and the decoder's answers to them are no longer parsed.
    private val exploreMessages = listOf(VERSION_REQUEST, PING_REQUEST)

    fun exploreDecoder(uuid: UUID) {
        val socket = decoders.find { it.uuid == uuid }?.connection

        CoroutineScope(Dispatchers.IO).launch {
            socket?.let { s ->
                if (s.isConnected) {

                    exploreMessages.forEach { m ->
                        try {
                            AmmcBridge.encodeRequest(m)?.let { bytes ->
                                s.getOutputStream().write(bytes)
                            }
                        } finally {
                            sleep(200)
                        }
                    }
                }
            }
        }

    }

    private fun listenOnSocketConnection(socket: Socket, orgDecoder: Decoder) {
        val buffer = ByteArray(1024)
        var decoder = orgDecoder
        try {
            var read = 0
            while (socket.isConnected && read != -1) {
                socket.getInputStream()?.let {
                    read = it.read(buffer)
                    Log.i(TAG, "Received $read bytes")
                    if (read > 0) {
                        // one read can carry several records; the library returns them all
                        val messages = processTcpMsg(
                            buffer.copyOf(read),
                            vostokDecoderId(decoder.ipAddress, decoder.port)
                        )
                        Log.d(TAG, "JSON: $messages")
                        messages.forEach { json ->
                            if (json.optString("msg").isNotEmpty()) {
                                sendBroadcastData(decoder, json)
                            }
                            handleMessage(json, decoder, buffer)

                            json.optString("decoder_type").takeIf { it.isNotEmpty() }
                                ?.let { type ->
                                    decoders.addOrUpdate(decoder.copy(decoderType = type))
                                }
                        }
                        decoders.addOrUpdate(decoder.copy(lastSeen = System.currentTimeMillis()))
                        sendBroadcastDecodersUpdate()
                    }
                }
                decoders.find { it.uuid == decoder.uuid }?.let { decoder = it }
            }
            Log.i(TAG, "Connected ${socket.isConnected}, read $read")
        } catch (e: Exception) {
            Log.w(TAG, "Decoder connection error $decoder", e)
        } catch (t: Throwable) {
            Log.e(TAG, "Decoder connection throwable $decoder", t)
        } finally {
            stopKeepAlive()
            decoder.connection?.close()
            decoder.connection = null
            Log.i(TAG, "Decoder disconnected")
            sendBroadcastDisconnected(decoder)
            decoders.remove(orgDecoder)
        }
    }

    private fun handleMessage(json: JSONObject, decoder: Decoder, buffer: ByteArray) {
        // PASSING, VERSION, STATUS and PING are everything the library hands back; the
        // settings / GPS / timeline / signals / loop-trigger records it used to parse
        // are now logged inside the library and never reach us.
        when (json.optString("msg")) {
            "PASSING" -> {
                appendDriver(json)
                sendBroadcastPassing(json.toString())
            }

            "VERSION" -> {
                val decoderType = json.optString("decoder_type").takeIf { it.isNotEmpty() }
                decoders.addOrUpdate(decoder.copy(decoderType = decoderType))
                sendBroadcastData(decoder, json)
            }

            "STATUS" -> {
                if (json.optString("decoder_type") == VOSTOK_NAME) {
                    decoders.addOrUpdate(
                        decoder.copy(
                            lastSeen = System.currentTimeMillis(),
                            decoderType = VOSTOK_NAME
                        )
                    )
                } else {
                    decoders.addOrUpdate(decoder.copy(lastSeen = System.currentTimeMillis()))
                }
            }

            // the library answers a PING on its own; console mode just displays it
            "PING" -> {
            }

            // raised by us or by P98Parser, not by the decoder
            "Error" -> {
            }

            else -> {
                CloudDB.badMessageReport(
                    application as MyApp,
                    "tcp_unknown_data",
                    buffer.toHexString()
                )
                Log.w(TAG, "received unknown data $json")
            }
        }
    }

    private fun appendDriver(json: JSONObject) {

        val transponder = if (json.has("tran_code"))
            json.getString("tran_code")
        else if (json.has("transponder"))
            json.getString("transponder")
        else if (json.has("transponder_code"))
            json.getString("transponder_code")
        else if (json.has("driver_id"))
            json.getString("driver_id")
        else null

        transponder?.let {
            (application as MyApp).recentTransponders.add(it)
        }
    }

    private fun vostokDecoderId(ipAddress: String?, port: Int?): String? {
        if (ipAddress == null || port == null) return null
        return "$ipAddress:$port"
    }

    private fun processTcpMsg(
        msgImut: ByteArray,
        decoderIdVostok: String?
    ): List<JSONObject> {
        // some Vostok firmware prefixes a record with 0x01
        val msg = if (msgImut.isNotEmpty() && msgImut[0] == 0x01.toByte())
            msgImut.copyOfRange(1, msgImut.size)
        else msgImut

        return if (msg.size > 1 && msg[0] == 0x8e.toByte()) {
            val messages = AmmcBridge.parse(msg)
            if (messages.isEmpty()) {
                // a settings / GPS / timeline answer, which the library only logs
                Log.d(TAG, "No client message in P3 record ${msg.toHexString()}")
            }
            messages
        } else if (msg.size > 1 && (msg[0] == '@'.code.toByte() || msg[0] == '#'.code.toByte())) {
            listOf(JSONObject(P98Parser.parse(msg, decoderIdVostok ?: "-")))
        } else {
            Log.w(TAG, "Invalid msg on TCP " + msg.toHexString())
            CloudDB.badMessageReport(
                application as MyApp,
                "tcp_msg_error",
                msg.toHexString()
            )
            listOf(JSONObject("""{"msg":"Error","description":"Invalid message"}"""))
        }
    }

    private fun processUdpMsg(msgB: ByteArray) {
        Log.d(TAG, "Data received: ${msgB.size}")

        // A discovery reply carries only the decoder's address. It is no longer part of
        // the parsed message stream and has its own entry point in the library.
        AmmcBridge.parseNetworkResponse(msgB)?.let { network ->
            val decoderId = network.decoderId
            if (decoderId == null) {
                Log.w(TAG, "Discovery reply without decoder id, ignoring")
                return
            }
            val decoder = findOrCreateDecoder(decoderId)
            decoders.addOrUpdate(decoder.copy(ipAddress = network.ipAddress))
            sendBroadcastDecodersUpdate()
            return
        }

        val messages = AmmcBridge.parse(msgB)
        if (messages.isEmpty()) {
            // not worth a bad-message report: a broadcast the library only logs looks
            // exactly the same from here as a malformed one
            Log.d(TAG, "No client message in UDP record ${msgB.toHexString()}")
            return
        }

        messages.forEach { handleUdpMessage(it) }
        sendBroadcastDecodersUpdate()
    }

    private fun handleUdpMessage(json: JSONObject) {
        Log.d(TAG, ">> $json")

        val decoderId = json.optString("decoder_id").takeIf { it.isNotEmpty() }
        if (decoderId == null) {
            Log.w(TAG, "Received P3 message without decoder id, ignoring $json")
            return
        }

        val decoder = findOrCreateDecoder(decoderId)
        decoder.lastSeen = System.currentTimeMillis()

        when (json.optString("msg")) {
            "STATUS" -> {
                sendUdpNetworkRequest()
                sendUdpVersionRequest()
                sendBroadcastData(decoder, json)
                decoders.addOrUpdate(decoder)
            }

            "VERSION" ->
                json.optString("decoder_type").takeIf { it.isNotEmpty() }?.let { decoderType ->
                    decoders.addOrUpdate(decoder.copy(decoderType = decoderType))
                    sendBroadcastData(decoder, json)
                }

            "PASSING", "PING" -> sendBroadcastData(decoder, json)

            else -> Log.w(TAG, "Unexpected message on UDP $json")
        }
    }

    private fun findOrCreateDecoder(decoderId: String): Decoder {
        decoders.find { it.decoderId == decoderId }?.let { return it }
        val decoder = Decoder.newDecoder(decoderId = decoderId)
        decoders.addOrUpdate(decoder)
        return decoder
    }

    private fun sendUdpVersionRequest() {
        sendUdpBroadcastMessage(VERSION_REQUEST)
    }

    private fun sendUdpNetworkRequest() {
        sendUdpBroadcastMessage(NETWORK_REQUEST)
    }

    private fun sendUdpBroadcastMessage(msg: String) {
        val bytes = AmmcBridge.encodeRequest(msg) ?: return
        try {
            DatagramSocket(P3_DEF_PORT).use { socket ->
                socket.broadcast = true
                socket.connect(InetAddress.getByName("255.255.255.255"), P3_DEF_PORT)
                Log.d(TAG, "Bytes size ${bytes.size}")
                socket.send(DatagramPacket(bytes, bytes.size))
            }
        } catch (e: java.lang.Exception) {
            Log.w(TAG, "Error $e", e)
        }
    }

    private fun sendBroadcastDecodersUpdate() {
        val intent = Intent()
        intent.action = DECODERS_UPDATE
        applicationContext.sendBroadcast(intent)
        Log.d(TAG, "Broadcast sent $intent")
    }

    private fun sendBroadcastConnect(decoder: Decoder) {
        Tools.wakeLock(this, true)
        val intent = Intent()
        intent.action = DECODER_CONNECT
        intent.putExtra("uuid", decoder.uuid.toString())
        applicationContext.sendBroadcast(intent)
        Log.d(TAG, "Broadcast sent $intent")
    }

    private fun sendBroadcastDisconnected(decoder: Decoder) {
        Tools.wakeLock(this, false)
        val intent = Intent()
        intent.action = DECODER_DISCONNECTED
        intent.putExtra("uuid", decoder.uuid.toString())
        // TBD more data as it is not in decoders anymore
        applicationContext.sendBroadcast(intent)
        Log.d(TAG, "Broadcast sent $intent")
    }

    private fun sendBroadcastPassing(jsonData: String) {
        val intent = Intent()
        intent.action = DECODER_PASSING
        intent.putExtra("PASSING", jsonData)
        applicationContext.sendBroadcast(intent)
        Log.d(TAG, "Broadcast passing sent $intent")



        if (DefaultPrefs(applicationContext).getBoolean(
                Const.transponderSoundK,
                true
            )
        ) {
            ToneGenerator(
                AudioManager.STREAM_MUSIC,
                100
            ).startTone(ToneGenerator.TONE_CDMA_INTERCEPT, 200)
        }
    }

    private fun sendBroadcastData(decoder: Decoder?, jsonData: JSONObject) {
        //updateCache(jsonData)
        val intent = Intent()
        intent.action = DECODER_DATA
        intent.putExtra("Data", jsonData.toString())
        decoder?.let { intent.putExtra("uuid", it.toString()) }
        applicationContext.sendBroadcast(intent)
        Log.d(TAG, "Broadcast data sent $intent")
    }

    private val myBinder = MyLocalBinder()
    override fun onDestroy() {
        super.onDestroy()
        stopKeepAlive()
        Tools.wakeLock(this, false)
        Log.w(TAG, "Destroyed")
    }

    override fun onRebind(intent: Intent?) {
        super.onRebind(intent)
        Log.w(TAG, "rebind")
    }

    override fun onBind(intent: Intent): IBinder? {
        return myBinder
    }

    fun disconnectAllDecoders() {
        stopKeepAlive()
        getConnectedDecoder()?.connection?.let { c ->
            if (c.isConnected) {
                try {
                    c.close()
                } catch (e: Exception) {
                    Log.i(
                        TAG,
                        "Disconnection issue for decoder ${getConnectedDecoder()}, error $e"
                    )
                }
            }
            getConnectedDecoder()?.connection = null
        }
    }

    fun connectDecoder(address: String, notifyError: Boolean = true): Boolean {
        Log.d(TAG, "Connecting to $address $notifyError")

        var addressIp: String = ""
        var port: Int? = null

        val foundByIp: Decoder? =
            if (address.contains(":") && address.split(":").size == 2) {
                val fields = address.split(":")
                addressIp = fields[0]
                port = fields[1].toInt()
                decoders.find { it.ipAddress == addressIp && it.port == port }

            } else {
                decoders.find { it.ipAddress == address }
            }

        if (foundByIp != null) {
            connectDecoderParsed(foundByIp)
        } else {  // create new decoder
            //toast("Connecting to $addressIp:$port")
            if (port != null) {
                connectDecoderParsed(
                    Decoder.newDecoder(ipAddress = addressIp, port = port),
                    notifyError
                )
            } else {
                connectDecoderParsed(
                    Decoder.newDecoder(ipAddress = address),
                    notifyError
                )
            }
        }
        sendBroadcastDecodersUpdate()
        return true
    }

    inner class MyLocalBinder : Binder() {
        fun getService(): DecoderService {
            return this@DecoderService
        }
    }

    companion object {
        private const val TAG = "DecoderService"
        const val DECODERS_UPDATE = "com.skoky.decoder.broadcast.decoders_update"
        const val DECODER_CONNECT = "com.skoky.decoder.broadcast.connect"
        const val DECODER_DATA = "com.skoky.decoder.broadcast.data"
        const val DECODER_PASSING = "com.skoky.decoder.broadcast.passing"
        const val DECODER_DISCONNECTED = "com.skoky.decoder.broadcast.disconnected"
        private const val INACTIVE_DECODER_TIMEOUT: Long = 10000  // 10secs
        private const val KEEP_ALIVE_INTERVAL: Long = 1000  // 1sec
    }
}

