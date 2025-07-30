package com.example.bitchat.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey val id: String,
    val sender: String,
    val content: String,
    val timestamp: Long,
    val isRelay: Boolean,
    val originalSender: String?,
    val isPrivate: Boolean,
    val recipientNickname: String?,
    val senderPeerId: String?,
    val deliveryStatus: String?,
    val retryCount: Int = 0,
    val isFavorite: Boolean = false,
    val delivered: Boolean = false
)
