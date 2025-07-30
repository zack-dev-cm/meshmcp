package com.example.bitchat

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NoiseBleIntegrationTest {
    @Test
    fun handshakeAndExchange() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val serviceA = BluetoothMeshService()
        val serviceB = BluetoothMeshService()

        serviceA.packetSender = { _, data -> serviceB.onBlePacketReceived("A", data) }
        serviceB.packetSender = { _, data -> serviceA.onBlePacketReceived("B", data) }

        serviceA.onPeerConnected("B")
        serviceB.onPeerConnected("A")

        assertEquals(NoiseEncryptionService.EncryptionStatus.NOISE_SECURED, serviceA.encryptionStatus("B"))
        assertEquals(NoiseEncryptionService.EncryptionStatus.NOISE_SECURED, serviceB.encryptionStatus("A"))

        val data = "ping".toByteArray()
        val enc = serviceA.encryptMessage("B", data)
        val dec = serviceB.decryptMessage("A", enc)
        assertArrayEquals(data, dec)
    }
}
