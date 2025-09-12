import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString
import java.util.Base64

object TestPackets {
    val packets: List<ByteArray> by lazy {
        val jsonString = {}::class.java.classLoader
            .getResourceAsStream("flows.json")
            ?.bufferedReader()
            ?.use { it.readText() }
            ?: throw IllegalStateException("flows.json not found in resources")

        val encoded: List<String> = Json.decodeFromString(jsonString)

        encoded.mapNotNull { base64 ->
            val bytes = try {
                Base64.getDecoder().decode(base64)
            } catch (e: IllegalArgumentException) {
                null // Игнорируем некорректный base64
            }

            bytes?.takeIf { b ->
                runCatching { Packet.fromBytes(b) }.isSuccess
            }
        }
    }
}
