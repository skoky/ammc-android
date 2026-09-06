package com.skoky

import android.util.Log
import com.skoky.Tools.decodeHex
import com.skoky.Tools.toHexString
import org.json.JSONArray
import org.json.JSONObject

/**
 * JNI surface of the ammc-rs shared library (libammc.so, built from crates/dynlib).
 *
 * Only the P3 binary protocol of the AMB / MyLaps decoder family goes through the
 * library. Vostok uses the text protocol and is parsed in-app by [P98Parser].
 *
 * The library returns JSON for PASSING, VERSION, STATUS and PING only; decoder
 * settings, GPS, timeline, signals and loop-trigger records are logged inside the
 * library and never reach the caller.
 */
object AmmcBridge {

    private const val TAG = "AmmcBridge"

    init {
        System.loadLibrary("ammc")
    }

    /** Decoder address as reported in a network-discovery reply. */
    data class NetworkInfo(val ipAddress: String, val decoderId: String?)

    /**
     * Parses hex-encoded P3 bytes into a JSON array of messages. Returns an empty
     * array for bytes the library does not turn into a client message.
     */
    @JvmStatic
    external fun p3_to_json(p3_bin: String): String

    /**
     * Parses a hex-encoded P3 network-discovery reply (TOR 0x16) into
     * `"<ip address>;<decoder id>"`. These bytes yield nothing from [p3_to_json].
     */
    @JvmStatic
    external fun p3_network_to_json(p3_bin: String): String

    /** Encodes a request message to hex-encoded P3 bytes. VERSION, NETWORKSETTINGS and PING only. */
    @JvmStatic
    external fun encode(json_str: String): String

    /** Converts a decoder RTC timestamp (`2023-03-19 09:29:13.682942`) to epoch millis. */
    @JvmStatic
    external fun time_to_millis(json_str: String): String

    /** ammc-rs library version. */
    @JvmStatic
    external fun version(): String

    /**
     * Parses P3 bytes into the messages the library recognises. A single read can
     * carry several records, so every one of them is returned.
     */
    fun parse(p3Bin: ByteArray): List<JSONObject> {
        val hex = p3Bin.toHexString()
        val json = p3_to_json(hex)
        Log.i(TAG, "p3_to_json: $hex > $json")
        return try {
            val array = JSONArray(json)
            (0 until array.length()).mapNotNull { array.optJSONObject(it) }
        } catch (e: Exception) {
            // the library reports malformed input as a plain string, not as JSON
            Log.w(TAG, "Unparseable response for $hex: $json")
            emptyList()
        }
    }

    /**
     * Parses a network-discovery reply, or returns null when these bytes are not one.
     */
    fun parseNetworkResponse(p3Bin: ByteArray): NetworkInfo? {
        val hex = p3Bin.toHexString()
        val response = p3_network_to_json(hex)
        Log.i(TAG, "p3_network_to_json: $hex > $response")
        val fields = response.split(";")
        if (fields.size != 2 || fields[0].isEmpty()) return null
        return NetworkInfo(fields[0], normalizeDecoderId(fields[1]))
    }

    /**
     * Encodes a request to raw P3 bytes, or returns null when the library rejects the
     * message. It answers an unsupported message type with an empty string and invalid
     * JSON with an error text, neither of which is hex.
     */
    fun encodeRequest(json: String): ByteArray? {
        val hex = encode(json)
        Log.i(TAG, "encode: $json > $hex")
        if (hex.isEmpty() || hex.length % 2 != 0 || !hex.all { it.isDigit() || it in 'a'..'f' || it in 'A'..'F' }) {
            Log.w(TAG, "Library did not encode $json: $hex")
            return null
        }
        return hex.decodeHex()
    }

    /**
     * A network reply carries the decoder id as four colon-separated bytes
     * (`00:0A:87:E5`); every other message carries it as `0A87E5`.
     */
    private fun normalizeDecoderId(raw: String): String? {
        val digits = raw.replace(":", "")
        if (digits.isEmpty()) return null
        return try {
            String.format("%06X", digits.toLong(16))
        } catch (e: NumberFormatException) {
            Log.w(TAG, "Unexpected decoder id $raw")
            null
        }
    }
}
