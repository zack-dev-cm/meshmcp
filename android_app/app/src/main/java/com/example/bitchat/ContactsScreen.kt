@file:Suppress("WildcardImport")

package com.example.bitchat

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.bitchat.db.PeerEntity

@Composable
@Suppress("FunctionName")
fun ContactsScreen(service: BluetoothMeshService) {
    val peers by service.contacts.collectAsState(initial = emptyList())
    LazyColumn(modifier = Modifier.padding(16.dp)) {
        items(peers) { peer ->
            ContactRow(peer)
        }
    }
}

@Composable
@Suppress("FunctionName")
private fun ContactRow(peer: PeerEntity) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable { }
                .padding(8.dp),
    ) {
        Text(peer.peerId)
        peer.nickname?.let { Text(it) }
    }
}
