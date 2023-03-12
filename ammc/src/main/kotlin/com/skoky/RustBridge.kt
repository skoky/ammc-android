package com.skoky

class RustBridge {
    companion object {
        @JvmStatic
        private external fun p3_to_json(p3_bin: String): String
        @JvmStatic
        private external fun encode(json_str: String): String
    }

    fun p3_to_json_local(to: String): String {
        return p3_to_json(to)
    }

    fun encode_local(json_str: String): String {
        return encode(json_str)
    }



}