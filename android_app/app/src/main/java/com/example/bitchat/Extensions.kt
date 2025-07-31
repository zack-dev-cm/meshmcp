package com.example.bitchat

fun ByteArray.toHex(): String = joinToString("") { "%02X".format(it) }
