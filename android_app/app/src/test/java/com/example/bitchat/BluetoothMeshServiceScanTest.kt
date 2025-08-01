package com.example.bitchat

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager
import android.location.LocationManager
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.bitchat.db.AppDatabase
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private class TestContext(
    base: Context,
    private val bluetooth: BluetoothManager,
    private val location: LocationManager,
) : ContextWrapper(base) {
    private val granted = mutableSetOf<String>()
    fun grant(permission: String) { granted += permission }
    fun revoke(permission: String) { granted -= permission }
    override fun checkPermission(permission: String, pid: Int, uid: Int): Int {
        return if (granted.contains(permission)) PackageManager.PERMISSION_GRANTED else PackageManager.PERMISSION_DENIED
    }
    override fun getSystemService(name: String): Any? {
        return when (name) {
            Context.BLUETOOTH_SERVICE -> bluetooth
            Context.LOCATION_SERVICE -> location
            else -> super.getSystemService(name)
        }
    }
}

class BluetoothMeshServiceScanTest {
    private lateinit var service: BluetoothMeshService
    private lateinit var db: AppDatabase
    private lateinit var context: TestContext
    private lateinit var scanner: BluetoothLeScanner
    private lateinit var locationManager: LocationManager

    @BeforeTest
    fun setup() {
        val base = ApplicationProvider.getApplicationContext<Context>()
        scanner = mockk(relaxed = true)
        val adapter = mockk<BluetoothAdapter> {
            every { bluetoothLeScanner } returns scanner
        }
        val bluetoothManager = mockk<BluetoothManager> {
            every { adapter } returns adapter
        }
        locationManager = mockk {
            every { isLocationEnabled } returns true
        }
        context = TestContext(base, bluetoothManager, locationManager)
        db = Room.inMemoryDatabaseBuilder(base, AppDatabase::class.java)
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

    @AfterTest
    fun tearDown() {
        db.close()
        AppDatabase::class.java.getDeclaredField("INSTANCE").apply {
            isAccessible = true
            set(null, null)
        }
    }

    private fun invokeStartScanning() {
        val m = BluetoothMeshService::class.java.getDeclaredMethod("startScanning")
        m.isAccessible = true
        m.invoke(service)
    }

    @Test
    fun scanningStartsWhenAllRequirementsMet() {
        context.grant(Manifest.permission.ACCESS_FINE_LOCATION)
        context.grant(Manifest.permission.BLUETOOTH_SCAN)
        every { locationManager.isLocationEnabled } returns true

        invokeStartScanning()

        assertTrue(service.scanningFlow.value)
        assertTrue(service.missingRequirementsFlow.value.isEmpty())
        verify { scanner.startScan(any(), any(), any()) }
    }

    @Test
    fun scanningFailsWithoutLocationPermission() {
        context.revoke(Manifest.permission.ACCESS_FINE_LOCATION)
        context.grant(Manifest.permission.BLUETOOTH_SCAN)
        every { locationManager.isLocationEnabled } returns true

        invokeStartScanning()

        assertFalse(service.scanningFlow.value)
        assertTrue(
            service.missingRequirementsFlow.value.contains(
                BluetoothMeshService.ScanRequirement.FINE_LOCATION_PERMISSION,
            ),
        )
        verify(exactly = 0) { scanner.startScan(any(), any(), any()) }
    }

    @Test
    fun scanningFailsWithoutScanPermission() {
        context.grant(Manifest.permission.ACCESS_FINE_LOCATION)
        context.revoke(Manifest.permission.BLUETOOTH_SCAN)
        every { locationManager.isLocationEnabled } returns true

        invokeStartScanning()

        assertFalse(service.scanningFlow.value)
        assertTrue(
            service.missingRequirementsFlow.value.contains(
                BluetoothMeshService.ScanRequirement.BLUETOOTH_SCAN_PERMISSION,
            ),
        )
        verify(exactly = 0) { scanner.startScan(any(), any(), any()) }
    }

    @Test
    fun scanningFailsWhenLocationDisabled() {
        context.grant(Manifest.permission.ACCESS_FINE_LOCATION)
        context.grant(Manifest.permission.BLUETOOTH_SCAN)
        every { locationManager.isLocationEnabled } returns false

        invokeStartScanning()

        assertFalse(service.scanningFlow.value)
        assertTrue(
            service.missingRequirementsFlow.value.contains(
                BluetoothMeshService.ScanRequirement.LOCATION_ENABLED,
            ),
        )
        verify(exactly = 0) { scanner.startScan(any(), any(), any()) }
    }
}
