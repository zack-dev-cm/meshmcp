package com.example.bitchat

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.bitchat.db.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.reflect.jvm.isAccessible

class BluetoothMeshServiceConcurrencyTest {
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
    fun sendMessageWhileConnectingDoesNotThrow() = runBlocking {
        val scopeField = BluetoothMeshService::class.java.getDeclaredField("scope").apply { isAccessible = true }
        val serviceScope = scopeField.get(service) as CoroutineScope
        val parentJob = serviceScope.coroutineContext[Job]!!

        val job1 = launch { service.sendPrivateMessage("peer1", "hello") }
        val job2 = launch { service.onPeerConnected("peer1") }
        job1.join()
        job2.join()
        parentJob.children.toList().forEach { it.join() }
    }
}
