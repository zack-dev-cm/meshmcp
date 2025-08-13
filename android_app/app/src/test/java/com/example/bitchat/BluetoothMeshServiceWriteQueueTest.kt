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
import io.mockk.any
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

class BluetoothMeshServiceWriteQueueTest {
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
    fun queuedWritesAreSequential() = runBlocking {
        val peerId = "AA:BB:CC:DD:EE:FF"
        val gatt = mockk<BluetoothGatt>(relaxed = true)
        val device = mockk<BluetoothDevice>(relaxed = true)
        every { device.address } returns peerId
        every { gatt.device } returns device
        val gattService = mockk<BluetoothGattService>(relaxed = true)
        val characteristic = mockk<BluetoothGattCharacteristic>(relaxed = true)
        every { gatt.getService(any()) } returns gattService
        every { gattService.getCharacteristic(any()) } returns characteristic
        val writeValues = mutableListOf<ByteArray>()
        every { gatt.writeCharacteristic(characteristic) } answers {
            writeValues += characteristic.value
            true
        }
        val connectionsField = BluetoothMeshService::class.java.getDeclaredField("connections").apply { isAccessible = true }
        val connections = connectionsField.get(service) as java.util.concurrent.ConcurrentHashMap<String, PeerConnection>
        connections[peerId] = PeerConnection(gatt = gatt, serviceDiscoveryStarted = true)
        val method = BluetoothMeshService::class.java.getDeclaredMethod(
            "writeOrQueue",
            String::class.java,
            MessageEntity::class.java,
            ByteArray::class.java,
        ).apply { isAccessible = true }
        val msg1 = MessageEntity("1", "me", "hello", 0L, false, null, true, null, peerId, "sending", 0, false, false)
        val msg2 = MessageEntity("2", "me", "world", 0L, false, null, true, null, peerId, "sending", 0, false, false)
        method.invoke(service, peerId, msg1, byteArrayOf(0x01))
        delay(20)
        assertEquals(1, writeValues.size)
        method.invoke(service, peerId, msg2, byteArrayOf(0x02))
        delay(20)
        assertEquals(1, writeValues.size)
        val cbField = BluetoothMeshService::class.java.getDeclaredField("gattClientCallback").apply { isAccessible = true }
        val callback = cbField.get(service) as android.bluetooth.BluetoothGattCallback
        callback.onCharacteristicWrite(gatt, characteristic, BluetoothGatt.GATT_SUCCESS)
        delay(20)
        assertEquals(2, writeValues.size)
        assertEquals(0x02.toByte(), writeValues[1][0])
    }

    @Test
    fun busyStackRetriesWrite() = runBlocking {
        val peerId = "11:22:33:44:55:66"
        val gatt = mockk<BluetoothGatt>(relaxed = true)
        val device = mockk<BluetoothDevice>(relaxed = true)
        every { device.address } returns peerId
        every { gatt.device } returns device
        val gattService = mockk<BluetoothGattService>(relaxed = true)
        val characteristic = mockk<BluetoothGattCharacteristic>(relaxed = true)
        every { gatt.getService(any()) } returns gattService
        every { gattService.getCharacteristic(any()) } returns characteristic
        var first = true
        every { gatt.writeCharacteristic(characteristic) } answers {
            if (first) {
                first = false
                false
            } else {
                true
            }
        }
        val connectionsField = BluetoothMeshService::class.java.getDeclaredField("connections").apply { isAccessible = true }
        val connections = connectionsField.get(service) as java.util.concurrent.ConcurrentHashMap<String, PeerConnection>
        connections[peerId] = PeerConnection(gatt = gatt, serviceDiscoveryStarted = true)
        val method = BluetoothMeshService::class.java.getDeclaredMethod(
            "writeOrQueue",
            String::class.java,
            MessageEntity::class.java,
            ByteArray::class.java,
        ).apply { isAccessible = true }
        val msg = MessageEntity("1", "me", "hello", 0L, false, null, true, null, peerId, "sending", 0, false, false)
        method.invoke(service, peerId, msg, byteArrayOf(0x01))
        delay(150)
        verify(exactly = 2) { gatt.writeCharacteristic(characteristic) }
    }
}

