@file:Suppress(
    "ktlint:standard:no-wildcard-imports",
    "ktlint:standard:backing-property-naming",
)

package com.example.bitchat

import android.Manifest
import android.bluetooth.*
import android.bluetooth.le.*
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.ParcelUuid
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.bitchat.BasicMessage
import com.example.bitchat.DeliveryAck
import com.example.bitchat.db.ChatRepository
import com.example.bitchat.db.MessageEntity
import com.example.bitchat.db.PeerEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.*
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.min

enum class PeerConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    AUTHENTICATED,
}

data class PeerConnection(
    var gatt: BluetoothGatt? = null,
    var serverConnected: Boolean = false,
    var serviceDiscoveryStarted: Boolean = false,
    var connectionRetryCount: Int = 0,
    var state: PeerConnectionState = PeerConnectionState.DISCONNECTED,
    var lastActivity: Long = System.currentTimeMillis(),
)

enum class ScanRequirement {
    FINE_LOCATION_PERMISSION,
    BLUETOOTH_SCAN_PERMISSION,
    LOCATION_ENABLED,
}

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
    private val peers = Collections.synchronizedSet(mutableSetOf<String>())
    private val _peersFlow = MutableStateFlow<List<String>>(emptyList())
    val peersFlow: StateFlow<List<String>> = _peersFlow

    private val discoveryRetentionMillis = 5 * 60 * 1000L
    private val discovered = ConcurrentHashMap<String, Long>()
    private val _discoveredFlow = MutableStateFlow<List<String>>(emptyList())
    val discoveredPeersFlow: StateFlow<List<String>> = _discoveredFlow
    private val _scanning = MutableStateFlow(false)
    val scanningFlow: StateFlow<Boolean> = _scanning
    private val _missingRequirements = MutableStateFlow<Set<ScanRequirement>>(emptySet())
    val missingRequirementsFlow: StateFlow<Set<ScanRequirement>> = _missingRequirements
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
    private val connections = ConcurrentHashMap<String, PeerConnection>()
    private val outgoingQueues = ConcurrentHashMap<String, MutableList<Pair<MessageEntity, ByteArray>>>()
    private val messageRetryCounts = ConcurrentHashMap<String, Int>()
    private val _writeFailureCounts = ConcurrentHashMap<Int, Int>()
    val writeFailureCounts: Map<Int, Int> get() = _writeFailureCounts
    private val _connectionFailureCounts = ConcurrentHashMap<Int, Int>()
    val connectionFailureCounts: Map<Int, Int> get() = _connectionFailureCounts

    private var connectionCleanupJob: Job? = null
    private val disconnectRetentionMillis = 5 * 60 * 1000L
    private val cleanupInterval = 60_000L

    private var writeRetryBaseDelay = 500L
    private var writeRetryMaxDelay = 5_000L
    private var connectionRetryBaseDelay = 1_000L
    private var connectionRetryMaxDelay = 16_000L
    private var maxConnectionRetries = 5

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
        synchronized(peers) {
            peers.clear()
            _peersFlow.value = emptyList()
        }
        synchronized(discovered) {
            discovered.clear()
            _discoveredFlow.value = emptyList()
        }
        _scanning.value = true
        _advertising.value = false
        startGattServer()
        startScanning()
        startAdvertising()
        connectionCleanupJob?.cancel()
        connectionCleanupJob =
            scope.launch {
                while (true) {
                    delay(cleanupInterval)
                    val now = System.currentTimeMillis()
                    val toRemove = mutableListOf<String>()
                    connections.forEach { (addr, conn) ->
                        if (
                            conn.state == PeerConnectionState.DISCONNECTED &&
                            now - conn.lastActivity > disconnectRetentionMillis
                        ) {
                            toRemove += addr
                        }
                    }
                    toRemove.forEach { addr ->
                        connections.remove(addr)
                        outgoingQueues.remove(addr)
                        synchronized(peers) {
                            peers.remove(addr)
                            _peersFlow.value = peers.toList()
                        }
                    }
                }
            }
    }

    fun stop() {
        Log.d("BluetoothMeshService", "stop() called")
        scanner?.stopScan(scanCallback)
        scanRestartJob?.cancel()
        advertiser?.stopAdvertising(advertiseCallback)
        gattServer?.close()
        gattServer = null
        synchronized(connections) {
            connections.values.forEach { it.gatt?.close() }
            connections.clear()
        }
        synchronized(peers) {
            peers.clear()
            _peersFlow.value = emptyList()
        }
        synchronized(discovered) {
            discovered.clear()
            _discoveredFlow.value = emptyList()
        }
        _scanning.value = false
        _advertising.value = false
        connectionCleanupJob?.cancel()
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
        val conn = connections.computeIfAbsent(peerId) { PeerConnection() }
        val firstConnection = conn.state != PeerConnectionState.AUTHENTICATED
        if (firstConnection) {
            conn.state = PeerConnectionState.AUTHENTICATED
        }
        conn.lastActivity = System.currentTimeMillis()
        Log.d("BluetoothMeshService", "Peer connected: $peerId")
        synchronized(peers) {
            peers.add(peerId)
            _peersFlow.value = peers.toList()
        }
        scope.launch {
            val nick = nickname ?: repository.getPeerNickname(peerId) ?: NicknameGenerator.generate(peerId)
            repository.savePeer(PeerEntity(peerId, nick, System.currentTimeMillis()))
            val connection = connections[peerId]
            val queue = outgoingQueues.computeIfAbsent(peerId) { Collections.synchronizedList(mutableListOf()) }

            if (firstConnection) {
                val queued = repository.undeliveredForPeer(peerId)
                queued.forEach { msg ->
                    val basic = BasicMessage(msg.id, msg.content, msg.timestamp)
                    val bytes = createPacket(peerId, basic)
                    synchronized(queue) { queue.add(msg to bytes) }
                }
            }

            // If services aren't yet discovered, the messages stay queued and
            // will be flushed from `onDescriptorWrite`.
            if (connection?.serviceDiscoveryStarted == true) {
                val items: List<Pair<MessageEntity, ByteArray>>
                synchronized(queue) {
                    items = queue.toList()
                    queue.clear()
                }
                items.forEach { (ent, data) -> writeOrQueue(peerId, ent, data) }
            }
        }
    }

    fun onPeerDisconnected(peerId: String) {
        val conn = connections[peerId] ?: return
        if (conn.serverConnected || conn.gatt != null) return
        conn.state = PeerConnectionState.DISCONNECTED
        conn.lastActivity = System.currentTimeMillis()
        Log.d("BluetoothMeshService", "Peer disconnected: $peerId")
        connections.remove(peerId)
        synchronized(peers) {
            peers.remove(peerId)
            _peersFlow.value = peers.toList()
        }
    }

    private fun startScanning() {
        Log.d("BluetoothMeshService", "startScanning() called")
        val missing = mutableSetOf<ScanRequirement>()
        val fineGranted =
            ContextCompat.checkSelfPermission(
                appContext,
                Manifest.permission.ACCESS_FINE_LOCATION,
            ) == PackageManager.PERMISSION_GRANTED
        if (!fineGranted) missing += ScanRequirement.FINE_LOCATION_PERMISSION
        val scanGranted =
            ContextCompat.checkSelfPermission(
                appContext,
                Manifest.permission.BLUETOOTH_SCAN,
            ) == PackageManager.PERMISSION_GRANTED
        if (!scanGranted) missing += ScanRequirement.BLUETOOTH_SCAN_PERMISSION
        val locationEnabled =
            (appContext.getSystemService(Context.LOCATION_SERVICE) as? LocationManager)
                ?.isLocationEnabled ?: false
        if (!locationEnabled) missing += ScanRequirement.LOCATION_ENABLED
        if (missing.isNotEmpty()) {
            Log.w("BluetoothMeshService", "Missing scan requirements: $missing")
            _scanning.value = false
            _missingRequirements.value = missing
            return
        }
        _missingRequirements.value = emptySet()
        val now = System.currentTimeMillis()
        synchronized(discovered) {
            discovered.entries.removeIf { (_, time) -> now - time > discoveryRetentionMillis }
            _discoveredFlow.value = discovered.keys.toList()
        }
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
            scanRestartJob =
                scope.launch {
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
        val descriptor =
            BluetoothGattDescriptor(
                UUID.fromString(
                    characteristicUuid.toString(),
                ),
                BluetoothGattDescriptor.PERMISSION_READ or BluetoothGattDescriptor.PERMISSION_WRITE,
            )
        characteristic.addDescriptor(descriptor)
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
                val conn = connections.computeIfAbsent(address) { PeerConnection() }
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    conn.serverConnected = true
                    conn.state = PeerConnectionState.CONNECTED
                    conn.lastActivity = System.currentTimeMillis()
                    if (conn.gatt == null) {
                        conn.gatt = device.connectGatt(appContext, false, gattClientCallback)
                    }
                    onPeerConnected(address)
                    Log.d("BluetoothMeshService", "GATT server connected: $address")
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    conn.serverConnected = false
                    conn.lastActivity = System.currentTimeMillis()
                    if (conn.gatt == null) {
                        conn.state = PeerConnectionState.DISCONNECTED
                    }
                    onPeerDisconnected(address)
                    Log.d("BluetoothMeshService", "GATT server disconnected: $address")
                }
            }

            override fun onDescriptorWriteRequest(
                device: BluetoothDevice,
                requestId: Int,
                descriptor: BluetoothGattDescriptor,
                preparedWrite: Boolean,
                responseNeeded: Boolean,
                offset: Int,
                value: ByteArray,
            ) {
                if (responseNeeded) {
                    gattServer?.sendResponse(
                        device,
                        requestId,
                        BluetoothGatt.GATT_SUCCESS,
                        offset,
                        value,
                    )
                }
                if (
                    descriptor.uuid ==
                    characteristicUuid &&
                    value.contentEquals(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)
                ) {
                    val address = device.address
                    connections[address]?.serviceDiscoveryStarted = true
                    outgoingQueues[address]?.let { queue ->
                        val serverCharacteristic =
                            gattServer
                                ?.getService(serviceUuid)
                                ?.getCharacteristic(characteristicUuid)
                        if (serverCharacteristic != null) {
                            val items: List<Pair<MessageEntity, ByteArray>>
                            synchronized(queue) {
                                items = queue.toList()
                                queue.clear()
                            }
                            items.forEach { (entity, bytes) ->
                                serverCharacteristic.value = bytes
                                val notified =
                                    gattServer?.notifyCharacteristicChanged(
                                        device,
                                        serverCharacteristic,
                                        false,
                                    ) == true
                                if (notified) {
                                    scope.launch { repository.markDelivered(entity) }
                                } else {
                                    synchronized(queue) { queue.add(entity to bytes) }
                                }
                            }
                        }
                    }
                    Log.d(
                        "BluetoothMeshService",
                        "Client ${device.address} enabled notifications",
                    )
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
                connections[device.address]?.lastActivity = System.currentTimeMillis()
                if (responseNeeded) {
                    gattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, value)
                }
                val ackBytes = handleIncomingData(device.address, value)
                ackBytes?.let { bytes ->
                    val char =
                        gattServer?.getService(serviceUuid)?.getCharacteristic(characteristicUuid)
                    char?.value = bytes
                    gattServer?.notifyCharacteristicChanged(device, char, false)
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
                val conn = connections.computeIfAbsent(address) { PeerConnection() }
                if (status != BluetoothGatt.GATT_SUCCESS || newState == BluetoothProfile.STATE_DISCONNECTED) {
                    conn.gatt = null
                    conn.serviceDiscoveryStarted = false
                    conn.lastActivity = System.currentTimeMillis()
                    if (!conn.serverConnected) {
                        conn.state = PeerConnectionState.DISCONNECTED
                    }
                    gatt.close()
                    val count = _connectionFailureCounts.merge(status, 1, Int::plus) ?: 1
                    conn.connectionRetryCount += 1
                    if (conn.connectionRetryCount < maxConnectionRetries) {
                        val delayMs =
                            min(
                                connectionRetryBaseDelay * (1L shl (conn.connectionRetryCount - 1)),
                                connectionRetryMaxDelay,
                            )
                        Log.w(
                            "BluetoothMeshService",
                            "Connection to $address failed with status $status (attempt ${conn.connectionRetryCount}, delay ${delayMs}ms, count=$count)",
                        )
                        val callback = this
                        scope.launch {
                            delay(delayMs)
                            gatt.device.connectGatt(appContext, false, callback)
                        }
                    } else {
                        Log.e(
                            "BluetoothMeshService",
                            "Connection to $address failed after ${conn.connectionRetryCount} attempts with status $status",
                        )
                        onPeerDisconnected(address)
                        outgoingQueues.remove(address)
                        conn.connectionRetryCount = 0
                    }
                    return
                }

                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    conn.connectionRetryCount = 0
                    conn.gatt = gatt
                    conn.serviceDiscoveryStarted = false
                    conn.state = PeerConnectionState.CONNECTED
                    conn.lastActivity = System.currentTimeMillis()
                    onPeerConnected(address)
                    Log.d("BluetoothMeshService", "GATT client connected: $address")
                    gatt.discoverServices()
                }
            }

            override fun onServicesDiscovered(
                gatt: BluetoothGatt,
                status: Int,
            ) {
                val characteristic =
                    gatt.getService(serviceUuid)?.getCharacteristic(characteristicUuid)
                if (characteristic != null) {
                    gatt.setCharacteristicNotification(characteristic, true)
                    characteristic.descriptors.forEach { desc ->
                        desc.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                        gatt.writeDescriptor(desc)
                    }
                    Log.d(
                        "BluetoothMeshService",
                        "Services discovered on ${gatt.device.address}",
                    )
                }
            }

            override fun onDescriptorWrite(
                gatt: BluetoothGatt,
                descriptor: BluetoothGattDescriptor,
                status: Int,
            ) {
                if (
                    descriptor.uuid ==
                    characteristicUuid &&
                    descriptor.value.contentEquals(
                        BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE,
                    )
                ) {
                    val peerId = gatt.device.address
                    connections[peerId]?.serviceDiscoveryStarted = true
                    outgoingQueues[peerId]?.let { queue ->
                        val items: List<Pair<MessageEntity, ByteArray>>
                        synchronized(queue) {
                            items = queue.toList()
                            queue.clear()
                        }
                        items.forEach { (ent, data) -> writeOrQueue(peerId, ent, data) }
                    }
                    Log.d(
                        "BluetoothMeshService",
                        "Enabled notifications for ${gatt.device.address}",
                    )
                }
            }

            override fun onCharacteristicWrite(
                gatt: BluetoothGatt,
                characteristic: BluetoothGattCharacteristic,
                status: Int,
            ) {
                connections[gatt.device.address]?.lastActivity = System.currentTimeMillis()
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    Log.d("BluetoothMeshService", "Write successful to ${gatt.device.address}")
                    val peerId = gatt.device.address
                    val queue = outgoingQueues[peerId]
                    queue?.let { q ->
                        synchronized(q) {
                            val item = q.firstOrNull()
                            if (item != null) {
                                q.removeAt(0)
                                messageRetryCounts.remove(item.first.id)
                                scope.launch { repository.markDelivered(item.first) }
                                if (q.isNotEmpty()) {
                                    val next = q.first()
                                    characteristic.value = next.second
                                    gatt.writeCharacteristic(characteristic)
                                }
                            }
                        }
                    }
                } else {
                    val count = _writeFailureCounts.merge(status, 1, Int::plus) ?: 1
                    val peerId = gatt.device.address
                    val queue = outgoingQueues[peerId]
                    queue?.let { q ->
                        synchronized(q) {
                            val item = q.firstOrNull()
                            if (item != null) {
                                q.removeAt(0)
                                q.add(0, item)
                                val msgId = item.first.id
                                val attempts = messageRetryCounts.getOrDefault(msgId, 0) + 1
                                messageRetryCounts[msgId] = attempts
                                val delayMs =
                                    min(
                                        writeRetryBaseDelay * (1L shl (attempts - 1)),
                                        writeRetryMaxDelay,
                                    )
                                Log.w(
                                    "BluetoothMeshService",
                                    "Write failed to ${gatt.device.address} with status $status (attempt $attempts, delay ${delayMs}ms, count=$count)",
                                )
                                scope.launch {
                                    delay(delayMs)
                                    characteristic.value = item.second
                                    gatt.writeCharacteristic(characteristic)
                                }
                            }
                        }
                    }
                }
            }

            override fun onCharacteristicChanged(
                gatt: BluetoothGatt,
                characteristic: BluetoothGattCharacteristic,
            ) {
                connections[gatt.device.address]?.lastActivity = System.currentTimeMillis()
                val peerId = gatt.device.address
                val ackBytes = handleIncomingData(peerId, characteristic.value)
                ackBytes?.let { bytes ->
                    val char = gatt.getService(serviceUuid)?.getCharacteristic(characteristicUuid)
                    char?.let { c ->
                        var backoff = 100L
                        val maxAttempts = 4
                        for (attempt in 1..maxAttempts) {
                            c.value = bytes
                            if (gatt.writeCharacteristic(c)) {
                                Log.d(
                                    "BluetoothMeshService",
                                    "ACK write to $peerId succeeded on attempt $attempt",
                                )
                                break
                            } else {
                                Log.w(
                                    "BluetoothMeshService",
                                    "ACK write to $peerId failed on attempt $attempt",
                                )
                                if (attempt == maxAttempts) {
                                    Log.e(
                                        "BluetoothMeshService",
                                        "Failed to write ACK to $peerId after $maxAttempts attempts",
                                    )
                                } else {
                                    try {
                                        Thread.sleep(backoff)
                                    } catch (_: InterruptedException) {
                                    }
                                    backoff *= 2
                                }
                            }
                        }
                    }
                }
            }
        }

    private fun handleIncomingData(
        peerId: String,
        value: ByteArray,
    ): ByteArray? {
        val packet = BitchatPacket.from(value)
        if (packet == null) {
            Log.w(
                "BluetoothMeshService",
                "Failed to parse packet from $peerId",
            )
            return null
        }

        return when (packet.type) {
            MessageType.MESSAGE -> {
                val msg = BasicMessage.from(packet.payload)
                val text = msg?.text ?: packet.payload.toString(Charsets.UTF_8)
                val messageId = msg?.id ?: UUID.randomUUID().toString()
                scope.launch {
                    val nick =
                        repository.getPeerNickname(peerId)
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
                }
                val ack =
                    DeliveryAck(
                        originalMessageId = messageId,
                        recipientId = myPeerId.toHex(),
                        recipientNickname = myNickname,
                        hopCount = 0,
                    )
                val ackPacket =
                    BitchatPacket(
                        type = MessageType.DELIVERY_ACK,
                        senderId = myPeerId,
                        recipientId = macToBytes(peerId),
                        payload = ack.toBytes(),
                    )
                ackPacket.toBytes()
            }

            MessageType.DELIVERY_ACK -> {
                val ack = DeliveryAck.from(packet.payload)
                ack?.let {
                    Log.d(
                        "BluetoothMeshService",
                        "Delivery ACK from $peerId for ${it.originalMessageId}",
                    )
                    scope.launch { repository.markDelivered(it.originalMessageId) }
                }
                null
            }

            else -> null
        }
    }

    private val scanCallback =
        object : ScanCallback() {
            override fun onScanResult(
                callbackType: Int,
                result: ScanResult?,
            ) {
                val device = result?.device ?: return
                val now = System.currentTimeMillis()
                var newlyDiscovered = false
                synchronized(discovered) {
                    val lastSeen = discovered[device.address]
                    if (lastSeen == null || now - lastSeen > discoveryRetentionMillis) {
                        newlyDiscovered = true
                        discovered[device.address] = now
                        _discoveredFlow.value = discovered.keys.toList()
                    } else {
                        discovered[device.address] = now
                    }
                }
                if (newlyDiscovered) {
                    Log.d(
                        "BluetoothMeshService",
                        "Discovered device ${device.address}",
                    )
                }
                val conn = connections.computeIfAbsent(device.address) { PeerConnection() }
                if (conn.gatt == null) {
                    conn.state = PeerConnectionState.CONNECTING
                    conn.lastActivity = now
                    val gatt = device.connectGatt(appContext, false, gattClientCallback)
                    conn.gatt = gatt
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
        if (_advertising.value) {
            Log.d("BluetoothMeshService", "Already advertising; skipping new start")
            return
        }
        advertiser?.stopAdvertising(advertiseCallback)
        _advertising.value = false
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
                val reason =
                    when (errorCode) {
                        AdvertiseCallback.ADVERTISE_FAILED_DATA_TOO_LARGE -> "DATA_TOO_LARGE"
                        AdvertiseCallback.ADVERTISE_FAILED_TOO_MANY_ADVERTISERS -> "TOO_MANY_ADVERTISERS"
                        AdvertiseCallback.ADVERTISE_FAILED_ALREADY_STARTED -> "ALREADY_STARTED"
                        AdvertiseCallback.ADVERTISE_FAILED_INTERNAL_ERROR -> "INTERNAL_ERROR"
                        AdvertiseCallback.ADVERTISE_FAILED_FEATURE_UNSUPPORTED -> "UNSUPPORTED"
                        else -> "UNKNOWN"
                    }
                Log.e(
                    "BluetoothMeshService",
                    "Advertising failed: $errorCode ($reason)",
                )
                advertiser?.stopAdvertising(this)
                if (errorCode == AdvertiseCallback.ADVERTISE_FAILED_DATA_TOO_LARGE && !advertiseRetryAttempted) {
                    advertiseRetryAttempted = true
                    advertiseNameLength = maxOf(4, advertiseNameLength - 2)
                    val shorter = myNickname.take(advertiseNameLength)
                    Log.w(
                        "BluetoothMeshService",
                        "Retrying advertising with shorter name: $shorter",
                    )
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

    /**
     * Attempts to write a message to a connected peer. If the peer's services
     * haven't been discovered or the characteristic is unavailable, the message
     * is queued and will be sent when `onDescriptorWrite` flushes pending
     * writes.
     */
    private fun writeOrQueue(
        peerId: String,
        entity: MessageEntity,
        bytes: ByteArray,
    ) {
        connections[peerId]?.lastActivity = System.currentTimeMillis()
        Log.d("BluetoothMeshService", "Attempting write to $peerId for ${entity.id}")
        val queue = outgoingQueues.computeIfAbsent(peerId) { Collections.synchronizedList(mutableListOf()) }
        val connection = connections[peerId]

        if (connection?.serviceDiscoveryStarted != true) {
            synchronized(queue) { queue.add(entity to bytes) }
            Log.d(
                "BluetoothMeshService",
                "Queued message ${entity.id} for $peerId (service discovery not complete)",
            )
            return
        }

        connection.gatt?.let { gatt ->
            val char = gatt.getService(serviceUuid)?.getCharacteristic(characteristicUuid)
            if (char == null) {
                synchronized(queue) { queue.add(entity to bytes) }
                Log.w(
                    "BluetoothMeshService",
                    "Characteristic null for $peerId; deferring write",
                )
                return
            }
            char.value = bytes
            if (gatt.writeCharacteristic(char)) {
                synchronized(queue) { queue.add(entity to bytes) }
            } else {
                synchronized(queue) { queue.add(entity to bytes) }
                Log.d(
                    "BluetoothMeshService",
                    "gatt.writeCharacteristic returned false for $peerId; will retry from queue",
                )
                scope.launch {
                    delay(100)
                    synchronized(queue) {
                        val next = queue.firstOrNull()
                        if (next != null) {
                            char.value = next.second
                            gatt.writeCharacteristic(char)
                        }
                    }
                }
            }
            return
        }

        val serverCharacteristic =
            gattServer?.getService(serviceUuid)?.getCharacteristic(characteristicUuid)
        if (connection?.serverConnected == true && serverCharacteristic != null) {
            val device = bluetoothAdapter?.getRemoteDevice(peerId)
            if (device != null) {
                serverCharacteristic.value = bytes
                gattServer?.notifyCharacteristicChanged(device, serverCharacteristic, false)
                scope.launch { repository.markDelivered(entity) }
                return
            }
        }

        synchronized(queue) { queue.add(entity to bytes) }
        Log.d(
            "BluetoothMeshService",
            "Queued message ${entity.id} for $peerId (connection not ready)",
        )
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
                val queue = outgoingQueues.computeIfAbsent(peerId) { Collections.synchronizedList(mutableListOf()) }
                synchronized(queue) { queue.add(entity to bytes) }
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
            synchronized(peers) {
                peers.forEach { peer ->
                    val msg = BasicMessage(entity.id, message)
                    val bytes = createPacket(peer, msg)
                    writeOrQueue(peer, entity, bytes)
                }
            }
        }
    }

    fun connectToPeer(address: String) {
        if (!isValidMac(address)) {
            Log.w("BluetoothMeshService", "Ignoring invalid address: $address")
            return
        }
        val device = bluetoothAdapter?.getRemoteDevice(address) ?: return
        val conn = connections.computeIfAbsent(address) { PeerConnection() }
        if (conn.gatt == null) {
            Log.d("BluetoothMeshService", "Attempting connection to $address")
            conn.state = PeerConnectionState.CONNECTING
            conn.lastActivity = System.currentTimeMillis()
            val gatt = device.connectGatt(appContext, false, gattClientCallback)
            conn.gatt = gatt
        }
    }

    fun connectedPeers(): List<String> = synchronized(peers) { peers.toList() }

    fun wipeAllData() {
        scope.launch { repository.wipeAll() }
        NoiseEncryptionService(appContext).wipeAll()
    }
}

private val appContext: Context
    get() = AppGlobals.appContext
