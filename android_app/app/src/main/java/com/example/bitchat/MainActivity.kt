package com.example.bitchat

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import android.content.ComponentName
import android.content.ServiceConnection
import android.os.IBinder

class MainActivity : ComponentActivity() {
    private val permissions = arrayOf(
        Manifest.permission.BLUETOOTH_SCAN,
        Manifest.permission.BLUETOOTH_ADVERTISE,
        Manifest.permission.BLUETOOTH_CONNECT,
        Manifest.permission.ACCESS_FINE_LOCATION
    )

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
            if (result.all { it.value }) {
                startMeshService()
            }
        }

    private var meshService: MeshService? by mutableStateOf(null)
    private var bound = false
    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            val b = binder as MeshService.LocalBinder
            meshService = b.getService()
            bound = true
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            bound = false
            meshService = null
        }
    }

    private fun startMeshService() {
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        if (!pm.isIgnoringBatteryOptimizations(packageName)) {
            val intent = Intent(
                Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                android.net.Uri.parse("package:$packageName")
            )
            startActivity(intent)
        }
        val intent = Intent(this, MeshService::class.java)
        androidx.core.content.ContextCompat.startForegroundService(this, intent)
        bindService(intent, connection, Context.BIND_AUTO_CREATE)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val missing = permissions.filter {
            androidx.core.content.ContextCompat.checkSelfPermission(this, it) !=
                android.content.pm.PackageManager.PERMISSION_GRANTED
        }
        if (missing.isNotEmpty()) {
            permissionLauncher.launch(permissions)
        } else {
            startMeshService()
        }
        setContent {
            MaterialTheme {
                meshService?.let { ChatScreen(it.bluetoothService) }
            }
        }
    }

    override fun onDestroy() {
        if (bound) {
            unbindService(connection)
            bound = false
        }
        super.onDestroy()
    }
}

@Composable
fun ChatScreen(service: BluetoothMeshService) {
    var text by remember { mutableStateOf(TextFieldValue("")) }
    val messages = remember { mutableStateListOf<String>() }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        messages.forEach { msg ->
            Text(text = msg)
        }
        Spacer(Modifier.weight(1f))
        Row {
            TextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.weight(1f)
            )
            Button(onClick = {
                val input = text.text
                text = TextFieldValue("")
                handleInput(input, service, messages)
            }) {
                Text("Send")
            }
        }
    }
}

fun handleInput(input: String, service: BluetoothMeshService, messages: MutableList<String>) {
    when {
        input.startsWith("/msg ") -> {
            val rest = input.removePrefix("/msg ")
            val parts = rest.split(" ", limit = 2)
            if (parts.size == 2) {
                val peer = parts[0]
                val content = parts[1]
                service.sendMessage(peer, content)
                messages.add("Me -> $peer: $content")
            } else {
                messages.add("Usage: /msg <peerId> <message>")
            }
        }
        input.startsWith("/wipe") -> {
            service.wipeAllData()
            messages.clear()
            messages.add("Data wiped")
        }
        input.startsWith("/who") -> {
            val peers = service.connectedPeers().joinToString(", ")
            messages.add("Peers: $peers")
        }
        else -> messages.add("Unknown command")
    }
}
