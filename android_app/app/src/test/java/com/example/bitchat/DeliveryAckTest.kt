package com.example.bitchat

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class DeliveryAckTest {
    @Test
    fun encodeDecodeRoundTrip() {
        val ack =
            DeliveryAck(
                originalMessageId = "11111111-1111-1111-1111-111111111111",
                recipientId = "AABBCCDDEEFF",
                recipientNickname = "Bob",
                hopCount = 1,
            )
        val bytes = ack.toBytes()
        val decoded = DeliveryAck.from(bytes)
        assertNotNull(decoded)
        assertEquals(ack.copy(), decoded)
    }
}
