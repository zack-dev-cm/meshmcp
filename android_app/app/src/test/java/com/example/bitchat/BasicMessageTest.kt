package com.example.bitchat

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class BasicMessageTest {
    @Test
    fun encodeDecodeRoundTrip() {
        val msg = BasicMessage(text = "hello")
        val bytes = msg.toBytes()
        val decoded = BasicMessage.from(bytes)
        assertNotNull(decoded)
        assertEquals(msg.copy(), decoded)
    }
}
