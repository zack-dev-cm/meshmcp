package com.example.bitchat

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattService
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.bitchat.db.AppDatabase
import com.example.bitchat.db.MessageEntity
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.assertTrue
import kotlin.reflect.jvm.isAccessible

class BluetoothMeshServiceQueueTest {
    private lateinit var service: BluetoothMeshService
    private lateinit var db: AppDatabase

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val dbField = AppDatabase::class.java.getDeclaredField("INSTANCE").apply { isAccessible = true }
        dbField.set(null, db)
        val appField = AppGlobals::class.java.getDeclaredField("appContext").apply { isAccessible = true }
        appField.set(null, context)
        service = BluetoothMeshService()
    }

    @After
    fun tearDown() {
        db.close()
        val dbField = AppDatabase::class.java.getDeclaredField("INSTANCE").apply { isAccessible = true }
        dbField.set(null, null)
    }

    @Test
    fun queuedWritesSendSequentially() = runBlocking {
        val scopeField = BluetoothMeshService::class.java.getDeclaredField("scope").apply { isAccessible = true }
        val serviceScope = scopeField.get(service) as CoroutineScope
        val parentJob = serviceScope.coroutineContext[Job]!!

        val peerId = "AA:BB:CC:DD:EE:01"
        val gatt = mockk<BluetoothGatt>(relaxed = true)
        val device = mockk<BluetoothDevice>()
        every { device.address } returns peerId
        every { gatt.device } returns device

        val serviceUuidField = BluetoothMeshService::class.java.getDeclaredField("serviceUuid").apply { isAccessible = true }
        val characteristicUuidField = BluetoothMeshService::class.java.getDeclaredField("characteristicUuid").apply { isAccessible = true }
        val gattService = mockk<BluetoothGattService>()
        val characteristic = mockk<BluetoothGattCharacteristic>(relaxed = true)
        every { gatt.getService(serviceUuidField.get(service) as java.util.UUID) } returns gattService
        every { gattService.getCharacteristic(characteristicUuidField.get(service) as java.util.UUID) } returns characteristic
        every { characteristic.descriptors } returns emptyList()
        every { gatt.writeCharacteristic(characteristic) } returns true

        val connectionsField = BluetoothMeshService::class.java.getDeclaredField("connections").apply { isAccessible = true }
        val connections = connectionsField.get(service) as MutableMap<String, PeerConnection>
        connections[peerId] = PeerConnection(gatt = gatt, serviceDiscoveryStarted = true)

        service.sendPrivateMessage(peerId, "one")
        parentJob.children.toList().forEach { it.join() }
        verify(exactly = 1) { gatt.writeCharacteristic(characteristic) }

        service.sendPrivateMessage(peerId, "two")
        parentJob.children.toList().forEach { it.join() }
        verify(exactly = 1) { gatt.writeCharacteristic(characteristic) }

        val cbField = BluetoothMeshService::class.java.getDeclaredField("gattClientCallback").apply { isAccessible = true }
        val callback = cbField.get(service) as android.bluetooth.BluetoothGattCallback
        callback.onCharacteristicWrite(gatt, characteristic, BluetoothGatt.GATT_SUCCESS)
        parentJob.children.toList().forEach { it.join() }
        verify(exactly = 2) { gatt.writeCharacteristic(characteristic) }

        val queueField = BluetoothMeshService::class.java.getDeclaredField("outgoingQueues").apply { isAccessible = true }
        val queues = queueField.get(service) as Map<String, MutableList<Pair<MessageEntity, ByteArray>>>
        assertTrue(queues[peerId]?.isEmpty() ?: true)
    }

    @Test
    fun busyStackRetries() = runBlocking {
        val scopeField = BluetoothMeshService::class.java.getDeclaredField("scope").apply { isAccessible = true }
        val serviceScope = scopeField.get(service) as CoroutineScope
        val parentJob = serviceScope.coroutineContext[Job]!!

        val peerId = "AA:BB:CC:DD:EE:02"
        val gatt = mockk<BluetoothGatt>(relaxed = true)
        val device = mockk<BluetoothDevice>()
        every { device.address } returns peerId
        every { gatt.device } returns device

        val serviceUuidField = BluetoothMeshService::class.java.getDeclaredField("serviceUuid").apply { isAccessible = true }
        val characteristicUuidField = BluetoothMeshService::class.java.getDeclaredField("characteristicUuid").apply { isAccessible = true }
        val gattService = mockk<BluetoothGattService>()
        val characteristic = mockk<BluetoothGattCharacteristic>(relaxed = true)
        every { gatt.getService(serviceUuidField.get(service) as java.util.UUID) } returns gattService
        every { gattService.getCharacteristic(characteristicUuidField.get(service) as java.util.UUID) } returns characteristic
        every { characteristic.descriptors } returns emptyList()
        every { gatt.writeCharacteristic(characteristic) } returnsMany listOf(false, true)

        val connectionsField = BluetoothMeshService::class.java.getDeclaredField("connections").apply { isAccessible = true }
        val connections = connectionsField.get(service) as MutableMap<String, PeerConnection>
        connections[peerId] = PeerConnection(gatt = gatt, serviceDiscoveryStarted = true)

        service.sendPrivateMessage(peerId, "hello")
        parentJob.children.toList().forEach { it.join() }
        delay(200)
        parentJob.children.toList().forEach { it.join() }
        verify(exactly = 2) { gatt.writeCharacteristic(characteristic) }

        val cbField = BluetoothMeshService::class.java.getDeclaredField("gattClientCallback").apply { isAccessible = true }
        val callback = cbField.get(service) as android.bluetooth.BluetoothGattCallback
        callback.onCharacteristicWrite(gatt, characteristic, BluetoothGatt.GATT_SUCCESS)
        parentJob.children.toList().forEach { it.join() }

        val queueField = BluetoothMeshService::class.java.getDeclaredField("outgoingQueues").apply { isAccessible = true }
        val queues = queueField.get(service) as Map<String, MutableList<Pair<MessageEntity, ByteArray>>>
        assertTrue(queues[peerId]?.isEmpty() ?: true)
    }
}
