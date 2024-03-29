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
import com.skoky.Tools.decodeHex
import com.skoky.Tools.toHexString
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.lang.Thread.sleep
import java.net.*
import java.util.*
import kotlin.concurrent.schedule

const val VOSTOK_DEFAULT_IP = "10.10.100.254"
const val VOSTOK_DEFAULT_PORT = 8899

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

    private fun sendInitialVersionRequest(decoder: Decoder, socket: Socket) {
        if (isP3Decoder(decoder)) {
            val parser = AmmcBridge()
            val versionRequest =
                parser.encode_local("{\"msg\":\"Version\",\"empty_fields\":[\"decoderType\"]}")

            socket.getOutputStream().write(versionRequest.decodeHex())
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

    private val exploreMessages = listOf(
        "{\"msg\":\"Version\",\"empty_fields\":[\"decoderType\"]}",
//        "{\"recordType\":\"Status\",\"emptyFields\":[\"loopTriggers\",\"noise\",\"gps\", \"temperature\",\"inputVoltage\",\"satInUse\"],\"VERSION\":\"2\"}",
//        "{\"recordType\":\"AuxiliarySettings\",\"emptyFields\":[\"photocellHoldOff\",\"externalStartHoldOff\",\"syncHoldOff\"],\"VERSION\":\"2\"}",
//        "{\"recordType\":\"GeneralSettings\",\"emptyFields\":[\"statusInterval\",\"realTimeClock\",\"enableFirstContactRecord\",\"decoderMode\"],\"VERSION\":\"2\"}",
//        "{\"recordType\":\"GPS\",\"emptyFields\":[\"longtitude\",\"latitude\",\"numOfSatInUse\"],\"VERSION\":\"2\"}",
//        "{\"recordType\":\"LoopTrigger\",\"emptyFields\":[\"flags\",\"pingCount\",\"temperature\",\"strength\",\"code\",\"lastReceivedPingRtcTime\",\"lastReceivedPingUtcTime\",\"actStrength\",\"recordIndex\"],\"VERSION\":\"2\"}",
//        "{\"recordType\":\"NetworkSettings\",\"emptyFields\":[\"automatic\",\"staticSubnetMask\",\"obtained\",\"activeIPAddress\",\"activeDNS\",\"activeGateway\",\"staticDNSServer\",\"activeSubNetMask\",\"activate\",\"interfaceNumber\",\"staticIpAddress\",\"staticGateway\"],\"VERSION\":\"2\"}",
//        "{\"recordType\":\"ServerSettings\",\"emptyFields\":[\"host\",\"ipPort\",\"interfaceName\"],\"VERSION\":\"2\"}",
//        "{\"recordType\":\"Signals\",\"emptyFields\":[\"beepFrequency\",\"beepDuration\",\"beepHoldOff\",\"auxiliaryOutput\"],\"VERSION\":\"2\"}",
//        "{\"recordType\":\"Time\",\"emptyFields\":[\"RTC_Time\",\"UTC_Time\",\"flags\"],\"VERSION\":\"2\"}",
//        "{\"recordType\":\"Timeline\",\"emptyFields\":[\"gateTime\",\"ID\",\"name\",\"sports\",\"loopTriggerEnabled\",\"minOutField\",\"squelch\"],\"VERSION\":\"2\"}",
//        "{\"recordType\":\"Version\",\"emptyFields\":[\"description\",\"options\",\"version\",\"decoderType\",\"release\",\"registration\",\"buildNumber\"],\"VERSION\":\"2\"}"
    )


    fun exploreDecoder(uuid: UUID) {
        val socket = decoders.find { it.uuid == uuid }?.connection

        CoroutineScope(Dispatchers.IO).launch {
            socket?.let { s ->
                if (s.isConnected) {

                    exploreMessages.forEach { m ->
                        try {
                            val parser = AmmcBridge()
                            val parsed = parser.encode_local(m)
                            parsed.let { p ->
                                if (p.isNotEmpty()) {
                                    s.getOutputStream().write(p.decodeHex())
                                }
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
                        val json = processTcpMsg(
                            buffer.copyOf(read),
                            vostokDecoderId(decoder.ipAddress, decoder.port)
                        )
                        Log.d(TAG, "JSON: $json")
                        if (json.get("msg").toString().isNotEmpty()) sendBroadcastData(
                            decoder,
                            json
                        )
                        handleMessage(json, decoder, buffer)

                        if (json.has("decoder_type")) {
                            json.getString("decoder_type")
                                .let { type ->
                                    decoders.addOrUpdate(
                                        decoder.copy(
                                            decoderType = type
                                        )
                                    )
                                }
                        } else {
                            if (json.has("decoder_type")) {
                                json.getString("decoder_type")
                                    .let { type ->
                                        decoders.addOrUpdate(
                                            decoder.copy(
                                                decoderType = type
                                            )
                                        )
                                    }
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
            decoder.connection?.close()
            decoder.connection = null
            Log.i(TAG, "Decoder disconnected")
            sendBroadcastDisconnected(decoder)
            decoders.remove(orgDecoder)
        }
    }

    private fun handleMessage(json: JSONObject, decoder: Decoder, buffer: ByteArray) {
        when (json.get("msg").toString()) {
            "PASSING" -> {
                appendDriver(json)
                sendBroadcastPassing(json.toString())
            }

            "VERSION" -> {
                val decoderType = json.get("decoder_type") as? String
                decoders.addOrUpdate(decoder.copy(decoderType = decoderType))
                sendBroadcastData(decoder, json)
            }

            "STATUS" -> {
                if (json.has("decoder_type") && json.get("decoder_type") == VOSTOK_NAME) {
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

            "NETWORKSETTINGS" -> {
            }

            "AuxiliarySettings" -> {
            }

            "ServerSettings" -> {
            }

            "Timeline" -> {
            }

            "Signals" -> {
            }

            "LoopTrigger" -> {
            }

            "GPS" -> {
            }

            "ERROR" -> {
            }

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
    ): JSONObject {
        val parser = AmmcBridge()
        var msg2 = msgImut.toMutableList()
        if (msg2[0] == 0x01.toByte()) {
            val m = msg2.toMutableList()
            m.removeAt(0)
            msg2 = m.toMutableList()
        }
        val msg = msg2.toByteArray()
        return if (msg.size > 1 && msg[0] == 0x8e.toByte()) {
            val responses = JSONArray(parser.p3_to_json_local(msg.toHexString()))
            Log.i(TAG, "response $responses")
            if (responses.length() > 0) {
                return responses.get(0) as JSONObject // FIXME get all messages
            }
            JSONObject("{\"msg\":\"Error\",\"description\":\"No message\"}")
        } else if (msg.size > 1 && (msg[0] == '@'.code.toByte() || msg[0] == '#'.code.toByte())) {
            JSONObject(P98Parser.parse(msg, decoderIdVostok ?: "-"))
        } else {
            Log.w(TAG, "Invalid msg on TCP " + msg.toHexString())
            Log.w(TAG, "Invalid msg on TCP $msg")
            CloudDB.badMessageReport(
                application as MyApp,
                "tcp_msg_error",
                msg.toHexString()
            )
            JSONObject("{\"msg\":\"Error\",\"description\":\"Invalid message\"}")
        }
    }

    private fun processUdpMsg(msgB: ByteArray) {
        Log.d(TAG, "Data received: ${msgB.size}")
        val parser = AmmcBridge()
        val msg = parser.p3_to_json_local(msgB.toHexString())
        Log.i(TAG, "HEX String {msg}")
        val json = JSONObject(msg)
        Log.d(TAG, ">> $json")

        if (msg.contains("Error")) {
            CloudDB.badMessageReport(
                application as MyApp,
                "tcp_msg_with_error",
                msgB.toHexString()
            )
        }

        val decoderId: String?
        if (json.has("decoder_id")) {
            decoderId = json.get("decoder_id") as String
        } else {
            Log.w(TAG, "Received P3 message without decoderId. Wired! $json")
            return
        }

        var decoder = decoders.find { it.decoderId == decoderId }
        if (decoder == null) {
            decoder = Decoder.newDecoder(decoderId = decoderId)
            decoders.addOrUpdate(decoder)
        }

        decoder.lastSeen = System.currentTimeMillis()

        decoder.let { d ->

            if (json.has("msg")) when (json.get("msg")) {
                "STATUS" -> {
                    sendUdpNetworkRequest()
                    sendUdpVersionRequest()
                    sendBroadcastData(d, json)
                    decoders.addOrUpdate(decoder)
                }

                "NETWORKSETTINGS" ->
                    if (json.has("activeIPAddress")) {  // FIXME naming?
                        val ipAddress = json.get("activeIPAddress") as? String
                        decoders.addOrUpdate(decoder.copy(ipAddress = ipAddress))
                        sendBroadcastData(d, json)
                    }

                "VERSION" ->
                    if (json.has("decoder_type")) {
                        val decoderType = json.get("decoder_type") as? String
                        decoders.addOrUpdate(decoder.copy(decoderType = decoderType))
                        sendBroadcastData(d, json)
                    }

                "ERROR" ->
                    CloudDB.badMessageReport(
                        application as MyApp,
                        "tcp_error",
                        msgB.toHexString()
                    )

            } else {
                Log.w(TAG, "Msg with record type on UDP. Wired! $json")
            }
            sendBroadcastDecodersUpdate()
        }
    }

    private fun sendUdpVersionRequest() {
        sendUdpBroadcastMessage("{\"msg\":\"VERSION\",\"empty_fields\":[\"decoderType\"]}")
    }

    private fun sendUdpNetworkRequest() {
        sendUdpBroadcastMessage("{\"msg\":\"NETWORKSETTINGS\",\"empty_fields\":[\"activeIPAddress\"]}")
    }

    private fun sendUdpBroadcastMessage(msg: String) {
        try {
            DatagramSocket(P3_DEF_PORT).use { socket ->
                socket.broadcast = true
                socket.connect(InetAddress.getByName("255.255.255.255"), P3_DEF_PORT)
                val parser = AmmcBridge()
                val bytes = parser.encode_local(msg)
                val bytes2 = bytes.decodeHex()
                Log.d(TAG, "Bytes size ${bytes2.size}")
                socket.send(DatagramPacket(bytes2, bytes2.size))
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
    }
}

