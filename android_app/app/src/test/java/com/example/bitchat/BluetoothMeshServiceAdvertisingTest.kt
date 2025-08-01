package com.example.bitchat

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.BluetoothLeAdvertiser
import android.content.Context
import android.content.ContextWrapper
import android.util.Log
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.bitchat.db.AppDatabase
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import io.mockk.just
import io.mockk.Runs
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

private class AdvTestContext(base: Context, private val bluetooth: BluetoothManager) : ContextWrapper(base) {
    override fun getSystemService(name: String): Any? =
        if (name == Context.BLUETOOTH_SERVICE) bluetooth else super.getSystemService(name)
}

class BluetoothMeshServiceAdvertisingTest {
    private lateinit var service: BluetoothMeshService
    private lateinit var db: AppDatabase
    private lateinit var advertiser: BluetoothLeAdvertiser

    @BeforeTest
    fun setup() {
        val base = ApplicationProvider.getApplicationContext<Context>()
        advertiser = mockk(relaxed = true)
        every { advertiser.startAdvertising(any(), any(), any()) } answers {
            val cb = arg<AdvertiseCallback>(2)
            cb.onStartSuccess(null)
        }
        val adapter = mockk<BluetoothAdapter> {
            every { bluetoothLeAdvertiser } returns advertiser
            every { name = any() } just Runs
        }
        val bluetoothManager = mockk<BluetoothManager> { every { adapter } returns adapter }
        val context = AdvTestContext(base, bluetoothManager)
        db = Room.inMemoryDatabaseBuilder(base, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        AppDatabase::class.java.getDeclaredField("INSTANCE").apply { isAccessible = true; set(null, db) }
        AppGlobals::class.java.getDeclaredField("appContext").apply { isAccessible = true; set(null, context) }
        service = BluetoothMeshService()

        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0
        every { Log.w(any(), any()) } returns 0
        every { Log.e(any(), any()) } returns 0
    }

    @AfterTest
    fun tearDown() {
        unmockkStatic(Log::class)
        db.close()
        AppDatabase::class.java.getDeclaredField("INSTANCE").apply { isAccessible = true; set(null, null) }
    }

    private fun invokeStartAdvertising() {
        val m = BluetoothMeshService::class.java.getDeclaredMethod("startAdvertising")
        m.isAccessible = true
        m.invoke(service)
    }

    @Test
    fun startAdvertisingTwiceDoesNotLogError() {
        invokeStartAdvertising()
        invokeStartAdvertising()
        verify(exactly = 1) { advertiser.startAdvertising(any(), any(), any()) }
        verify(exactly = 0) { Log.e(any(), any()) }
    }
}
