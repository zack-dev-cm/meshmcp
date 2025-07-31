package com.example.bitchat.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {
    @Query("SELECT * FROM messages ORDER BY timestamp ASC")
    fun getAll(): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE delivered = 0 AND senderPeerId = :peerId")
    suspend fun undeliveredForPeer(peerId: String): List<MessageEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(message: MessageEntity)

    @Update
    suspend fun update(message: MessageEntity)

    @Query("UPDATE messages SET delivered = 1, deliveryStatus = 'delivered' WHERE id = :id")
    suspend fun markDelivered(id: String)

    @Query("DELETE FROM messages")
    suspend fun wipe()
}
