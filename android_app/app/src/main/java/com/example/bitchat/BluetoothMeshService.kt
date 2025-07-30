package com.example.bitchat

import android.bluetooth.le.*
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.os.ParcelUuid
import java.util.*
import com.example.bitchat.db.ChatRepository
import com.example.bitchat.db.MessageEntity
import com.example.bitchat.db.PeerEntity
import com.example.bitchat.NoiseEncryptionService
import com.example.bitchat.NoiseMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BluetoothMeshService {
    private val serviceUuid = UUID.fromString("F47B5E2D-4A9E-4C5A-9B3F-8E1D2C3A4B5C")
    private val characteristicUuid = UUID.fromString("A1B2C3D4-E5F6-4A5B-8C9D-0E1F2A3B4C5D")

    private val bluetoothAdapter: BluetoothAdapter? =
        (appContext.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter

    private val scanner: BluetoothLeScanner? = bluetoothAdapter?.bluetoothLeScanner
    private val advertiser: BluetoothLeAdvertiser? = bluetoothAdapter?.bluetoothLeAdvertiser

    private val repository = ChatRepository(appContext)
    private val scope = CoroutineScope(Dispatchers.IO)
    private val peers = mutableSetOf<String>()
    private val noiseService = NoiseEncryptionService(appContext)
    private val sessionMap = mutableMapOf<String, String>()

    var packetSender: ((String, ByteArray) -> Unit)? = null

    fun start() {
        startScanning()
        startAdvertising()
    }

    fun stop() {
        scanner?.stopScan(scanCallback)
        advertiser?.stopAdvertising(advertiseCallback)
    }

    fun onBlePacketReceived(peerId: String, data: ByteArray) {
        val message = NoiseMessage.from(data) ?: return
        val response = noiseService.receiveHandshakeMessage(peerId, message)
        response?.let {
            sessionMap[peerId] = it.sessionId
            packetSender?.invoke(peerId, it.toBytes())
        }
    }

    fun onPeerConnected(peerId: String, nickname: String? = null) {
        peers.add(peerId)
        scope.launch {
            repository.savePeer(PeerEntity(peerId, nickname, System.currentTimeMillis()))
            val queued = repository.undeliveredForPeer(peerId)
            queued.forEach { msg ->
                // Messages queued before handshake will be sent after we secure the channel
                repository.markDelivered(msg)
            }
        }
        initiateNoiseHandshake(peerId)
    }

    fun onPeerDisconnected(peerId: String) {
        peers.remove(peerId)
    }

    private fun initiateNoiseHandshake(peerId: String) {
        val msg = noiseService.initiateHandshake(peerId)
        sessionMap[peerId] = msg.sessionId
        packetSender?.invoke(peerId, msg.toBytes())
    }

    private fun startScanning() {
        val filter = ScanFilter.Builder()
            .setServiceUuid(ParcelUuid(serviceUuid))
            .build()
        val settings = ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build()
        scanner?.startScan(listOf(filter), settings, scanCallback)
    }

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            // handle incoming packets
        }
    }

    private fun startAdvertising() {
        val settings = AdvertiseSettings.Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
            .setConnectable(true)
            .build()
        val data = AdvertiseData.Builder()
            .addServiceUuid(ParcelUuid(serviceUuid))
            .build()
        advertiser?.startAdvertising(settings, data, advertiseCallback)
    }

    private val advertiseCallback = object : AdvertiseCallback() {
        override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) {
            // Advertising started
        }

        override fun onStartFailure(errorCode: Int) {
            // handle failure
        }
    }

    fun sendMessage(peerId: String, message: String) {
        val entity = MessageEntity(
            id = UUID.randomUUID().toString(),
            sender = "me",
            content = message,
            timestamp = System.currentTimeMillis(),
            isRelay = false,
            originalSender = null,
            isPrivate = true,
            recipientNickname = null,
            senderPeerId = peerId,
            deliveryStatus = if (peers.contains(peerId)) "sent" else "sending",
            retryCount = 0,
            isFavorite = false,
            delivered = peers.contains(peerId)
        )

        scope.launch {
            repository.saveMessage(entity)
            if (peers.contains(peerId)) {
                val payload = if (noiseService.status(peerId) == NoiseEncryptionService.EncryptionStatus.NOISE_SECURED) {
                    noiseService.encrypt(peerId, message.toByteArray())
                } else {
                    message.toByteArray()
                }
                packetSender?.invoke(peerId, payload)
                repository.markDelivered(entity)
            }
        }
    }

    fun connectedPeers(): List<String> {
        return peers.toList()
    }

    fun wipeAllData() {
        scope.launch { repository.wipeAll() }
        NoiseEncryptionService(appContext).wipeAll()
    }

    fun encryptionStatus(peerId: String): NoiseEncryptionService.EncryptionStatus {
        return noiseService.status(peerId)
    }

    fun decryptMessage(peerId: String, data: ByteArray): ByteArray {
        return noiseService.decrypt(peerId, data)
    }

    fun encryptMessage(peerId: String, data: ByteArray): ByteArray {
        return noiseService.encrypt(peerId, data)
    }

    fun sessionIdForPeer(peerId: String): String? = sessionMap[peerId]
}

private val appContext: Context
    get() = AppGlobals.appContext
