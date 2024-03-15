package com.skoky.services

import android.util.Log
import com.skoky.Tools
import java.net.Socket
import java.util.UUID


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
