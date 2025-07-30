package com.example.bitchat

import java.security.SecureRandom

/**
 * Manages the ephemeral peer identifier used during this app session.
 * The identifier is an 8 byte value generated from a cryptographically
 * secure random source and stored only in memory. Call [rotate] to
 * refresh the identifier at runtime.
 */
object PeerIdentityManager {
    private var currentId: ByteArray = generateId()

    /** Returns the current peer ID. */
    val peerId: ByteArray
        get() = currentId.copyOf()

    /**
     * Generates and sets a new random peer ID.
     * This can be invoked periodically to reduce linkability
     * between sessions.
     */
    @Synchronized
    fun rotate() {
        currentId = generateId()
    }

    private fun generateId(): ByteArray {
        val random = SecureRandom()
        return ByteArray(8).also { random.nextBytes(it) }
    }
}
