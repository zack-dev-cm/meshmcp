@file:Suppress("WildcardImport")

package com.example.bitchat

import android.bluetooth.*
import android.bluetooth.le.*
import android.content.Context
import android.os.ParcelUuid
import android.util.Log
import com.example.bitchat.db.ChatRepository
import com.example.bitchat.db.MessageEntity
import com.example.bitchat.db.PeerEntity
import com.example.bitchat.BasicMessage
import com.example.bitchat.DeliveryAck
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import java.util.*

data class PeerConnection(
    var gatt: BluetoothGatt? = null,
    var serverConnected: Boolean = false,
)

class BluetoothMeshService {
    private val serviceUuid = UUID.fromString("F47B5E2D-4A9E-4C5A-9B3F-8E1D2C3A4B5C")
    private val characteristicUuid = UUID.fromString("A1B2C3D4-E5F6-4A5B-8C9D-0E1F2A3B4C5D")

    private val bluetoothAdapter: BluetoothAdapter?
        get() =
            (appContext.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter

    private val scanner: BluetoothLeScanner?
        get() = bluetoothAdapter?.bluetoothLeScanner

    private val advertiser: BluetoothLeAdvertiser?
        get() = bluetoothAdapter?.bluetoothLeAdvertiser

    private val repository = ChatRepository(appContext)
    private val scope = CoroutineScope(Dispatchers.IO)
    private var scanRestartJob: kotlinx.coroutines.Job? = null
    private val scanRestartInterval = 15_000L
    private val peers = mutableSetOf<String>()
    private val _peersFlow = MutableStateFlow<List<String>>(emptyList())
    val peersFlow: StateFlow<List<String>> = _peersFlow

    private val discovered = mutableSetOf<String>()
    private val _discoveredFlow = MutableStateFlow<List<String>>(emptyList())
    val discoveredPeersFlow: StateFlow<List<String>> = _discoveredFlow
    private val _scanning = MutableStateFlow(false)
    val scanningFlow: StateFlow<Boolean> = _scanning
    private val _advertising = MutableStateFlow(false)
    val advertisingFlow: StateFlow<Boolean> = _advertising
    val messages: Flow<List<MessageEntity>> = repository.messages()
    val contacts: Flow<List<PeerEntity>> = repository.peers()
    private val identity = PeerIdentityManager

    /** Current ephemeral peer ID used by this service. */
    val myPeerId: ByteArray
        get() = identity.peerId

    val myNickname: String
        get() = NicknameGenerator.generate(myPeerId.toHex())

    private val bluetoothManager: BluetoothManager? =
        appContext.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    private var gattServer: BluetoothGattServer? = null
    private val connections = mutableMapOf<String, PeerConnection>()
    private val outgoingQueues = mutableMapOf<String, MutableList<Pair<MessageEntity, ByteArray>>>()

    private var advertiseNameLength = 7
    private var advertiseRetryAttempted = false

    @Suppress("BackingPropertyNaming")
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
        Log.d("BluetoothMeshService", "start() called")
        peers.clear()
        discovered.clear()
        _peersFlow.value = emptyList()
        _discoveredFlow.value = emptyList()
        _scanning.value = true
        _advertising.value = true
        startGattServer()
        startScanning()
        startAdvertising()
    }

