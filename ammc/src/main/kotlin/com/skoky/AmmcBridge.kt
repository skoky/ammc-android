package com.skoky

import android.util.Log

class AmmcBridge {
    val TAG = "RustBridge"

    companion object {
        @JvmStatic
        external fun p3_to_json(p3_bin: String): String

        @JvmStatic
        external fun p3_network_to_json(p3_bin: String): String

        @JvmStatic
        external fun encode(json_str: String): String

        @JvmStatic
        external fun time_to_millis(json_str: String): String

        @JvmStatic
        external fun version(): String
    }

    fun p3_to_json_local(to: String): String {
        val jsonStr = p3_to_json(to)
        Log.i(TAG, "to_json: $to > $jsonStr <")
        return jsonStr
    }

    fun p3_network_to_json_local(to: String): String {
        val ip = p3_network_to_json(to)
        Log.i(TAG, "network ip: $to > $ip <")
        return ip
    }

    fun encode_local(json_str: String): String {
        val encoded = encode(json_str)
        Log.i(TAG, "from_json: $json_str > $encoded")
        return encoded
    }
}