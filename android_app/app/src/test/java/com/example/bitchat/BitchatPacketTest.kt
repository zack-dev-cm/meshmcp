package com.example.bitchat

import io.mockk.spyk
import io.mockk.verify
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertNotNull
import org.junit.Test

class BitchatPacketTest {
    @Test
    fun encodeDecodeRoundTrip() {
        val senderId = ByteArray(8) { it.toByte() }
        val payload = "hello".toByteArray()
        val packet = spyk(BitchatPacket(MessageType.MESSAGE, senderId, payload, 5))
        val bytes = packet.toBytes()
        verify { packet.toBytes() }
        val decoded = BitchatPacket.from(bytes)
        assertNotNull(decoded)
        assertEquals(packet, decoded)
        assertArrayEquals(bytes, decoded!!.toBytes())
    }

    @Test
    fun decodeInvalidTypeReturnsNull() {
        val invalid = ByteArray(10)
        invalid[0] = 0x7F
        val result = BitchatPacket.from(invalid)
        assertNull(result)
    }
}
