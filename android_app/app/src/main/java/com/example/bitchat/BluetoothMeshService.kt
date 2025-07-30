package com.example.bitchat

import android.bluetooth.le.*
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.content.Context
import android.os.ParcelUuid
import java.util.*
import com.example.bitchat.db.ChatRepository
import com.example.bitchat.db.MessageEntity
import com.example.bitchat.db.PeerEntity
import com.example.bitchat.NoiseEncryptionService
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

    private val connections = mutableMapOf<String, BluetoothGatt>()
    private val pendingDeliveries = mutableMapOf<String, MutableList<Pair<MessageEntity, BitchatPacket>>>()
    private val queuedPackets = mutableMapOf<String, MutableList<Pair<MessageEntity, BitchatPacket>>>()

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onCharacteristicWrite(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
            val peerId = gatt.device.address
            val list = pendingDeliveries[peerId] ?: return
            if (list.isNotEmpty()) {
                val (entity, packet) = list.removeAt(0)
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    scope.launch { repository.markDelivered(entity) }
                } else {
                    queuedPackets.getOrPut(peerId) { mutableListOf() }.add(entity to packet)
                }
                if (list.isEmpty()) pendingDeliveries.remove(peerId)
            }
            flushQueue(peerId)
        }
    }

    fun start() {
        startScanning()
        startAdvertising()
    }

    fun stop() {
        scanner?.stopScan(scanCallback)
        advertiser?.stopAdvertising(advertiseCallback)
    }

    fun onPeerConnected(peerId: String, nickname: String? = null) {
        peers.add(peerId)
        flushQueue(peerId)
        scope.launch {
            repository.savePeer(PeerEntity(peerId, nickname, System.currentTimeMillis()))
            val queued = repository.undeliveredForPeer(peerId)
            queued.forEach { msg ->
                val packet = BitchatPacket(
                    MessageType.MESSAGE,
                    noiseService.staticPublic.copyOfRange(0, 8),
                    msg.content.toByteArray()
                )
                sendPacket(peerId, msg, packet)
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
                val packet = BitchatPacket(
                    MessageType.MESSAGE,
                    noiseService.staticPublic.copyOfRange(0, 8),
                    message.toByteArray()
                )
                sendPacket(peerId, entity, packet)
            } else {
                queueForPeer(peerId, entity, BitchatPacket(MessageType.MESSAGE, noiseService.staticPublic.copyOfRange(0, 8), message.toByteArray()))
            }
        }
    }

    private fun sendPacket(peerId: String, entity: MessageEntity, packet: BitchatPacket) {
        val gatt = connections[peerId]
        val characteristic = gatt?.getService(serviceUuid)?.getCharacteristic(characteristicUuid)
        if (gatt != null && characteristic != null) {
            characteristic.value = packet.toBytes()
            characteristic.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
            if (gatt.writeCharacteristic(characteristic)) {
                pendingDeliveries.getOrPut(peerId) { mutableListOf() }.add(entity to packet)
            } else {
                queueForPeer(peerId, entity, packet)
            }
        } else {
            queueForPeer(peerId, entity, packet)
        }
    }

    private fun queueForPeer(peerId: String, entity: MessageEntity, packet: BitchatPacket) {
        queuedPackets.getOrPut(peerId) { mutableListOf() }.add(entity to packet)
    }

    private fun flushQueue(peerId: String) {
        val gatt = connections[peerId] ?: return
        val queue = queuedPackets[peerId] ?: return
        if (pendingDeliveries[peerId]?.isNotEmpty() == true) return

        while (queue.isNotEmpty()) {
            val (entity, packet) = queue.first()
            val characteristic = gatt.getService(serviceUuid)?.getCharacteristic(characteristicUuid)
            if (characteristic == null) break
            characteristic.value = packet.toBytes()
            characteristic.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
            if (gatt.writeCharacteristic(characteristic)) {
                pendingDeliveries.getOrPut(peerId) { mutableListOf() }.add(entity to packet)
                queue.removeAt(0)
            } else {
                break
            }
        }
        if (queue.isEmpty()) queuedPackets.remove(peerId)
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
