import com.skoky.P98Parser
import org.junit.Test
import kotlin.test.assertEquals

class P98ParserTest {

    @Test
    fun testsWork() {
        val bytes = "xxx".toByteArray()
        val json = P98Parser.parse(bytes,"xxx")
        assert(json.contains("Error"))
    }

    @Test
    fun testsWork1() {
        val bytes = "#\t101\t2\t3\t4".toByteArray()
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
                "  \"RTC_Time\": \"4100\"\n" +
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
                "  \"RTC_Time\": \"46820\"\n" +
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