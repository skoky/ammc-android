package com.skoky.services

import android.content.Context
import android.util.Log
import com.skoky.CloudDB
import com.skoky.MyApp
import com.skoky.Tools.toHexString
import com.skoky.VOSTOK_NAME
import org.json.JSONObject


private const val TAG = "DecoderServiceMsgPrc"

val exploreMessages = listOf(
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

fun handleMessage(app: MyApp, context: Context, json: JSONObject, decoder: Decoder, rawMessage: ByteArray, decoders: MutableList<Decoder>) {
    when (json.get("msg").toString()) {
        "PASSING" -> {
            appendDriver(app, json)
            sendBroadcastPassing(json.toString(), app)
        }

        "VERSION" -> {
            val decoderType = json.get("decoder_type") as? String
            decoders.addOrUpdate(decoder.copy(decoderType = decoderType))
            sendBroadcastData(decoder, json, context)
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
                app,
                "tcp_unknown_data",
                rawMessage.toHexString()
            )
            Log.w(TAG, "received unknown data $json")
        }
    }

}