    fun stop() {
        Log.d("BluetoothMeshService", "stop() called")
        scanner?.stopScan(scanCallback)
        scanRestartJob?.cancel()
        advertiser?.stopAdvertising(advertiseCallback)
        gattServer?.close()
        gattServer = null
        connections.values.forEach { it.gatt?.close() }
        connections.clear()
        peers.clear()
        discovered.clear()
        _peersFlow.value = emptyList()
        _discoveredFlow.value = emptyList()
        _scanning.value = false
        _advertising.value = false
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
        Log.d("BluetoothMeshService", "Peer connected: $peerId")
        peers.add(peerId)
        _peersFlow.value = peers.toList()
        scope.launch {
            val nick = nickname ?: repository.getPeerNickname(peerId) ?: NicknameGenerator.generate(peerId)
            repository.savePeer(PeerEntity(peerId, nick, System.currentTimeMillis()))
            val queued = repository.undeliveredForPeer(peerId)
            queued.forEach { msg ->
                val basic = BasicMessage(msg.id, msg.content, msg.timestamp)
                val bytes = createPacket(peerId, basic)
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
        Log.d("BluetoothMeshService", "Peer disconnected: $peerId")
        val conn = connections[peerId]
        if (conn != null) {
            if (!conn.serverConnected && conn.gatt == null) {
                connections.remove(peerId)
            }
        }
        peers.remove(peerId)
        _peersFlow.value = peers.toList()
    }

    private fun startScanning() {
        Log.d("BluetoothMeshService", "startScanning() called")
        discovered.clear()
        _discoveredFlow.value = emptyList()
        val filter =
            ScanFilter
                .Builder()
                .setServiceUuid(ParcelUuid(serviceUuid))
                .build()
        val settings = ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build()
        val started = scanner != null
        if (started) {
            _scanning.value = true
            scanner?.startScan(listOf(filter), settings, scanCallback)
            Log.d("BluetoothMeshService", "Scanning started")
            scanRestartJob?.cancel()
            scanRestartJob = scope.launch {
                while (_scanning.value) {
                    delay(scanRestartInterval)
                    scanner?.let {
                        Log.d("BluetoothMeshService", "Restarting scan")
                        it.stopScan(scanCallback)
                        it.startScan(listOf(filter), settings, scanCallback)
                    }
                }
            }
        } else {
            _scanning.value = false
            Log.w("BluetoothMeshService", "Scanner not available")
        }
    }

    private fun startGattServer() {
        Log.d("BluetoothMeshService", "startGattServer() called")
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
                val address = device.address
                val conn = connections.getOrPut(address) { PeerConnection() }
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    conn.serverConnected = true
                    onPeerConnected(address)
                    Log.d("BluetoothMeshService", "GATT server connected: $address")
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    conn.serverConnected = false
                    onPeerDisconnected(address)
                    if (conn.gatt == null) {
                        connections.remove(address)
                    }
                    Log.d("BluetoothMeshService", "GATT server disconnected: $address")
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
                    val msg = BasicMessage.from(packet.payload)
                    val text = msg?.text ?: packet.payload.toString(Charsets.UTF_8)
                    val messageId = msg?.id ?: UUID.randomUUID().toString()
                    val peerId = device.address
                    scope.launch {
                        val nick = repository.getPeerNickname(peerId)
                            ?: NicknameGenerator.generate(peerId)
                        val entity =
                            MessageEntity(
                                id = messageId,
                                sender = nick,
                                content = text,
                                timestamp = msg?.timestamp ?: packet.timestamp,
                                isRelay = false,
                                originalSender = null,
                                isPrivate = packet.recipientId != null,
                                recipientNickname = null,
                                senderPeerId = peerId,
                                deliveryStatus = "received",
                                retryCount = 0,
                                isFavorite = false,
                                delivered = true,
                            )
                        Log.d("BluetoothMeshService", "Received message from $peerId: $text")
                        repository.savePeer(PeerEntity(peerId, nick, System.currentTimeMillis()))
                        repository.saveMessage(entity)

                        val ack = DeliveryAck(
                            originalMessageId = messageId,
                            recipientId = myPeerId.toHex(),
                            recipientNickname = myNickname,
                            hopCount = 0,
                        )
                        val ackPacket = BitchatPacket(
                            type = MessageType.DELIVERY_ACK,
                            senderId = myPeerId,
                            recipientId = macToBytes(peerId),
                            payload = ack.toBytes(),
                        )
                        val ackBytes = ackPacket.toBytes()
                        val char = gattServer?.getService(serviceUuid)?.getCharacteristic(characteristicUuid)
                        char?.value = ackBytes
                        gattServer?.notifyCharacteristicChanged(device, char, false)
                    }
                } else if (packet?.type == MessageType.DELIVERY_ACK) {
                    val ack = DeliveryAck.from(packet.payload)
                    ack?.let {
                        scope.launch { repository.markDelivered(it.originalMessageId) }
                    }
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
                val address = gatt.device.address
                val conn = connections.getOrPut(address) { PeerConnection() }
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    conn.gatt = gatt
                    onPeerConnected(address)
                    Log.d("BluetoothMeshService", "GATT client connected: $address")
                    gatt.discoverServices()
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    conn.gatt = null
                    gatt.close()
                    onPeerDisconnected(address)
                    if (!conn.serverConnected) {
                        connections.remove(address)
                    }
                    Log.d("BluetoothMeshService", "GATT client disconnected: $address")
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
                    Log.d("BluetoothMeshService", "Services discovered on ${gatt.device.address}")
                }
            }

            override fun onCharacteristicWrite(
                gatt: BluetoothGatt,
                characteristic: BluetoothGattCharacteristic,
                status: Int,
            ) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    Log.d("BluetoothMeshService", "Write successful to ${gatt.device.address}")
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
                Log.d("BluetoothMeshService", "Discovered device ${device.address}")
                if (discovered.add(device.address)) {
                    _discoveredFlow.value = discovered.toList()
                }
                val conn = connections[device.address]
                if (conn?.gatt == null) {
                    val gatt = device.connectGatt(appContext, false, gattClientCallback)
                    connections.getOrPut(device.address) { PeerConnection() }.gatt = gatt
                    Log.d("BluetoothMeshService", "Connecting to ${device.address}")
                }
            }

