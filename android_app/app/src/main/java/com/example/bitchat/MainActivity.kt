package com.example.bitchat

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.example.bitchat.db.MessageEntity

class MainActivity : ComponentActivity() {
    private val permissions: Array<String>
        get() {
            val list = mutableListOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_ADVERTISE,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.ACCESS_FINE_LOCATION,
            )
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                list += Manifest.permission.FOREGROUND_SERVICE_CONNECTED_DEVICE
            }
            return list.toTypedArray()
        }

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
            if (result.all { it.value }) {
                startMeshService()
            }
        }

    private var meshService: MeshService? by mutableStateOf(null)
    private var bound = false
    private val connection =
        object : ServiceConnection {
            override fun onServiceConnected(
                name: ComponentName?,
                binder: IBinder?,
            ) {
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
            val intent =
                Intent(
                    Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                    android.net.Uri.parse("package:$packageName"),
                )
            startActivity(intent)
        }
        val intent = Intent(this, MeshService::class.java)
        androidx.core.content.ContextCompat
            .startForegroundService(this, intent)
        bindService(intent, connection, Context.BIND_AUTO_CREATE)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val missing =
            permissions.filter {
                androidx.core.content.ContextCompat
                    .checkSelfPermission(this, it) !=
                    android.content.pm.PackageManager.PERMISSION_GRANTED
            }
        if (missing.isNotEmpty()) {
            permissionLauncher.launch(missing.toTypedArray())
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
    val messages by service.messages.collectAsState(initial = emptyList())
    val peers by service.peersFlow.collectAsState(initial = emptyList())
    val discovered by service.discoveredPeersFlow.collectAsState(initial = emptyList())

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Peers: " + peers.joinToString(", "))
        if (discovered.isNotEmpty()) {
            Text("Available peers:")
            LazyColumn(modifier = Modifier.height(100.dp)) {
                items(discovered) { addr ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { service.connectToPeer(addr) }
                            .padding(4.dp)
                    ) {
                        Text(addr)
                    }
                }
            }
        }
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(messages) { msg ->
                MessageItem(msg)
            }
        }
        Row {
            TextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.weight(1f),
            )
            Button(onClick = {
                val input = text.text
                text = TextFieldValue("")
                handleInput(input, service)
            }) {
                Text("Send")
            }
        }
    }
}

@Composable
fun MessageItem(msg: MessageEntity) {
    var showDetails by remember { mutableStateOf(false) }
    val bg = if (msg.sender == "me") {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.secondaryContainer
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(MaterialTheme.shapes.medium)
            .background(bg)
            .clickable { showDetails = !showDetails }
            .padding(8.dp)
    ) {
        Text(text = "${if (msg.sender == "me") "Me" else msg.sender}: ${msg.content}")
        if (showDetails) {
            Text(
                text = java.util.Date(msg.timestamp).toString(),
                style = MaterialTheme.typography.bodySmall
            )
            if (msg.deliveryStatus != null) {
                Text(
                    text = msg.deliveryStatus,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

fun handleInput(
    input: String,
    service: BluetoothMeshService,
) {
    Log.d("ChatScreen", "User input: $input")
    when {
        input.startsWith("/msg ") -> {
            val rest = input.removePrefix("/msg ")
            val parts = rest.split(" ", limit = 2)
            if (parts.size == 2) {
                val peer = parts[0]
                val content = parts[1]
                Log.d("ChatScreen", "Sending private message to $peer: $content")
                service.sendPrivateMessage(peer, content)
            }
        }
        input.startsWith("/wipe") -> {
            Log.d("ChatScreen", "Wiping all data")
            service.wipeAllData()
        }
        else -> {
            Log.d("ChatScreen", "Sending public message: $input")
            service.sendPublicMessage(input)
        }
    }
}
