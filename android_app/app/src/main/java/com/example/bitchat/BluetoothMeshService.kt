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
import com.example.bitchat.PeerIdentityManager
import com.example.bitchat.BitchatPacket
import com.example.bitchat.MessageType
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
    private val identity = PeerIdentityManager

    /** Current ephemeral peer ID used by this service. */
    val myPeerId: ByteArray
        get() = identity.peerId

    fun start() {
        startScanning()
        startAdvertising()
    }

    fun stop() {
        scanner?.stopScan(scanCallback)
        advertiser?.stopAdvertising(advertiseCallback)
    }

    /**
     * Force rotation of the ephemeral peer ID.
     * Callers can use this to periodically change the identifier
     * or reset it across app restarts.
     */
    fun rotatePeerId() {
        identity.rotate()
    }

    fun onPeerConnected(peerId: String, nickname: String? = null) {
        peers.add(peerId)
        scope.launch {
            repository.savePeer(PeerEntity(peerId, nickname, System.currentTimeMillis()))
            val queued = repository.undeliveredForPeer(peerId)
            queued.forEach { msg ->
                // TODO send via BLE
                repository.markDelivered(msg)
            }
        }
    }

    fun onPeerDisconnected(peerId: String) {
        peers.remove(peerId)
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
            .addServiceData(ParcelUuid(serviceUuid), myPeerId)
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

        // Prepare BLE packet with our current peer ID in the header
        val packet = BitchatPacket(
            MessageType.MESSAGE,
            myPeerId,
            message.toByteArray()
        )
        val packetBytes = packet.toBytes() // TODO send packet over BLE

        scope.launch {
            repository.saveMessage(entity)
            if (peers.contains(peerId)) {
                // TODO send via BLE
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
}

private val appContext: Context
    get() = AppGlobals.appContext
