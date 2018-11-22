import com.skoky.P98Parser
import org.junit.Test
import kotlin.test.assertEquals

class P98ParserTest {

//    @Test9
//    fun testVostokRealData() {
//        val bytes  = ByteArray(1, 35, 9, 49, 48, 49, 9, 57, 48, 48, 9, 48, 9, 120, 69, 65, 66, 66, 13, 10)
//    }

    @Test
    fun testsWork() {
        val bytes = "xxx".toByteArray()
        val json = P98Parser.parse(bytes,"xxx")
        assert(json.contains("Error"))
    }

// FIXME add 0x01 on the beginning for all tests
    @Test
    fun testsWork1() {
        val bytes1 = "#\t101\t2\t3\t4".toByteArray()
        val json = P98Parser.parse(bytes,"xxx")
        assertEquals("{\n" +
                "  \"recordType\": \"Status\",\n" +
                "  \"decoderType\": \"Vostok\",\n" +
                "  \"decoderId\": \"xxx\",\n" +
                "  \"num\": 2,\n" +
                "  \"noise\": 3,\n" +
                "  \"crcOk\": true\n" +
                "}", json)
    }

    @Test
    fun testsWork2() {
        val bytes = "@\t1\t2\t3\t4.1\t5\t6\t7\t8".toByteArray()
        val json = P98Parser.parse(bytes,"xxx")
        assertEquals("{\n" +
                "  \"recordType\": \"Passing\",\n" +
                "  \"decoderType\": \"\",\n" +
                "  \"decoderId\": \"xxx\",\n" +
                "  \"num\": 2,\n" +
                "  \"transponderCode\": \"3\",\n" +
                "  \"timeSinceStart\": 4.1,\n" +
                "  \"hitCounts\": 5,\n" +
                "  \"signalStrength\": 6,\n" +
                "  \"passingStatus\": 7,\n" +
                "  \"crcOk\": true,\n" +
                "  \"RTC_Time\": \"4100000\"\n" +
                "}", json)
    }

    @Test
    fun testsWork3() {
        val bytes = "@\t1\t2\t3\t4,1\t5\t6\t7\t8".toByteArray()
        val json = P98Parser.parse(bytes,"xxx")
        assertEquals("{\n" +
                "  \"recordType\": \"Error\",\n" +
                "  \"msg\": \"For input string: \\\"4,1\\\"\"\n" +
                "}", json)
    }

    @Test
    fun testsWork3err() {
        val bytes = "@\t1\txxx\tyyy\tddd\t5\t6\t7\t8".toByteArray()
        val json = P98Parser.parse(bytes,"xxx")
        assertEquals("{\n" +
                "  \"recordType\": \"Error\",\n" +
                "  \"msg\": \"For input string: \\\"ddd\\\"\"\n" +
                "}", json)
    }

    @Test
    fun testsWork4() {
        val bytes = "@\t101\t12\t3444425\t46.82\t61\t40\t2\txE934".toByteArray()
        val json = P98Parser.parse(bytes,"vostok123")
        assertEquals("{\n" +
                "  \"recordType\": \"Passing\",\n" +
                "  \"decoderType\": \"Vostok\",\n" +
                "  \"decoderId\": \"vostok123\",\n" +
                "  \"num\": 12,\n" +
                "  \"transponderCode\": \"3444425\",\n" +
                "  \"timeSinceStart\": 46.82,\n" +
                "  \"hitCounts\": 61,\n" +
                "  \"signalStrength\": 40,\n" +
                "  \"passingStatus\": 2,\n" +
                "  \"crcOk\": true,\n" +
                "  \"RTC_Time\": \"46820000\"\n" +
                "}", json)
    }
//
//    @Test
//    fun testCrc() {
//        val bytes = "@\t101\t12\t3444425\t46.82\t61\t40\t2\txE934".toByteArray()
//        val crc = P98Parser.calcCrc(bytes)
//        assertEquals(crc,Integer.parseInt("E934",16).toShort())
//    }
//
//    @Test
//    fun testCrcSpec() {
//        val bytes = "$\t40\t3\t5\t28800\txF9EB".toByteArray()
//        val crc = P98Parser.calcCrc(bytes)
//        assertEquals(crc,Integer.parseInt("F9EB",16).toShort())
//    }
}


//Testing data
//
//#	101	1378	0	x53D2
//@	101	1379	8110984	2434.835	190	40	2	x26C0
//#	101	1380	0	xF64A
//@	101	1381	8110984	2438.000	146	40	2	x3DD3
//#	101	1382	0	x9028
//@	101	1383	8110984	2441.156	187	40	2	xAA0C
//#	101	1384	0	x3A8E
//@	101	1385	8110984	2444.320	130	40	2	x7150
//@	101	1386	8110984	2447.476	159	40	2	xCEAF
//#	101	1387	0	x6FDD
//@	101	1388	8110984	2450.628	98	40	2	x4D06
//#	101	1389	0	x4CD2
//@	101	1390	8110984	2453.789	146	40	2	xEA66
//#	101	1391	0	xF24B
//@	101	1392	8110984	2456.960	191	40	2	x638E
//#	101	1393	0	x9429
//@	101	1394	8110984	2460.125	145	40	2	x0751
//@	101	1395	8110984	2463.277	176	40	2	x384A
//#	101	1396	0	x6BDC
//@	101	1397	8110984	2466.437	114	40	2	xA5C4