package com.example.bitchat

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.bitchat.db.AppDatabase
import com.example.bitchat.db.MessageEntity
import io.mockk.every
import io.mockk.mockk
import io.mockk.any
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.delay
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BluetoothMeshServiceRetryTest {
    private lateinit var service: BluetoothMeshService
    private lateinit var db: AppDatabase

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        AppDatabase::class.java.getDeclaredField("INSTANCE").apply {
            isAccessible = true
            set(null, db)
        }
        AppGlobals::class.java.getDeclaredField("appContext").apply {
            isAccessible = true
            set(null, context)
        }
        service = BluetoothMeshService()
    }

    @After
    fun tearDown() {
        db.close()
        AppDatabase::class.java.getDeclaredField("INSTANCE").apply {
            isAccessible = true
            set(null, null)
        }
    }

    @Test
    fun writeRetriesExponentialBackoff() = runBlocking {
        val baseField = BluetoothMeshService::class.java.getDeclaredField("writeRetryBaseDelay").apply { isAccessible = true }
        baseField.setLong(service, 10L)
        val maxField = BluetoothMeshService::class.java.getDeclaredField("writeRetryMaxDelay").apply { isAccessible = true }
        maxField.setLong(service, 40L)

        val peerId = "AA:BB:CC:DD:EE:FF"
        val entity = MessageEntity(
            id = "1",
            sender = "me",
            content = "hello",
            timestamp = 0L,
            isRelay = false,
            originalSender = null,
            isPrivate = true,
            recipientNickname = null,
            senderPeerId = peerId,
            deliveryStatus = "sending",
            retryCount = 0,
            isFavorite = false,
            delivered = false,
        )
        val queueField = BluetoothMeshService::class.java.getDeclaredField("outgoingQueues").apply { isAccessible = true }
        val queues = queueField.get(service) as java.util.concurrent.ConcurrentHashMap<String, MutableList<Pair<MessageEntity, ByteArray>>>
        queues[peerId] = java.util.Collections.synchronizedList(mutableListOf(entity to byteArrayOf(0x01)))

        val gatt = mockk<BluetoothGatt>(relaxed = true)
        val device = mockk<BluetoothDevice>(relaxed = true)
        every { device.address } returns peerId
        every { gatt.device } returns device
        val characteristic = mockk<BluetoothGattCharacteristic>(relaxed = true)

        val callTimes = mutableListOf<Long>()
        every { gatt.writeCharacteristic(characteristic) } answers {
            callTimes += System.currentTimeMillis()
            true
        }

        val cbField = BluetoothMeshService::class.java.getDeclaredField("gattClientCallback").apply { isAccessible = true }
        val callback = cbField.get(service) as android.bluetooth.BluetoothGattCallback

        val start1 = System.currentTimeMillis()
        callback.onCharacteristicWrite(gatt, characteristic, 133)
        delay(30)
        assertEquals(1, callTimes.size)
        val firstDelay = callTimes[0] - start1
        assertTrue(firstDelay >= 10)

        val start2 = System.currentTimeMillis()
        callback.onCharacteristicWrite(gatt, characteristic, 133)
        delay(60)
        assertEquals(2, callTimes.size)
        val secondDelay = callTimes[1] - start2
        assertTrue(secondDelay >= 20)
        assertTrue(secondDelay >= firstDelay * 2)
        assertEquals(2, service.writeFailureCounts[133])
    }

    @Test
    fun connectionRetriesAndQueueCleared() = runBlocking {
        val baseField = BluetoothMeshService::class.java.getDeclaredField("connectionRetryBaseDelay").apply { isAccessible = true }
        baseField.setLong(service, 10L)
        val maxDelayField = BluetoothMeshService::class.java.getDeclaredField("connectionRetryMaxDelay").apply { isAccessible = true }
        maxDelayField.setLong(service, 40L)
        val maxRetriesField = BluetoothMeshService::class.java.getDeclaredField("maxConnectionRetries").apply { isAccessible = true }
        maxRetriesField.setInt(service, 2)

        val peerId = "11:22:33:44:55:66"
        val entity = MessageEntity(
            id = "2",
            sender = "me",
            content = "hi",
            timestamp = 0L,
            isRelay = false,
            originalSender = null,
            isPrivate = true,
            recipientNickname = null,
            senderPeerId = peerId,
            deliveryStatus = "sending",
            retryCount = 0,
            isFavorite = false,
            delivered = false,
        )
        val queueField = BluetoothMeshService::class.java.getDeclaredField("outgoingQueues").apply { isAccessible = true }
        val queues = queueField.get(service) as java.util.concurrent.ConcurrentHashMap<String, MutableList<Pair<MessageEntity, ByteArray>>>
        queues[peerId] = java.util.Collections.synchronizedList(mutableListOf(entity to byteArrayOf(0x01)))

        val gatt = mockk<BluetoothGatt>(relaxed = true)
        val device = mockk<BluetoothDevice>(relaxed = true)
        every { device.address } returns peerId
        every { gatt.device } returns device
        every { device.connectGatt(any<Context>(), any(), any<BluetoothGattCallback>()) } returns gatt

        val cbField = BluetoothMeshService::class.java.getDeclaredField("gattClientCallback").apply { isAccessible = true }
        val callback = cbField.get(service) as android.bluetooth.BluetoothGattCallback

        callback.onConnectionStateChange(gatt, 133, android.bluetooth.BluetoothProfile.STATE_DISCONNECTED)
        delay(30)
        assertEquals(1, service.connectionFailureCounts[133])
        assertTrue(queues[peerId]?.isNotEmpty() == true)

        callback.onConnectionStateChange(gatt, 133, android.bluetooth.BluetoothProfile.STATE_DISCONNECTED)
        delay(30)
        assertEquals(2, service.connectionFailureCounts[133])
        assertNull(queues[peerId])
    }
}

