package inoagent.maxapi.packets

import OpcodeEnum
import Packet
import Utils

fun AuthPacket(phone: String, type: String = "START_AUTH"): Packet {
    val payload = Utils.packMapToBytes(
        hashMapOf(
            "phone" to phone,
            "type" to type
        )
    )
    return Packet(opcode = OpcodeEnum.AUTH_REQUEST, payload = payload)
}

fun SessionInitPacket(
    clientSessionId: Int = 2,
    mt_instanceid: String = "884a3b39-68ca-4e73-a3c1-6ed7f97fb709",
    deviceId: String = "6f538c4af7791b40",
): Packet {
    val map = hashMapOf<String, Any?>(
        "clientSessionId" to clientSessionId,
        "mt_instanceid" to mt_instanceid,
        "userAgent" to hashMapOf(
            "deviceType" to "ANDROID",
            "appVersion" to "25.9.0",
            "osVersion" to "Android 12",
            "timezone" to "Europe/Moscow",
            "screen" to "mdpi 160dpi 1366x720",
            "pushDeviceType" to "GCM",
            "locale" to "ru",
            "buildNumber" to 6395,
            "deviceName" to "unknown Android SDK built for x86_64",
            "deviceLocale" to "en"
        ),
        "deviceId" to deviceId
    )
    val payload = Utils.packMapToBytes(map)
    return Packet(opcode = OpcodeEnum.SESSION_INIT, payload = payload)
}
