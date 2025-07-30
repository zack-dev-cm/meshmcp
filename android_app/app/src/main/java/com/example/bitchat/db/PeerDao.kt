package com.example.bitchat.db

import androidx.room.*

@Dao
interface PeerDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(peer: PeerEntity)

    @Query("DELETE FROM peers")
    suspend fun wipe()
}
