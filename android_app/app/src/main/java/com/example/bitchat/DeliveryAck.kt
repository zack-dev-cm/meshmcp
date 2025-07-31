package com.example.bitchat

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.UUID

/** Delivery acknowledgment for private messages. */
data class DeliveryAck(
    val originalMessageId: String,
    val ackId: String = UUID.randomUUID().toString(),
    val recipientId: String,
    val recipientNickname: String,
    val timestamp: Long = System.currentTimeMillis(),
    val hopCount: Int = 0,
) {
    fun toBytes(): ByteArray {
        val nickBytes = recipientNickname.toByteArray()
        val buffer = ByteBuffer.allocate(16 + 16 + 8 + 1 + 8 + 1 + nickBytes.size)
            .order(ByteOrder.BIG_ENDIAN)
        val orig = UUID.fromString(originalMessageId)
        val ack = UUID.fromString(ackId)
        buffer.putLong(orig.mostSignificantBits)
        buffer.putLong(orig.leastSignificantBits)
        buffer.putLong(ack.mostSignificantBits)
        buffer.putLong(ack.leastSignificantBits)
        buffer.put(recipientId.hexToByteArray().copyOf(8))
        buffer.put(hopCount.toByte())
        buffer.putLong(timestamp)
        buffer.put(nickBytes.size.toByte())
        buffer.put(nickBytes)
        return buffer.array()
    }

    companion object {
        fun from(data: ByteArray): DeliveryAck? {
            if (data.size < 42) return null
            val buffer = ByteBuffer.wrap(data).order(ByteOrder.BIG_ENDIAN)
            val orig = UUID(buffer.long, buffer.long)
            val ack = UUID(buffer.long, buffer.long)
            val peerBytes = ByteArray(8)
            buffer.get(peerBytes)
            val hop = buffer.get().toInt() and 0xFF
            val ts = buffer.long
            val len = buffer.get().toInt() and 0xFF
            if (len > buffer.remaining()) return null
            val nickBytes = ByteArray(len)
            buffer.get(nickBytes)
            val nick = String(nickBytes, Charsets.UTF_8)
            return DeliveryAck(orig.toString(), ack.toString(), peerBytes.toHex(), nick, ts, hop)
        }
    }
}
