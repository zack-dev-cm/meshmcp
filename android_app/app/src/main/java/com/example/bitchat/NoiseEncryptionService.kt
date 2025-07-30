package com.example.bitchat

import android.content.Context
import android.util.Base64
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.southernstorm.noise.protocol.CipherState
import com.southernstorm.noise.protocol.CipherStatePair
import com.southernstorm.noise.protocol.HandshakeState
import com.southernstorm.noise.protocol.Noise
import java.nio.ByteBuffer
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Basic Noise XX encryption service.
 * Generates Curve25519 keys and performs the XX handshake over BLE.
 */
class NoiseEncryptionService(context: Context) {
    enum class EncryptionStatus { NONE, NO_HANDSHAKE, NOISE_HANDSHAKING, NOISE_SECURED }

    private data class Session(
        val sessionId: String,
        val handshake: HandshakeState,
        var sendCipher: CipherState? = null,
        var recvCipher: CipherState? = null,
        var status: EncryptionStatus = EncryptionStatus.NOISE_HANDSHAKING
    )

    private val prefs = createPrefs(context)
    private val staticPrivate: ByteArray
    val staticPublic: ByteArray
    private val sessions = ConcurrentHashMap<String, Session>()

    init {
        val dh = Noise.createDH("25519")
        val saved = prefs.getString("static_key", null)
        if (saved != null) {
            val priv = Base64.decode(saved, Base64.NO_WRAP)
            dh.setPrivateKey(priv, 0)
            dh.generatePublicKey()
        } else {
            dh.generateKeyPair()
            val priv = ByteArray(dh.privateKeyLength)
            dh.getPrivateKey(priv, 0)
            prefs.edit().putString("static_key", Base64.encodeToString(priv, Base64.NO_WRAP)).apply()
        }
        staticPrivate = ByteArray(dh.privateKeyLength)
        dh.getPrivateKey(staticPrivate, 0)
        staticPublic = ByteArray(dh.publicKeyLength)
        dh.getPublicKey(staticPublic, 0)
    }

    private fun createPrefs(context: Context): EncryptedSharedPreferences {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        return EncryptedSharedPreferences.create(
            context,
            "noise_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun initiateHandshake(peerId: String): NoiseMessage {
        val hs = createHandshake(HandshakeState.INITIATOR)
        hs.start()
        val msg = ByteArray(Noise.MAX_PACKET_LEN)
        val len = hs.writeMessage(msg, 0, null, 0, 0)
        val sessionId = UUID.randomUUID().toString()
        sessions[peerId] = Session(sessionId, hs)
        return NoiseMessage(NoiseMessageType.HANDSHAKE_INITIATION, sessionId, msg.copyOf(len))
    }

    fun receiveHandshakeMessage(peerId: String, message: NoiseMessage): NoiseMessage? {
        var session = sessions[peerId]
        if (session == null) {
            val hs = createHandshake(HandshakeState.RESPONDER)
            hs.start()
            session = Session(message.sessionId, hs)
            sessions[peerId] = session
        }
        val hs = session.handshake
        val buffer = ByteArray(Noise.MAX_PACKET_LEN)
        hs.readMessage(message.payload, 0, message.payload.size, null, 0)
        if (hs.action == HandshakeState.WRITE_MESSAGE) {
            val len = hs.writeMessage(buffer, 0, null, 0, 0)
            if (hs.action == HandshakeState.SPLIT || hs.action == HandshakeState.COMPLETE) {
                val pair = hs.split()
                session.sendCipher = pair.sender
                session.recvCipher = pair.receiver
                session.status = EncryptionStatus.NOISE_SECURED
                return NoiseMessage(NoiseMessageType.HANDSHAKE_FINAL, session.sessionId, buffer.copyOf(len))
            }
            return NoiseMessage(NoiseMessageType.HANDSHAKE_RESPONSE, session.sessionId, buffer.copyOf(len))
        } else if (hs.action == HandshakeState.SPLIT || hs.action == HandshakeState.COMPLETE) {
            val pair: CipherStatePair = hs.split()
            session.sendCipher = pair.sender
            session.recvCipher = pair.receiver
            session.status = EncryptionStatus.NOISE_SECURED
        }
        return null
    }

    fun encrypt(peerId: String, plaintext: ByteArray): ByteArray {
        val session = sessions[peerId] ?: throw IllegalStateException("No session")
        val cipher = session.sendCipher ?: throw IllegalStateException("Handshake not finished")
        val out = ByteArray(plaintext.size + cipher.macLength)
        val len = cipher.encryptWithAd(null, plaintext, 0, out, 0, plaintext.size)
        return out.copyOf(len)
    }

    fun decrypt(peerId: String, ciphertext: ByteArray): ByteArray {
        val session = sessions[peerId] ?: throw IllegalStateException("No session")
        val cipher = session.recvCipher ?: throw IllegalStateException("Handshake not finished")
        val out = ByteArray(ciphertext.size)
        val len = cipher.decryptWithAd(null, ciphertext, 0, out, 0, ciphertext.size)
        return out.copyOf(len)
    }

    fun status(peerId: String): EncryptionStatus {
        return sessions[peerId]?.status ?: EncryptionStatus.NO_HANDSHAKE
    }

    fun wipeAll() {
        prefs.edit().clear().apply()
        sessions.clear()
    }

    private fun createHandshake(role: Int): HandshakeState {
        val hs = HandshakeState("Noise_XX_25519_ChaChaPoly_SHA256", role)
        val local = Noise.createDH("25519")
        local.setPrivateKey(staticPrivate, 0)
        hs.localKeyPair.copyFrom(local)
        return hs
    }
}

enum class NoiseMessageType(val id: Byte) {
    HANDSHAKE_INITIATION(0x10),
    HANDSHAKE_RESPONSE(0x11),
    HANDSHAKE_FINAL(0x12),
    ENCRYPTED_MESSAGE(0x13),
    SESSION_RENEGOTIATION(0x14)
}

data class NoiseMessage(
    val type: NoiseMessageType,
    val sessionId: String,
    val payload: ByteArray
) {
    fun toBytes(): ByteArray {
        val id = UUID.fromString(sessionId)
        val buffer = ByteBuffer.allocate(1 + 16 + 4 + payload.size)
        buffer.put(type.id)
        buffer.putLong(id.mostSignificantBits)
        buffer.putLong(id.leastSignificantBits)
        buffer.putInt(payload.size)
        buffer.put(payload)
        return buffer.array()
    }

    companion object {
        fun from(data: ByteArray): NoiseMessage? {
            if (data.size < 21) return null
            val buffer = ByteBuffer.wrap(data)
            val type = buffer.get()
            val msb = buffer.long
            val lsb = buffer.long
            val size = buffer.int
            if (size < 0 || size > buffer.remaining()) return null
            val payload = ByteArray(size)
            buffer.get(payload)
            val mt = NoiseMessageType.values().firstOrNull { it.id == type } ?: return null
            return NoiseMessage(mt, UUID(msb, lsb).toString(), payload)
        }
    }
}