            override fun onScanFailed(errorCode: Int) {
                _scanning.value = false
                Log.e("BluetoothMeshService", "Scan failed: $errorCode")
            }
        }

    private fun startAdvertising() {
        Log.d("BluetoothMeshService", "startAdvertising() called")
        val advName = myNickname.take(advertiseNameLength)
        val settings =
            AdvertiseSettings
                .Builder()
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
                .setConnectable(true)
                .build()
        val data =
            AdvertiseData
                .Builder()
                .setIncludeDeviceName(true)
                .setIncludeTxPowerLevel(false)
                .addServiceUuid(ParcelUuid(serviceUuid))
                .build()
        val adv = advertiser
        bluetoothAdapter?.name = advName
        if (adv != null) {
            _advertising.value = true
            adv.startAdvertising(settings, data, advertiseCallback)
            Log.d("BluetoothMeshService", "Advertising started as $advName")
        } else {
            _advertising.value = false
            Log.w("BluetoothMeshService", "Advertiser not available")
        }
    }

    private val advertiseCallback =
        object : AdvertiseCallback() {
            override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) {
                _advertising.value = true
                Log.d("BluetoothMeshService", "Advertising started successfully")
            }

            override fun onStartFailure(errorCode: Int) {
                _advertising.value = false
                val reason = when (errorCode) {
                    AdvertiseCallback.ADVERTISE_FAILED_DATA_TOO_LARGE -> "DATA_TOO_LARGE"
                    AdvertiseCallback.ADVERTISE_FAILED_TOO_MANY_ADVERTISERS -> "TOO_MANY_ADVERTISERS"
                    AdvertiseCallback.ADVERTISE_FAILED_ALREADY_STARTED -> "ALREADY_STARTED"
                    AdvertiseCallback.ADVERTISE_FAILED_INTERNAL_ERROR -> "INTERNAL_ERROR"
                    AdvertiseCallback.ADVERTISE_FAILED_FEATURE_UNSUPPORTED -> "UNSUPPORTED"
                    else -> "UNKNOWN"
                }
                Log.e(
                    "BluetoothMeshService",
                    "Advertising failed: $errorCode ($reason)"
                )
                if (errorCode == AdvertiseCallback.ADVERTISE_FAILED_DATA_TOO_LARGE && !advertiseRetryAttempted) {
                    advertiseRetryAttempted = true
                    advertiseNameLength = maxOf(4, advertiseNameLength - 2)
                    val shorter = myNickname.take(advertiseNameLength)
                    Log.w(
                        "BluetoothMeshService",
                        "Retrying advertising with shorter name: $shorter"
                    )
                    advertiser?.stopAdvertising(this)
                    startAdvertising()
                }
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
        message: BasicMessage,
    ): ByteArray {
        val packet =
            BitchatPacket(
                type = MessageType.MESSAGE,
                senderId = myPeerId,
                recipientId = macToBytes(peerId),
                payload = message.toBytes(),
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
        val connection = connections[peerId]
        connection?.gatt?.let { gatt ->
            val char = gatt.getService(serviceUuid)?.getCharacteristic(characteristicUuid)
            if (char != null) {
                char.value = bytes
                if (gatt.writeCharacteristic(char)) {
                    outgoingQueues.getOrPut(peerId) { mutableListOf() }.add(entity to bytes)
                    wrote = true
                }
            }
        } ?: run {
            if (connection?.serverConnected == true && serverCharacteristic != null) {
                val device = bluetoothAdapter?.getRemoteDevice(peerId)
                if (device != null) {
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

        scope.launch {
            repository.saveMessage(entity)
            val msg = BasicMessage(entity.id, message)
            val bytes = createPacket(peerId, msg)
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
                val msg = BasicMessage(entity.id, message)
                val bytes = createPacket(peer, msg)
                writeOrQueue(peer, entity, bytes)
            }
        }
    }

    fun connectToPeer(address: String) {
        if (!isValidMac(address)) {
            Log.w("BluetoothMeshService", "Ignoring invalid address: $address")
            return
        }
        val device = bluetoothAdapter?.getRemoteDevice(address) ?: return
        val conn = connections[address]
        if (conn?.gatt == null) {
            Log.d("BluetoothMeshService", "Attempting connection to $address")
            val gatt = device.connectGatt(appContext, false, gattClientCallback)
            connections.getOrPut(address) { PeerConnection() }.gatt = gatt
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
