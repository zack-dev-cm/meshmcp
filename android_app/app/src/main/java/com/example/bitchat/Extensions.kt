package com.example.bitchat

import android.bluetooth.BluetoothAdapter

fun ByteArray.toHex(): String = joinToString("") { "%02X".format(it) }

/** Utility to validate Bluetooth MAC addresses. */
fun isValidMac(address: String): Boolean =
    BluetoothAdapter.checkBluetoothAddress(address)
