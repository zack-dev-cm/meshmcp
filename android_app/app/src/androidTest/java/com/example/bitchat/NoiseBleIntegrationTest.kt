package com.example.bitchat

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.mockk.spyk
import io.mockk.verify
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NoiseBleIntegrationTest {
    @Test
    fun handshakeAndExchange() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val serviceA = spyk(NoiseEncryptionService(context))
        val serviceB = NoiseEncryptionService(context)

        val init = serviceA.initiateHandshake("B")
        val resp = serviceB.receiveHandshakeMessage("A", init)!!
        val fin = serviceA.receiveHandshakeMessage("B", resp)!!
        serviceB.receiveHandshakeMessage("A", fin)

        assertEquals(NoiseEncryptionService.EncryptionStatus.NOISE_SECURED, serviceA.status("B"))
        assertEquals(NoiseEncryptionService.EncryptionStatus.NOISE_SECURED, serviceB.status("A"))

        val data = "ping".toByteArray()
        val enc = serviceA.encrypt("B", data)
        val dec = serviceB.decrypt("A", enc)
        assertArrayEquals(data, dec)

        verify { serviceA.initiateHandshake("B") }
    }
}
