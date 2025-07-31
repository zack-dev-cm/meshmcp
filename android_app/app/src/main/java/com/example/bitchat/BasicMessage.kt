package com.example.bitchat

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.UUID

/** Minimal message structure with ID and timestamp. */
data class BasicMessage(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
) {
    fun toBytes(): ByteArray {
        val textBytes = text.toByteArray()
        val buffer = ByteBuffer.allocate(16 + 8 + 2 + textBytes.size)
            .order(ByteOrder.BIG_ENDIAN)
        val uuid = UUID.fromString(id)
        buffer.putLong(uuid.mostSignificantBits)
        buffer.putLong(uuid.leastSignificantBits)
        buffer.putLong(timestamp)
        buffer.putShort(textBytes.size.toShort())
        buffer.put(textBytes)
        return buffer.array()
    }

    companion object {
        fun from(data: ByteArray): BasicMessage? {
            if (data.size < 26) return null
            val buffer = ByteBuffer.wrap(data).order(ByteOrder.BIG_ENDIAN)
            val orig = UUID(buffer.long, buffer.long)
            val ts = buffer.long
            val len = buffer.short.toInt() and 0xFFFF
            if (len > buffer.remaining()) return null
            val bytes = ByteArray(len)
            buffer.get(bytes)
            val text = String(bytes, Charsets.UTF_8)
            return BasicMessage(orig.toString(), text, ts)
        }
    }
}
