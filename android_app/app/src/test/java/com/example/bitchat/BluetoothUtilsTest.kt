package com.example.bitchat

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BluetoothUtilsTest {
    @Test
    fun isValidMac_validAndInvalid() {
        assertTrue(isValidMac("00:11:22:33:44:55"))
        assertFalse(isValidMac("0011:2233:4455"))
        assertFalse(isValidMac("invalid"))
    }
}
