package com.example.bitchat

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.bitchat.db.AppDatabase
import com.example.bitchat.db.ChatRepository
import com.example.bitchat.db.MessageEntity
import com.example.bitchat.db.PeerEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class ChatRepositoryTest {
    private lateinit var context: Context
    private lateinit var db: AppDatabase
    private lateinit var repository: ChatRepository

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        // Inject in-memory DB into singleton via reflection
        val field = AppDatabase::class.java.getDeclaredField("INSTANCE").apply { isAccessible = true }
        field.set(null, db)
        repository = ChatRepository(context)
    }

    @After
    fun tearDown() {
        db.close()
        val field = AppDatabase::class.java.getDeclaredField("INSTANCE").apply { isAccessible = true }
        field.set(null, null)
    }

    @Test
    fun messagesAndPeersInsertedAndRetrieved() = runBlocking {
        val peer = PeerEntity("peer1", "Alice", 123L)
        repository.savePeer(peer)
        val msg = MessageEntity(
            id = "1",
            sender = "me",
            content = "hello",
            timestamp = 1L,
            isRelay = false,
            originalSender = null,
            isPrivate = true,
            recipientNickname = null,
            senderPeerId = "peer1",
            deliveryStatus = "sending",
            retryCount = 0,
            isFavorite = false,
            delivered = false
        )
        repository.saveMessage(msg)

        val messages = repository.messages().first()
        assertEquals(1, messages.size)
        assertEquals(msg, messages.first())

        db.query("SELECT COUNT(*) FROM peers", null).use { cursor ->
            cursor.moveToFirst()
            assertEquals(1, cursor.getInt(0))
        }
    }

    @Test
    fun undeliveredForPeerReturnsOnlyPending() = runBlocking {
        val msg1 = MessageEntity("1", "me", "a", 1L, false, null, true, null, "peer1", "sending", 0, false, false)
        val msg2 = MessageEntity("2", "me", "b", 2L, false, null, true, null, "peer1", "sent", 0, false, true)
        val msg3 = MessageEntity("3", "me", "c", 3L, false, null, true, null, "peer2", "sending", 0, false, false)
        repository.saveMessage(msg1)
        repository.saveMessage(msg2)
        repository.saveMessage(msg3)

        val undelivered = repository.undeliveredForPeer("peer1")
        assertEquals(listOf(msg1), undelivered)
    }

    @Test
    fun wipeAllClearsTables() = runBlocking {
        val peer = PeerEntity("peer1", "Alice", 123L)
        repository.savePeer(peer)
        val msg = MessageEntity("1", "me", "hello", 1L, false, null, true, null, "peer1", "sending", 0, false, false)
        repository.saveMessage(msg)

        repository.wipeAll()

        val messages = repository.messages().first()
        assertEquals(0, messages.size)
        db.query("SELECT COUNT(*) FROM peers", null).use { cursor ->
            cursor.moveToFirst()
            assertEquals(0, cursor.getInt(0))
        }
    }
}
