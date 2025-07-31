package com.example.bitchat.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface PeerDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(peer: PeerEntity)

    @Query("SELECT * FROM peers ORDER BY lastSeen DESC")
    fun getAll(): Flow<List<PeerEntity>>

    @Query("DELETE FROM peers")
    suspend fun wipe()
}
