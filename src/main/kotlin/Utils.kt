import org.msgpack.core.MessagePack
import org.msgpack.core.MessagePacker
import org.msgpack.value.Value
import org.msgpack.value.ValueType
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer

object Utils {
    @JvmStatic
    fun packMapToBytes(map: Map<String, Any?>): ByteArray {
        val baos = ByteArrayOutputStream()
        MessagePack.newDefaultPacker(baos).use { packer ->
            packer.packMapHeader(map.size)
            for ((k, v) in map) {
                packer.packString(k)
                packValue(packer, v)
            }
        }
        return baos.toByteArray()
    }

    private fun packValue(p: MessagePacker, value: Any?) {
        when (value) {
            null -> p.packNil()

            is Boolean -> p.packBoolean(value)

            is String -> p.packString(value)
            is CharSequence -> p.packString(value.toString())

            is ByteArray -> {
                p.packBinaryHeader(value.size)
                p.writePayload(value)
            }
            is ByteBuffer -> {
                val remaining = value.remaining()
                val bytes = ByteArray(remaining)
                value.get(bytes)
                p.packBinaryHeader(remaining)
                p.writePayload(bytes)
            }

            is Int, is Short, is Byte -> {
                p.packInt((value as Number).toInt())
            }
            is Long -> p.packLong(value)
            is Float -> p.packFloat(value)
            is Double -> p.packDouble(value)

            is List<*> -> {
                p.packArrayHeader(value.size)
                for (it in value) packValue(p, it)
            }
            is Array<*> -> {
                p.packArrayHeader(value.size)
                for (it in value) packValue(p, it)
            }
            is Map<*, *> -> {
                p.packMapHeader(value.size)
                for ((rawK, rawV) in value.entries) {
                    val keyStr = rawK?.toString() ?: "null"
                    p.packString(keyStr)
                    packValue(p, rawV)
                }
            }

            is Number -> {
                // fallback (BigInteger, etc.)
                // MessagePack core supports long/double - fallback to string for unknown Number ranges
                when (value) {
                    is java.math.BigInteger -> p.packString(value.toString())
                    else -> {
                        // best-effort
                        val longVal = value.toLong()
                        if (longVal == value.toDouble().toLong()) p.packLong(longVal)
                        else p.packDouble(value.toDouble())
                    }
                }
            }
            
            else -> {
                p.packString(value.toString())
                println("WARNING: Unknown type $value")
            }
        }
    }

    fun unpackPayloadToMap(payload: ByteArray): Map<String, Any?> {
        val unpacker = MessagePack.newDefaultUnpacker(payload)
        val top = unpacker.unpackValue()
        if (top.valueType != ValueType.MAP) {
            return mapOf("value" to valueToObject(top))
        }
        val mv = top.asMapValue().map()
        val out = LinkedHashMap<String, Any?>()
        for ((k, v) in mv) {
            val key = if (k.valueType == ValueType.STRING) k.asStringValue().asString() else k.toString()
            out[key] = valueToObject(v)
        }
        return out
    }

    private fun valueToObject(v: Value): Any? {
        return when (v.valueType) {
            ValueType.NIL -> null
            ValueType.BOOLEAN -> v.asBooleanValue().boolean
            ValueType.INTEGER -> {
                val iv = v.asIntegerValue()
                try {
                    iv.toInt()
                } catch (ex: Exception) {
                    try {
                        iv.toLong()
                    } catch (e: Exception) {
                        iv.toBigInteger().toString()
                    }
                }
            }
            ValueType.FLOAT -> {
                val fv = v.asFloatValue()
                // MessagePack float may be double or float; return Double
                fv.toDouble()
            }
            ValueType.STRING -> v.asStringValue().asString()
            ValueType.BINARY -> v.asBinaryValue().asByteArray()
            ValueType.ARRAY -> {
                val list = ArrayList<Any?>()
                for (elem in v.asArrayValue().list()) list.add(valueToObject(elem))
                list
            }
            ValueType.MAP -> {
                val m = LinkedHashMap<String, Any?>()
                for ((kk, vv) in v.asMapValue().map()) {
                    val keyStr = if (kk.valueType == ValueType.STRING) kk.asStringValue().asString() else kk.toString()
                    m[keyStr] = valueToObject(vv)
                }
                m
            }
            else -> v.toString()
        }
    }

    fun formatValue(value: Any?): String {
        return when (value) {
            null -> "null"
            is Map<*, *> -> value.entries.joinToString(
                prefix = "{",
                postfix = "}",
                separator = ", "
            ) { (k, v) ->
                "$k=${formatValue(v)}"
            }
            is Collection<*> -> value.joinToString(
                prefix = "[",
                postfix = "]",
                separator = ", "
            ) { elem -> formatValue(elem) }
            else -> {
                val type = value::class.simpleName
                "$value:$type"
            }
        }
    }

    fun testPacket(packet: Packet, name: String = "Packet") {
        println("$name info: ${packet.getInfo()}")
        if (packet.payload.isNotEmpty()) {
            val map: Map<String, Any?> = unpackPayloadToMap(packet.payload)

            val formatted = map.entries.joinToString(
                prefix = "{",
                postfix = "}",
                separator = ", "
            ) { (key, value) ->
                val valueStr = value.toString().replace("\n", "\\n")
                "$key=${valueStr}"
            }

            println("$name payload map: $formatted")
        }
    }
}
