@file:Suppress("WildcardImport")

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
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
            val list =
                mutableListOf(
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_ADVERTISE,
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
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
                meshService?.let { BitchatApp(it.bluetoothService) }
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
@Suppress("FunctionName")
fun BitchatApp(service: BluetoothMeshService) {
    var selected by remember { mutableStateOf(0) }
    var pendingPeer by remember { mutableStateOf<String?>(null) }
    val screens = listOf("Chat", "Broadcast", "Contacts")
    Scaffold(
        bottomBar = {
            NavigationBar {
                screens.forEachIndexed { index, title ->
                    NavigationBarItem(
                        selected = selected == index,
                        onClick = { selected = index },
                        label = { Text(title) },
                        icon = {},
                    )
                }
            }
        },
    ) { inner ->
        when (selected) {
            0 -> PrivateChatScreen(service, Modifier.padding(inner), pendingPeer)
            1 -> PublicChatScreen(service, Modifier.padding(inner))
            else -> ContactsScreen(service) { peerId ->
                pendingPeer = peerId
                service.connectToPeer(peerId)
                selected = 0
            }
        }
    }
}

@Composable
fun PrivateChatScreen(
    service: BluetoothMeshService,
    modifier: Modifier = Modifier,
    initialTarget: String? = null,
) {
    var text by remember { mutableStateOf(TextFieldValue("")) }
    val messages by service.messages.collectAsState(initial = emptyList())
    val peers by service.peersFlow.collectAsState(initial = emptyList())
    val discovered by service.discoveredPeersFlow.collectAsState(initial = emptyList())
    val scanning by service.scanningFlow.collectAsState(initial = false)
    val advertising by service.advertisingFlow.collectAsState(initial = false)
    var target by remember { mutableStateOf<String?>(initialTarget) }

    LaunchedEffect(initialTarget) {
        initialTarget?.let { target = it }
    }
    LaunchedEffect(peers) {
        if (target == null && peers.isNotEmpty()) target = peers.first()
    }

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        Text("Connected peers: " + peers.joinToString(", "))
        target?.let { Text("Chatting with $it") }
        Row(Modifier.padding(vertical = 4.dp)) {
            peers.forEach { peer ->
                val selected = peer == target
                Text(
                    peer,
                    modifier =
                        Modifier
                            .padding(end = 8.dp)
                            .clip(MaterialTheme.shapes.small)
                            .background(
                                if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                            ).clickable { target = peer }
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                )
            }
        }
        Text(if (scanning) "Discovering peers..." else "Discovery stopped")
        Text(if (advertising) "Advertising as ${service.myNickname}" else "Not advertising")
        Spacer(Modifier.height(8.dp))
        if (discovered.isNotEmpty()) {
            Text("Available peers:")
            LazyColumn(modifier = Modifier.height(100.dp)) {
                items(discovered) { addr ->
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .clickable { service.connectToPeer(addr) }
                                .padding(4.dp),
                    ) {
                        Text(addr)
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        val filtered =
            messages.filter { msg ->
                val p = target
                if (p == null) false else (msg.sender == p || msg.senderPeerId == p)
            }
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(filtered) { msg ->
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
                target?.let { service.sendPrivateMessage(it, input) }
            }, enabled = target != null) {
                Text("Send")
            }
        }
    }
}

@Composable
fun PublicChatScreen(
    service: BluetoothMeshService,
    modifier: Modifier = Modifier,
) {
    var text by remember { mutableStateOf(TextFieldValue("")) }
    val messages by service.messages.collectAsState(initial = emptyList())
    val peers by service.peersFlow.collectAsState(initial = emptyList())
    val discovered by service.discoveredPeersFlow.collectAsState(initial = emptyList())
    val scanning by service.scanningFlow.collectAsState(initial = false)
    val advertising by service.advertisingFlow.collectAsState(initial = false)

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        Text("Peers: " + peers.joinToString(", "))
        Text(if (scanning) "Discovering peers..." else "Discovery stopped")
        Text(if (advertising) "Advertising as ${service.myNickname}" else "Not advertising")
        Spacer(Modifier.height(8.dp))
        if (discovered.isNotEmpty()) {
            Text("Available peers:")
            LazyColumn(modifier = Modifier.height(100.dp)) {
                items(discovered) { addr ->
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .clickable { service.connectToPeer(addr) }
                                .padding(4.dp),
                    ) {
                        Text(addr)
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Text("Tap a peer above to connect. Use '/msg <peer> <text>' for private messages or type below for public chat.")
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
                handlePublicInput(input, service)
            }) {
                Text("Send")
            }
        }
    }
}

@Composable
fun MessageItem(msg: MessageEntity) {
    var showDetails by remember { mutableStateOf(false) }
    val bg =
        if (msg.sender == "me") {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.secondaryContainer
        }
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .clip(MaterialTheme.shapes.medium)
                .background(bg)
                .clickable { showDetails = !showDetails }
                .padding(8.dp),
    ) {
        val from = if (msg.sender == "me") "Me" else msg.sender
        val to = if (msg.sender == "me") msg.senderPeerId ?: "public" else "Me"
        Text(text = "$from → $to: ${msg.content}")
        if (showDetails) {
            Text(
                text = java.util.Date(msg.timestamp).toString(),
                style = MaterialTheme.typography.bodySmall,
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

fun handlePublicInput(
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
