package com.example.bitchat

import android.bluetooth.BluetoothAdapter

fun ByteArray.toHex(): String = joinToString("") { "%02X".format(it) }

/** Convert a hexadecimal string to a byte array. Non-hex characters are ignored. */
fun String.hexToByteArray(): ByteArray {
    val clean = replace("[^0-9A-Fa-f]".toRegex(), "")
    val len = clean.length / 2
    val out = ByteArray(len)
    for (i in 0 until len) {
        val hex = clean.substring(i * 2, i * 2 + 2)
        out[i] = hex.toInt(16).toByte()
    }
    return out
}

/** Utility to validate Bluetooth MAC addresses. */
fun isValidMac(address: String): Boolean =
    BluetoothAdapter.checkBluetoothAddress(address)
