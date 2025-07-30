package com.example.bitchat

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class PeerIdentityManagerTest {
    @Test
    fun generatesEightByteId() {
        val id = PeerIdentityManager.peerId
        assertEquals(8, id.size)
    }

    @Test
    fun rotateChangesId() {
        val first = PeerIdentityManager.peerId
        PeerIdentityManager.rotate()
        val second = PeerIdentityManager.peerId
        assertEquals(8, second.size)
        assertFalse(first.contentEquals(second))
    }
}
