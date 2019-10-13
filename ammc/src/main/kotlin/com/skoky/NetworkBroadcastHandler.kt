package com.skoky

import android.util.Log
import java.lang.Thread.sleep
import java.net.DatagramPacket
import java.net.DatagramSocket


object NetworkBroadcastHandler {

    private const val TAG = "NetworkBroadcastHandler"
    private const val INCOMING_PORT = 5303

    fun receiveBroadcastData(handler: (ByteArray) -> Unit) {

        Log.w(TAG, "Starting broadcast listener")
        val incomingBuffer = ByteArray(1024)
        var socket: DatagramSocket? = null
        while (true) {
            sleep(2000)
            socket = try {
                DatagramSocket(INCOMING_PORT)
            } catch (e: Exception) {
                Log.i(TAG, "Unable to listen in port $INCOMING_PORT, error $e")
                socket?.close()
                return
            }
            socket.let { s ->

                try {
                    while (!s.isClosed) {
                        val incomingPacket = DatagramPacket(incomingBuffer, incomingBuffer.size)
                        s.receive(incomingPacket)
                        val data = incomingPacket.data

                        data?.let {
                            handler(it.copyOf(incomingPacket.length))
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Broadcast socket closed $e")
                    socket?.close()
                    socket = null

                } finally {
                    s.let { s.close() }
                    sleep(1000)
                    Log.i(TAG, "Reconnecting broadcast port $INCOMING_PORT")
                }
            }
        }
    }

}
