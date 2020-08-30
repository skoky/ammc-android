package com.skoky;

public class CRC16 {


    private static final int poly = 0x1021; /* x16 + x12 + x5 + 1 generator polynomial */

    /* 0x8408 used in European X.25 */
    /**
     * scrambler lookup table for fast computation.
     */
    private static int[] crcTable = new int[256];

    static {
        // initialise scrambler table
        for (int i = 0; i < 256; i++) {
            int fcs = 0;
            int d = i << 8;
            for (int k = 0; k < 8; k++) {
                if (((fcs ^ d) & 0x8000) != 0) {
                    fcs = (fcs << 1) ^ poly;
                } else {
                    fcs = (fcs << 1);
                }
                d <<= 1;
                fcs &= 0xffff;
            }
            crcTable[i] = fcs;
        }
    }

    public static short cmpCRC(byte[] b) {
// loop, calculating CRC for each byte of the string
        short work = (short) 0xffff;
        for (byte value : b) {
            work = (short) ((crcTable[((work >> 8)) & 0xff] ^ (work << 8) ^ (value & 0xff)) & 0xffff);
        }
        return work;
    }
}
