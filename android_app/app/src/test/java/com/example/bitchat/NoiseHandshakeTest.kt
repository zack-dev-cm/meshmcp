package com.example.bitchat

import com.southernstorm.noise.protocol.HandshakeState
import com.southernstorm.noise.protocol.Noise
import io.mockk.spyk
import io.mockk.verify
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class NoiseHandshakeTest {
    @Test
    fun xxHandshakeCompletes() {
        val initiator = spyk(HandshakeState("Noise_XX_25519_ChaChaPoly_SHA256", HandshakeState.INITIATOR))
        val responder = spyk(HandshakeState("Noise_XX_25519_ChaChaPoly_SHA256", HandshakeState.RESPONDER))

        initiator.start()
        responder.start()

        val msg1 = ByteArray(Noise.MAX_PACKET_LEN)
        val len1 = initiator.writeMessage(msg1, 0, null, 0, 0)
        val msg2 = ByteArray(Noise.MAX_PACKET_LEN)
        responder.readMessage(msg1, 0, len1, null, 0)
        val len2 = responder.writeMessage(msg2, 0, null, 0, 0)

        val msg3 = ByteArray(Noise.MAX_PACKET_LEN)
        initiator.readMessage(msg2, 0, len2, null, 0)
        val len3 = initiator.writeMessage(msg3, 0, null, 0, 0)
        responder.readMessage(msg3, 0, len3, null, 0)

        verify { initiator.writeMessage(any(), any(), any(), any(), any()) }
        verify { responder.writeMessage(any(), any(), any(), any(), any()) }

        val pairInitiator = initiator.split()
        val pairResponder = responder.split()

        val plaintext = "secret".toByteArray()
        val out = ByteArray(plaintext.size + pairInitiator.sender.macLength)
        val encLen = pairInitiator.sender.encryptWithAd(null, plaintext, 0, out, 0, plaintext.size)
        val decrypted = ByteArray(encLen)
        val decLen = pairResponder.receiver.decryptWithAd(null, out, 0, decrypted, 0, encLen)
        assertEquals(encLen, decLen)
        assertArrayEquals(plaintext, decrypted.copyOf(decLen))
    }
}
