package com.example.bitchat

import java.nio.ByteBuffer

enum class MessageType(val id: Byte) {
    ANNOUNCE(0x01),
    LEAVE(0x03),
    MESSAGE(0x04);
}

data class BitchatPacket(
    val type: MessageType,
    val senderId: ByteArray,
    val payload: ByteArray,
    var ttl: Byte = 3
) {
    fun toBytes(): ByteArray {
        val buffer = ByteBuffer.allocate(1 + senderId.size + payload.size + 1)
        buffer.put(type.id)
        buffer.put(senderId)
        buffer.put(payload)
        buffer.put(ttl)
        return buffer.array()
    }

    companion object {
        fun from(data: ByteArray): BitchatPacket? {
            if (data.isEmpty()) return null
            val type = MessageType.values().firstOrNull { it.id == data[0] } ?: return null
            val senderId = data.sliceArray(1 until 9)
            val payload = data.sliceArray(9 until data.size - 1)
            val ttl = data.last()
            return BitchatPacket(type, senderId, payload, ttl)
        }
    }
}
