package inoagent.maxapi

import ConnectionManager
import Packet
import Utils.testPacket
import inoagent.maxapi.packets.*
import kotlinx.coroutines.delay

suspend fun init() {
    val initPacket = SessionInitPacket()
    val authPacket = AuthPacket("+79111111111")

    val manager = ConnectionManager()
    try {
        manager.init()

        manager.send(initPacket)
        delay(1_000)
        manager.send(authPacket)
        delay(60_000)
    } catch (e: Exception) {
        e.printStackTrace()
    } finally {
        manager.close()
    }
}

suspend fun main() {
    init()
}