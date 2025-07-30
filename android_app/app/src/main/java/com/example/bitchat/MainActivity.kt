package com.example.bitchat

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {
    private val bluetoothService = BluetoothMeshService()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                ChatScreen(bluetoothService)
            }
        }
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
