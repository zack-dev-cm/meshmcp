package com.example.bitchat

import java.nio.ByteBuffer
import java.nio.ByteOrder

enum class MessageType(val id: Byte) {
    ANNOUNCE(0x01),
    LEAVE(0x03),
    MESSAGE(0x04),
    FRAGMENT_START(0x05),
    FRAGMENT_CONTINUE(0x06),
    FRAGMENT_END(0x07),
    DELIVERY_ACK(0x0A),
    DELIVERY_STATUS_REQUEST(0x0B),
    READ_RECEIPT(0x0C),
    NOISE_HANDSHAKE_INIT(0x10),
    NOISE_HANDSHAKE_RESP(0x11),
    NOISE_ENCRYPTED(0x12),
    NOISE_IDENTITY_ANNOUNCE(0x13),
    VERSION_HELLO(0x20),
    VERSION_ACK(0x21),
    PROTOCOL_ACK(0x22),
    PROTOCOL_NACK(0x23),
    SYSTEM_VALIDATION(0x24),
    HANDSHAKE_REQUEST(0x25);

    companion object {
        fun fromId(id: Byte): MessageType? = values().firstOrNull { it.id == id }
    }
}

data class BitchatPacket(
    val version: Int = 1,
    val type: MessageType,
    var ttl: Byte = 3,
    val timestamp: Long = System.currentTimeMillis(),
    val senderId: ByteArray,
    val recipientId: ByteArray? = null,
    val payload: ByteArray,
    val signature: ByteArray? = null
) {
    fun toBytes(): ByteArray {
        val flags = computeFlags()
        val payloadLength = payload.size.toShort()
        val capacity = 13 + 8 + (if (recipientId != null) 8 else 0) + payload.size + (signature?.size ?: 0)
        val buffer = ByteBuffer.allocate(capacity).order(ByteOrder.BIG_ENDIAN)
        buffer.put(version.toByte())
        buffer.put(type.id)
        buffer.put(ttl)
        buffer.putLong(timestamp)
        buffer.put(flags)
        buffer.putShort(payloadLength)
        buffer.put(senderId.copyOf(8))
        recipientId?.let { buffer.put(it.copyOf(8)) }
        buffer.put(payload)
        signature?.let { buffer.put(it) }
        return buffer.array()
    }

    private fun computeFlags(): Byte {
        var f = 0
        if (recipientId != null) {
            f = f or Flags.HAS_RECIPIENT.toInt()
        }
        if (signature != null) {
            f = f or Flags.HAS_SIGNATURE.toInt()
        }
        return f.toByte()
    }

    companion object {
        object Flags {
            const val HAS_RECIPIENT: Byte = 0x01
            const val HAS_SIGNATURE: Byte = 0x02
            const val IS_COMPRESSED: Byte = 0x04
        }

        fun from(data: ByteArray): BitchatPacket? {
            if (data.size < 21) return null // minimum header + senderId
            val buffer = ByteBuffer.wrap(data).order(ByteOrder.BIG_ENDIAN)
            val version = buffer.get()
            val typeByte = buffer.get()
            val type = MessageType.fromId(typeByte) ?: return null
            val ttl = buffer.get()
            val timestamp = buffer.long
            val flags = buffer.get()
            val payloadLength = buffer.short.toInt() and 0xFFFF
            val senderId = ByteArray(8)
            buffer.get(senderId)
            var recipientId: ByteArray? = null
            if ((flags.toInt() and Flags.HAS_RECIPIENT.toInt()) != 0) {
                if (buffer.remaining() < 8) return null
                recipientId = ByteArray(8)
                buffer.get(recipientId)
            }
            if (buffer.remaining() < payloadLength) return null
            val payload = ByteArray(payloadLength)
            buffer.get(payload)
            var signature: ByteArray? = null
            if ((flags.toInt() and Flags.HAS_SIGNATURE.toInt()) != 0) {
                if (buffer.remaining() < 64) return null
                signature = ByteArray(64)
                buffer.get(signature)
            }
            return BitchatPacket(version, type, ttl, timestamp, senderId, recipientId, payload, signature)
        }
    }
}
