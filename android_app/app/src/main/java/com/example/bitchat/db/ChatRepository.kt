package com.example.bitchat.db

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ChatRepository(context: Context) {
    private val db = AppDatabase.get(context)
    private val messageDao = db.messageDao()
    private val peerDao = db.peerDao()

    fun messages() = messageDao.getAll()

    suspend fun saveMessage(entity: MessageEntity) = withContext(Dispatchers.IO) {
        messageDao.insert(entity)
    }

    suspend fun markDelivered(entity: MessageEntity) = withContext(Dispatchers.IO) {
        messageDao.update(entity.copy(delivered = true, deliveryStatus = "sent"))
    }

    suspend fun undeliveredForPeer(peerId: String) = withContext(Dispatchers.IO) {
        messageDao.undeliveredForPeer(peerId)
    }

    suspend fun savePeer(peer: PeerEntity) = withContext(Dispatchers.IO) {
        peerDao.upsert(peer)
    }

    suspend fun wipeAll() = withContext(Dispatchers.IO) {
        messageDao.wipe()
        peerDao.wipe()
        db.clearAllTables()
    }
}
