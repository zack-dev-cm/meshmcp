package com.example.bitchat

import io.mockk.spyk
import io.mockk.verify
import org.junit.Assert.*
import org.junit.Test

class BitchatPacketTest {
    @Test
    fun encodeDecodeRoundTrip() {
        val senderId = ByteArray(8) { it.toByte() }
        val payload = "hello".toByteArray()
        val packet = spyk(
            BitchatPacket(
                version = 1,
                type = MessageType.MESSAGE,
                ttl = 5,
                timestamp = 123456789L,
                senderId = senderId,
                payload = payload
            )
        )
        val bytes = packet.toBytes()
        verify { packet.toBytes() }
        val decoded = BitchatPacket.from(bytes)
        assertNotNull(decoded)
        assertEquals(packet, decoded)
        assertArrayEquals(bytes, decoded!!.toBytes())
    }

    @Test
    fun encodeDecodeWithRecipient() {
        val senderId = ByteArray(8) { it.toByte() }
        val recipientId = ByteArray(8) { (it + 1).toByte() }
        val payload = "hi".toByteArray()
        val packet = BitchatPacket(
            type = MessageType.MESSAGE,
            ttl = 2,
            timestamp = 987654321L,
            senderId = senderId,
            recipientId = recipientId,
            payload = payload
        )
        val bytes = packet.toBytes()
        val decoded = BitchatPacket.from(bytes)
        assertNotNull(decoded)
        assertArrayEquals(recipientId, decoded!!.recipientId)
        assertEquals(packet, decoded)
    }

    @Test
    fun decodeInvalidTypeReturnsNull() {
        val invalid = ByteArray(30) { 0 }
        invalid[1] = 0x7F.toByte()
        val result = BitchatPacket.from(invalid)
        assertNull(result)
    }
}
