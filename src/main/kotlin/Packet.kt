import net.jpountz.lz4.LZ4Factory
import java.nio.ByteBuffer
import java.nio.ByteOrder

class Packet(
    val version: Int = 10,
    var cmd: Byte = 0,
    var sequence: Short = 0,
    var opcode: Short,
    var payload: ByteArray,
    private val lz4: LZ4Factory = LZ4Factory.fastestInstance()
) {
    constructor(
        version: Int = 10,
        cmd: Byte = 0,
        sequence: Short = 0,
        opcode: OpcodeEnum,
        payload: ByteArray,
        lz4: LZ4Factory = LZ4Factory.fastestInstance()
    ) : this(version, cmd, sequence, opcode.opcode, payload, lz4)

    val payloadLength: Int
        get() = payload.size

    fun getInfo(): String {
        return "cmd=$cmd, seq=$sequence, opcode=$opcode:${OpcodeEnum.nameOf(opcode)}"
    }

    fun toBytes(): ByteArray {
        val headerSize = 10

        var flags = 0
        var bodyBytes = payload
        if (payloadLength >= 32) {
            // compress
            val compressor = lz4.fastCompressor()
            val maxCompressed = compressor.maxCompressedLength(payloadLength)
            val compressed = ByteArray(maxCompressed)
            val compressedLen = compressor.compress(payload, 0, payloadLength, compressed, 0, maxCompressed)
            if (compressedLen >= 0) {
                flags = (payloadLength / compressedLen) + 1
                bodyBytes = compressed.copyOf(compressedLen)
            }
        }
        val length = bodyBytes.size
        val flagsAndLen = ((flags and 0xFF) shl 24) or (length and 0x00FFFFFF)

        val buf = ByteBuffer.allocate(headerSize + length).order(ByteOrder.BIG_ENDIAN)
        buf.put(version.toByte())
        buf.put(cmd)
        buf.putShort(sequence)
        buf.putShort(opcode)
        buf.putInt(flagsAndLen)
        if (length > 0) buf.put(bodyBytes)
        return buf.array()
    }

    companion object {
        fun fromBytes(input: ByteArray): Packet {
            if (input.size < 10) throw IllegalArgumentException("input too short")

            val bb = ByteBuffer.wrap(input).order(ByteOrder.BIG_ENDIAN)
            val version = bb.get().toInt() and 0xFF
            val command = bb.get()
            val sequence = bb.short
            val opcode = bb.short
            val flagsAndLen = bb.int

            val flags = (flagsAndLen ushr 24) and 0xFF
            val length = flagsAndLen and 0x00FFFFFF

            val headerBytes = 10
            if (input.size < headerBytes + length) {
                throw IllegalArgumentException("not enough bytes for body: need $length, have ${input.size - headerBytes}")
            }

            var payload = input.copyOfRange(headerBytes, headerBytes + length)

            if (flags != 0) {
                // decompress
                val compressedLen = length
                val destLen = compressedLen * flags
                val factory = LZ4Factory.fastestInstance()
                val safeDecompressor = factory.safeDecompressor()
                val dest = ByteArray(destLen)

                val decompressedSize = try {
                    safeDecompressor.decompress(payload, 0, compressedLen, dest, 0, destLen)
                } catch (ex: Exception) {
                    throw RuntimeException("LZ4 decompression failed: ${ex.message}", ex)
                }

                payload = dest.copyOf(decompressedSize)
            }

            return Packet(version, command, sequence, opcode, payload)
        }
    }
}
