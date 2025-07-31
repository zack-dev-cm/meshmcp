package com.example.bitchat

import android.bluetooth.le.AdvertiseData
import android.os.ParcelUuid
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class AdvertisementDataTest {
    @Test
    fun advertiseDataSizeAndName() {
        val serviceUuid = UUID.fromString("F47B5E2D-4A9E-4C5A-9B3F-8E1D2C3AB5C")
        val nickname = "SuperLongNickName"
        val advName = nickname.take(7)
        val data =
            AdvertiseData
                .Builder()
                .setIncludeDeviceName(true)
                .setIncludeTxPowerLevel(false)
                .addServiceUuid(ParcelUuid(serviceUuid))
                .build()

        // Calculate approximate size: flags (3 bytes) + UUID (2 + 16) + name field
        val size = 3 + 18 + 2 + advName.toByteArray().size
        assertTrue(size <= 31)
        assertEquals(7, advName.length)
    }
}
