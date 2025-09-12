import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.DynamicNode
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.DynamicContainer

class PacketTest {

    private fun toHex(b: ByteArray): String =
        b.joinToString("") { String.format("%02x", it.toInt() and 0xFF) }

    private fun createPacketFrom(tp: ByteArray): Packet = Packet.fromBytes(tp)

    @TestFactory
    fun packetTestSuites(): Collection<DynamicNode> {
        return TestPackets.packets.mapIndexed { index, tp ->
            val tests = mutableListOf<DynamicNode>()

            // 1) fromBytes -> not null, info present
            tests += DynamicTest.dynamicTest("fromBytes -> non-null & info present") {
                val packet = try {
                    createPacketFrom(tp)
                } catch (ex: Exception) {
                    fail<Nothing>("[$index]: fromBytes threw exception: ${ex.message}", ex)
                }
                assertNotNull(packet, "[$index]: Packet must not be null")
                val info = try {
                    packet.getInfo()
                } catch (ex: Exception) {
                    fail<Nothing>("[$index]: getInfo() threw exception: ${ex.message}", ex)
                }
                assertNotNull(info, "[$index]: getInfo() must not return null")
                assertTrue(info.isNotBlank(), "[$index]: getInfo() should not be blank")
            }

            // 2) toBytes should equal original bytes
//            tests += DynamicTest.dynamicTest("toBytes -> serialized equals original (len=${tp.size})") {
//                val packet = try {
//                    createPacketFrom(tp)
//                } catch (ex: Exception) {
//                    fail<Nothing>("[$index]: fromBytes threw exception: ${ex.message}", ex)
//                }
//                val serialized = try {
//                    packet.toBytes()
//                } catch (ex: Exception) {
//                    fail<Nothing>("[$index]: toBytes() threw exception: ${ex.message}", ex)
//                }
//                assertArrayEquals(tp, serialized, "[$index]: Serialized bytes must equal original input bytes\noriginal=${toHex(tp)}\nserialized=${toHex(serialized)}")
//            }

            // 3) from->to->from preserves bytes & payload
            tests += DynamicTest.dynamicTest("from->to->from preserves bytes & payload") {
                val packet = try {
                    createPacketFrom(tp)
                } catch (ex: Exception) {
                    fail<Nothing>("[$index]: fromBytes threw exception: ${ex.message}", ex)
                }
                val serialized = packet.toBytes()
                val newPacket = try {
                    Packet.fromBytes(serialized)
                } catch (ex: Exception) {
                    fail<Nothing>("[$index]: Packet.fromBytes(serialized) threw: ${ex.message}", ex)
                }
                val reserialized = newPacket.toBytes()
                assertArrayEquals(serialized, reserialized, "[$index]: Re-serialized bytes should equal original serialized bytes")
                assertArrayEquals(packet.payload, newPacket.payload, "[$index]: Payloads must be equal after roundtrip")
            }

            // 4) payload map unpack/pack roundtrip — только если payload не пустой
//            if (tp.isNotEmpty()) {
//                val possiblePayload = try {
//                    createPacketFrom(tp).payload
//                } catch (ex: Exception) {
//                    null
//                }
//
//                if (possiblePayload != null && possiblePayload.isNotEmpty()) {
//                    tests += DynamicTest.dynamicTest("payload map unpack/pack roundtrip (payload len=${possiblePayload.size})") {
//                        val payload = possiblePayload
//                        val map: Map<String, Any?> = try {
//                            Utils.unpackPayloadToMap(payload)
//                        } catch (ex: Exception) {
//                            fail<Nothing>("[$index]: Utils.unpackPayloadToMap threw: ${ex.message}", ex)
//                        }
//
//                        if (map.isEmpty()) {
//                            org.junit.jupiter.api.Assumptions.assumeTrue(false, "[$index]: payload is not a map or is empty — skipping map roundtrip checks")
//                        }
//
//                        assertTrue(map.isNotEmpty(), "[$index]: Unpacked map should not be empty (expecting a map payload)")
//
//                        val packedBytes = try {
//                            Utils.packMapToBytes(map)
//                        } catch (ex: Exception) {
//                            fail<Nothing>("[$index]: Utils.packMapToBytes threw: ${ex.message}", ex)
//                        }
//
//                        assertArrayEquals(payload, packedBytes, "[$index]: Re-packed payload bytes should equal original packet.payload")
//                        val mapAfter: Map<String, Any?> = try {
//                            Utils.unpackPayloadToMap(packedBytes)
//                        } catch (ex: Exception) {
//                            fail<Nothing>("[$index]: Utils.unpackPayloadToMap (after packing) threw: ${ex.message}", ex)
//                        }
//                        assertEquals(map, mapAfter, "[$index]: Map must be equal after pack->unpack roundtrip")
//                    }
//                } else {
//                    tests += DynamicTest.dynamicTest("payload is empty — skipping map roundtrip") {
//                        org.junit.jupiter.api.Assumptions.assumeTrue(false, "[$index]: payload is empty — skipping map tests")
//                    }
//                }
//            } else {
//                tests += DynamicTest.dynamicTest("bytes are empty — nothing to test") {
//                    org.junit.jupiter.api.Assumptions.assumeTrue(false, "[$index]: bytes are empty — skipping")
//                }
//            }

            val containerName = "Packet[$index] (bytes=${tp.size}, hex=${toHex(tp).take(32)}${if (tp.size * 2 > 32) "..." else ""})"
            DynamicContainer.dynamicContainer(containerName, tests)
        }
    }
}
