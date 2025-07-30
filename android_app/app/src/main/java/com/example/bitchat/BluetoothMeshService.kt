package com.example.bitchat

import android.bluetooth.*
import android.bluetooth.le.*
import android.content.Context
import android.os.ParcelUuid
import android.util.Log
import com.example.bitchat.db.ChatRepository
import com.example.bitchat.db.MessageEntity
import com.example.bitchat.db.PeerEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.*

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
    private val _peersFlow = MutableStateFlow<List<String>>(emptyList())
    val peersFlow: StateFlow<List<String>> = _peersFlow

    private val discovered = mutableSetOf<String>()
    private val _discoveredFlow = MutableStateFlow<List<String>>(emptyList())
    val discoveredPeersFlow: StateFlow<List<String>> = _discoveredFlow
    val messages: Flow<List<MessageEntity>> = repository.messages()
    private val identity = PeerIdentityManager

    /** Current ephemeral peer ID used by this service. */
    val myPeerId: ByteArray
        get() = identity.peerId

    private val bluetoothManager: BluetoothManager? =
        appContext.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    private var gattServer: BluetoothGattServer? = null
    private val connections = mutableMapOf<BluetoothDevice, BluetoothGatt?>()
    private val outgoingQueues = mutableMapOf<String, MutableList<Pair<MessageEntity, ByteArray>>>()

    private val localPeerId: ByteArray by lazy {
        val addr = bluetoothAdapter?.address ?: UUID.randomUUID().toString()
        val parts = addr.split(":")
        val id = ByteArray(8)
        for (i in parts.indices) {
            if (i >= 8) break
            val p = parts[i]
            id[i] = p.toInt(16).toByte()
        }
        id
    }

    fun start() {
        peers.clear()
        discovered.clear()
        _peersFlow.value = emptyList()
        _discoveredFlow.value = emptyList()
        startGattServer()
        startScanning()
        startAdvertising()
    }

    fun stop() {
        scanner?.stopScan(scanCallback)
        advertiser?.stopAdvertising(advertiseCallback)
        gattServer?.close()
        gattServer = null
        connections.values.forEach { it?.close() }
        connections.clear()
        peers.clear()
        discovered.clear()
        _peersFlow.value = emptyList()
        _discoveredFlow.value = emptyList()
    }

    /**
     * Force rotation of the ephemeral peer ID.
     * Callers can use this to periodically change the identifier
     * or reset it across app restarts.
     */
    fun rotatePeerId() {
        identity.rotate()
    }

    fun onPeerConnected(
        peerId: String,
        nickname: String? = null,
    ) {
        peers.add(peerId)
        _peersFlow.value = peers.toList()
        scope.launch {
            repository.savePeer(PeerEntity(peerId, nickname, System.currentTimeMillis()))
            val queued = repository.undeliveredForPeer(peerId)
            queued.forEach { msg ->
                val bytes = createPacket(peerId, msg.content)
                writeOrQueue(peerId, msg, bytes)
            }
            outgoingQueues[peerId]?.let { list ->
                val iterator = list.iterator()
                while (iterator.hasNext()) {
                    val (ent, data) = iterator.next()
                    writeOrQueue(peerId, ent, data)
                    iterator.remove()
                }
            }
        }
    }

    fun onPeerDisconnected(peerId: String) {
        peers.remove(peerId)
        _peersFlow.value = peers.toList()
    }

    private fun startScanning() {
        val filter =
            ScanFilter
                .Builder()
                .setServiceUuid(ParcelUuid(serviceUuid))
                .build()
        val settings = ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build()
        scanner?.startScan(listOf(filter), settings, scanCallback)
    }

    private fun startGattServer() {
        gattServer = bluetoothManager?.openGattServer(appContext, gattServerCallback)
        val service = BluetoothGattService(serviceUuid, BluetoothGattService.SERVICE_TYPE_PRIMARY)
        val characteristic =
            BluetoothGattCharacteristic(
                characteristicUuid,
                BluetoothGattCharacteristic.PROPERTY_WRITE or BluetoothGattCharacteristic.PROPERTY_NOTIFY,
                BluetoothGattCharacteristic.PERMISSION_WRITE,
            )
        service.addCharacteristic(characteristic)
        gattServer?.addService(service)
    }

    private val gattServerCallback =
        object : BluetoothGattServerCallback() {
            override fun onConnectionStateChange(
                device: BluetoothDevice,
                status: Int,
                newState: Int,
            ) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    connections[device] = null
                    onPeerConnected(device.address)
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    connections.remove(device)
                    onPeerDisconnected(device.address)
                }
            }

            override fun onCharacteristicWriteRequest(
                device: BluetoothDevice,
                requestId: Int,
                characteristic: BluetoothGattCharacteristic,
                preparedWrite: Boolean,
                responseNeeded: Boolean,
                offset: Int,
                value: ByteArray,
            ) {
                if (responseNeeded) {
                    gattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, value)
                }
                val packet = BitchatPacket.from(value)
                if (packet?.type == MessageType.MESSAGE) {
                    val text = packet.payload.toString(Charsets.UTF_8)
                    val sender = device.address
                    val entity = MessageEntity(
                        id = UUID.randomUUID().toString(),
                        sender = sender,
                        content = text,
                        timestamp = packet.timestamp,
                        isRelay = false,
                        originalSender = null,
                        isPrivate = packet.recipientId != null,
                        recipientNickname = null,
                        senderPeerId = sender,
                        deliveryStatus = "received",
                        retryCount = 0,
                        isFavorite = false,
                        delivered = true
                    )
                    Log.d("BluetoothMeshService", "Received message from $sender: $text")
                    scope.launch { repository.saveMessage(entity) }
                }
            }
        }

    private val gattClientCallback =
        object : BluetoothGattCallback() {
            override fun onConnectionStateChange(
                gatt: BluetoothGatt,
                status: Int,
                newState: Int,
            ) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    connections[gatt.device] = gatt
                    onPeerConnected(gatt.device.address)
                    gatt.discoverServices()
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    connections.remove(gatt.device)
                    onPeerDisconnected(gatt.device.address)
                    gatt.close()
                }
            }

            override fun onServicesDiscovered(
                gatt: BluetoothGatt,
                status: Int,
            ) {
                val characteristic = gatt.getService(serviceUuid)?.getCharacteristic(characteristicUuid)
                if (characteristic != null) {
                    gatt.setCharacteristicNotification(characteristic, true)
                    characteristic.descriptors.forEach { desc ->
                        desc.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                        gatt.writeDescriptor(desc)
                    }
                }
            }

            override fun onCharacteristicWrite(
                gatt: BluetoothGatt,
                characteristic: BluetoothGattCharacteristic,
                status: Int,
            ) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    val peerId = gatt.device.address
                    val queue = outgoingQueues[peerId]
                    val item = queue?.firstOrNull()
                    if (item != null) {
                        queue.removeAt(0)
                        scope.launch { repository.markDelivered(item.first) }
                        if (queue.isNotEmpty()) {
                            val next = queue.first()
                            characteristic.value = next.second
                            gatt.writeCharacteristic(characteristic)
                        }
                    }
                }
            }
        }

    private val scanCallback =
        object : ScanCallback() {
            override fun onScanResult(
                callbackType: Int,
                result: ScanResult?,
            ) {
                val device = result?.device ?: return
                if (discovered.add(device.address)) {
                    _discoveredFlow.value = discovered.toList()
                }
                if (!connections.containsKey(device)) {
                    val gatt = device.connectGatt(appContext, false, gattClientCallback)
                    connections[device] = gatt
                }
            }
        }

    private fun startAdvertising() {
        val settings =
            AdvertiseSettings
                .Builder()
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
                .setConnectable(true)
                .build()
        val data =
            AdvertiseData
                .Builder()
                .addServiceUuid(ParcelUuid(serviceUuid))
                .addServiceData(ParcelUuid(serviceUuid), myPeerId)
                .build()
        advertiser?.startAdvertising(settings, data, advertiseCallback)
    }

    private val advertiseCallback =
        object : AdvertiseCallback() {
            override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) {
                // Advertising started
            }

            override fun onStartFailure(errorCode: Int) {
                // handle failure
            }
        }

    private fun macToBytes(mac: String): ByteArray {
        val parts = mac.split(":")
        val bytes = ByteArray(8)
        for (i in parts.indices) {
            if (i >= 8) break
            bytes[i] = parts[i].toInt(16).toByte()
        }
        return bytes
    }

    private fun createPacket(
        peerId: String,
        text: String,
    ): ByteArray {
        val packet =
            BitchatPacket(
                type = MessageType.MESSAGE,
                senderId = localPeerId,
                recipientId = macToBytes(peerId),
                payload = text.toByteArray(),
            )
        return packet.toBytes()
    }

    private fun writeOrQueue(
        peerId: String,
        entity: MessageEntity,
        bytes: ByteArray,
    ) {
        var wrote = false
        val serverCharacteristic = gattServer?.getService(serviceUuid)?.getCharacteristic(characteristicUuid)
        connections.forEach { (device, gatt) ->
            if (device.address == peerId) {
                if (gatt != null) {
                    val char = gatt.getService(serviceUuid)?.getCharacteristic(characteristicUuid)
                    if (char != null) {
                        char.value = bytes
                        if (gatt.writeCharacteristic(char)) {
                            outgoingQueues.getOrPut(peerId) { mutableListOf() }.add(entity to bytes)
                            wrote = true
                        }
                    }
                } else if (serverCharacteristic != null) {
                    serverCharacteristic.value = bytes
                    gattServer?.notifyCharacteristicChanged(device, serverCharacteristic, false)
                    scope.launch { repository.markDelivered(entity) }
                    wrote = true
                }
            }
        }
        if (!wrote) {
            outgoingQueues.getOrPut(peerId) { mutableListOf() }.add(entity to bytes)
        }
    }

    fun sendPrivateMessage(
        peerId: String,
        message: String,
    ) {
        val entity =
            MessageEntity(
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
                delivered = peers.contains(peerId),
            )

        // Prepare BLE packet with our current peer ID in the header
        val packet =
            BitchatPacket(
                type = MessageType.MESSAGE,
                senderId = myPeerId,
                payload = message.toByteArray(),
            )
        val packetBytes = packet.toBytes() // TODO send packet over BLE

        scope.launch {
            repository.saveMessage(entity)
            val bytes = createPacket(peerId, message)
            if (peers.contains(peerId)) {
                writeOrQueue(peerId, entity, bytes)
            } else {
                outgoingQueues.getOrPut(peerId) { mutableListOf() }.add(entity to bytes)
            }
        }
    }

    fun sendPublicMessage(message: String) {
        val entity =
            MessageEntity(
                id = UUID.randomUUID().toString(),
                sender = "me",
                content = message,
                timestamp = System.currentTimeMillis(),
                isRelay = false,
                originalSender = null,
                isPrivate = false,
                recipientNickname = null,
                senderPeerId = null,
                deliveryStatus = if (peers.isEmpty()) "sending" else "sent",
                retryCount = 0,
                isFavorite = false,
                delivered = peers.isNotEmpty(),
            )

        scope.launch {
            repository.saveMessage(entity)
            peers.forEach { peer ->
                val bytes = createPacket(peer, message)
                writeOrQueue(peer, entity, bytes)
            }
        }
    }

    fun connectToPeer(address: String) {
        val device = bluetoothAdapter?.getRemoteDevice(address) ?: return
        if (!connections.containsKey(device)) {
            val gatt = device.connectGatt(appContext, false, gattClientCallback)
            connections[device] = gatt
        }
    }

    fun connectedPeers(): List<String> = peers.toList()

    fun wipeAllData() {
        scope.launch { repository.wipeAll() }
        NoiseEncryptionService(appContext).wipeAll()
    }
}

private val appContext: Context
    get() = AppGlobals.appContext
