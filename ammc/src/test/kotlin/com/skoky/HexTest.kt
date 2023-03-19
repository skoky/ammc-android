package com.skoky

import com.skoky.Tools.toHexString
import org.junit.Test
import kotlin.test.assertEquals

class HexTest {

    @Test
    fun bytesToString() {
        val b = byteArrayOf(0xA, 0x2, 0x79, 0x00, 0x79, 0x23)
        assertEquals("0A0279007923", b.toHexString().uppercase())
    }
}
