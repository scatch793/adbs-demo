package com.omnidapt.pd.real.ble

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.ParcelUuid
import androidx.core.content.ContextCompat
import com.google.gson.Gson
import com.omnidapt.protocol.BleFrameReassembler
import com.omnidapt.protocol.BleFrame
import com.omnidapt.protocol.CommandAck
import com.omnidapt.protocol.DeviceInfo
import com.omnidapt.protocol.DeviceTelemetry
import com.omnidapt.protocol.ImpedanceSnapshot
import com.omnidapt.protocol.ImpedanceRequest
import com.omnidapt.protocol.LfpChunk
import com.omnidapt.protocol.MessageType
import com.omnidapt.protocol.OminidaptBleProtocol
import com.omnidapt.protocol.OminidaptBleUuids
import com.omnidapt.protocol.ScenarioCommand
import com.omnidapt.protocol.SimulatedState
import com.omnidapt.protocol.StimulationParameters
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class BleLinkState { IDLE, SCANNING, CONNECTING, VERIFYING, CONNECTED, RECONNECTING }

data class BleCentralSnapshot(
    val linkState: BleLinkState = BleLinkState.IDLE,
    val deviceName: String = "未连接",
    val deviceInfo: DeviceInfo? = null,
    val verifiedSimulator: Boolean = false,
    val negotiatedMtu: Int = 23,
    val batteryPercent: Int? = null,
    val alarm: Boolean = false,
    val streaming: Boolean = false,
    val parameters: StimulationParameters? = null,
    val impedance: ImpedanceSnapshot? = null,
    val simulatedState: SimulatedState? = null,
    val medicationEffectPercent: Int? = null,
    val movementIntensityPercent: Int? = null,
    val lastSequence: Long? = null,
    val receivedFrames: Long = 0,
    val lostFrames: Long = 0,
    val crcErrors: Long = 0,
    val reconnects: Int = 0,
    val lastError: String? = null,
)

/**
 * Central-side adapter for the competition simulator only. It has no UUID or
 * command path for an implanted device, and it verifies simulated=true before
 * enabling notifications or writes.
 */
class BleCentralClient(context: Context) {
    private val appContext = context.applicationContext
    private val manager = appContext.getSystemService(BluetoothManager::class.java)
    private val adapter get() = manager.adapter
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val commandSequence = AtomicLong(1)
    private val gson = Gson()
    private val reassembler = BleFrameReassembler()
    private val mutableSnapshot = MutableStateFlow(BleCentralSnapshot())
    private val mutableLfp = MutableSharedFlow<Pair<BleFrame, LfpChunk>>(extraBufferCapacity = 32)
    private val mutableAcks = MutableSharedFlow<CommandAck>(replay = 16, extraBufferCapacity = 8)
    private val mutableImpedanceMeasurements =
        MutableSharedFlow<ImpedanceSnapshot>(replay = 4, extraBufferCapacity = 4)
    private val mutableRecentSamples = MutableStateFlow(ShortArray(0))

    val snapshot: StateFlow<BleCentralSnapshot> = mutableSnapshot
    val lfp: SharedFlow<Pair<BleFrame, LfpChunk>> = mutableLfp
    val acknowledgements: SharedFlow<CommandAck> = mutableAcks
    val impedanceMeasurements: SharedFlow<ImpedanceSnapshot> = mutableImpedanceMeasurements
    val recentSamples: StateFlow<ShortArray> = mutableRecentSamples

    private var gatt: BluetoothGatt? = null
    private var keepConnected = false
    private var reconnectJob: Job? = null
    private var pendingSubscriptions = ArrayDeque<BluetoothGattCharacteristic>()
    private var servicesDiscoveryRequested = false
    private var impedanceReadRequested = false

    private val scanCallback = object : ScanCallback() {
        @SuppressLint("MissingPermission")
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            adapter.bluetoothLeScanner?.stopScan(this)
            mutableSnapshot.update {
                it.copy(linkState = BleLinkState.CONNECTING, deviceName = result.device.name ?: result.device.address)
            }
            gatt = result.device.connectGatt(appContext, false, gattCallback, BluetoothDeviceTransport.LE)
        }

        override fun onScanFailed(errorCode: Int) {
            fail("BLE 扫描失败：$errorCode", reconnect = true)
        }
    }

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS && newState == BluetoothGatt.STATE_CONNECTED) {
                mutableSnapshot.update { it.copy(linkState = BleLinkState.VERIFYING, lastError = null) }
                servicesDiscoveryRequested = false
                impedanceReadRequested = false
                reassembler.clear()
                @SuppressLint("MissingPermission")
                val requested = gatt.requestMtu(OminidaptBleProtocol.REQUESTED_MTU)
                if (!requested) discoverServicesOnce(gatt)
                scope.launch {
                    delay(2_000)
                    if (this@BleCentralClient.gatt === gatt &&
                        mutableSnapshot.value.linkState == BleLinkState.VERIFYING
                    ) {
                        discoverServicesOnce(gatt)
                    }
                }
            } else if (newState == BluetoothGatt.STATE_DISCONNECTED) {
                gatt.close()
                if (this@BleCentralClient.gatt === gatt) this@BleCentralClient.gatt = null
                if (keepConnected) scheduleReconnect("模拟器连接已断开") else {
                    mutableSnapshot.update { it.copy(linkState = BleLinkState.IDLE, verifiedSimulator = false) }
                }
            }
        }

        override fun onMtuChanged(gatt: BluetoothGatt, mtu: Int, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                mutableSnapshot.update { it.copy(negotiatedMtu = mtu) }
            }
            discoverServicesOnce(gatt)
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            val service = gatt.getService(OminidaptBleUuids.SERVICE)
            val info = service?.getCharacteristic(OminidaptBleUuids.DEVICE_INFO)
            if (status != BluetoothGatt.GATT_SUCCESS || !isSimulatorServiceComplete(service) || info == null) {
                rejectGatt(gatt, "设备没有完整的 Ominidapt 模拟服务")
                return
            }
            @SuppressLint("MissingPermission")
            gatt.readCharacteristic(info)
        }

        @Deprecated("Kept for API 24-32 callback compatibility")
        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int,
        ) {
            @Suppress("DEPRECATION")
            handleRead(gatt, characteristic, characteristic.value ?: ByteArray(0), status)
        }

        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray,
            status: Int,
        ) = handleRead(gatt, characteristic, value, status)

        @Deprecated("Kept for API 24-32 callback compatibility")
        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
            @Suppress("DEPRECATION")
            handleNotification(characteristic.uuid, characteristic.value ?: return)
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray,
        ) = handleNotification(characteristic.uuid, value)

        override fun onDescriptorWrite(gatt: BluetoothGatt, descriptor: BluetoothGattDescriptor, status: Int) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                rejectGatt(gatt, "无法订阅模拟器数据通知")
                return
            }
            subscribeNext(gatt)
        }
    }

    fun hasPermissions(): Boolean {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Manifest.permission.BLUETOOTH_SCAN
        } else {
            Manifest.permission.ACCESS_FINE_LOCATION
        }
        return ContextCompat.checkSelfPermission(appContext, permission) == PackageManager.PERMISSION_GRANTED &&
            (Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
                ContextCompat.checkSelfPermission(appContext, Manifest.permission.BLUETOOTH_CONNECT) ==
                PackageManager.PERMISSION_GRANTED)
    }

    @SuppressLint("MissingPermission")
    fun connect() {
        if (!hasPermissions()) {
            fail("缺少蓝牙扫描/连接权限")
            return
        }
        if (adapter?.isEnabled != true) {
            fail("请先开启手机蓝牙")
            return
        }
        keepConnected = true
        ContextCompat.startForegroundService(
            appContext,
            Intent(appContext, BleConnectionService::class.java),
        )
        reconnectJob?.cancel()
        gatt?.close()
        gatt = null
        mutableSnapshot.update { it.copy(linkState = BleLinkState.SCANNING, lastError = null) }
        val filter = ScanFilter.Builder().setServiceUuid(ParcelUuid(OminidaptBleUuids.SERVICE)).build()
        val settings = ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build()
        adapter.bluetoothLeScanner?.startScan(listOf(filter), settings, scanCallback)
        scope.launch {
            delay(12_000)
            if (mutableSnapshot.value.linkState == BleLinkState.SCANNING) {
                adapter.bluetoothLeScanner?.stopScan(scanCallback)
                scheduleReconnect("未发现 Ominidapt 模拟器")
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun disconnect() {
        keepConnected = false
        reconnectJob?.cancel()
        if (hasPermissions()) adapter?.bluetoothLeScanner?.stopScan(scanCallback)
        gatt?.disconnect()
        gatt?.close()
        gatt = null
        mutableSnapshot.value = BleCentralSnapshot()
        mutableRecentSamples.value = ShortArray(0)
        appContext.stopService(Intent(appContext, BleConnectionService::class.java))
    }

    fun setParameters(parameters: StimulationParameters, commandSequence: Long? = null): Long? {
        if (!mutableSnapshot.value.verifiedSimulator) {
            fail("参数写入被拒绝：未验证 simulated=true")
            return null
        }
        return writeCommand(
            MessageType.SET_PARAMETERS,
            OminidaptBleProtocol.encodeParameters(parameters),
            commandSequence,
        )
    }

    fun setScenario(command: ScenarioCommand): Long? {
        if (!mutableSnapshot.value.verifiedSimulator) {
            fail("场景切换被拒绝：未验证 simulated=true")
            return null
        }
        return writeCommand(
            MessageType.SET_SCENARIO,
            OminidaptBleProtocol.encodeScenario(command),
        )
    }

    fun measureImpedance(pairs: List<Pair<Int, Int>>): Long? {
        if (!mutableSnapshot.value.verifiedSimulator) {
            fail("阻抗测量被拒绝：未验证 simulated=true")
            return null
        }
        return writeCommand(
            MessageType.MEASURE_IMPEDANCE,
            OminidaptBleProtocol.encodeImpedanceRequest(ImpedanceRequest(pairs)),
        )
    }

    private fun startStream(): Long? = writeCommand(MessageType.STREAM_CONTROL, byteArrayOf(1))

    @SuppressLint("MissingPermission")
    private fun writeCommand(type: MessageType, payload: ByteArray, requestedSequence: Long? = null): Long? {
        val activeGatt = gatt ?: return null
        val command = activeGatt.getService(OminidaptBleUuids.SERVICE)
            ?.getCharacteristic(OminidaptBleUuids.COMMAND) ?: return null
        val sequence = requestedSequence ?: (commandSequence.getAndIncrement() and 0xFFFF_FFFFL)
        val bytes = OminidaptBleProtocol.encode(BleFrame(type, sequence, System.currentTimeMillis(), payload))
        val accepted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            activeGatt.writeCharacteristic(
                command,
                bytes,
                BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT,
            ) == 0
        } else {
            @Suppress("DEPRECATION")
            command.value = bytes
            @Suppress("DEPRECATION")
            activeGatt.writeCharacteristic(command)
        }
        return sequence.takeIf { accepted }
    }

    private fun handleRead(
        gatt: BluetoothGatt,
        characteristic: BluetoothGattCharacteristic,
        value: ByteArray,
        status: Int,
    ) {
        when (characteristic.uuid) {
            OminidaptBleUuids.DEVICE_INFO -> {
                val rawInfo = value.decodeToString()
                val info = runCatching {
                    gson.fromJson(rawInfo, DeviceInfo::class.java)
                }.getOrNull()
                if (status != BluetoothGatt.GATT_SUCCESS ||
                    info == null ||
                    !info.simulated ||
                    info.clinicalUse ||
                    info.protocolVersion != OminidaptBleProtocol.VERSION
                ) {
                    rejectGatt(
                        gatt,
                        "安全拒绝：设备信息${value.size}字节，" +
                            "解析=${info != null}，simulated=${info?.simulated}，" +
                            "clinicalUse=${info?.clinicalUse}，protocol=${info?.protocolVersion}",
                    )
                    return
                }
                mutableSnapshot.update {
                    it.copy(
                        verifiedSimulator = true,
                        deviceInfo = info,
                        deviceName = info.name,
                    )
                }
                val service = requireNotNull(gatt.getService(OminidaptBleUuids.SERVICE))
                pendingSubscriptions = ArrayDeque(
                    listOf(
                        requireNotNull(service.getCharacteristic(OminidaptBleUuids.TELEMETRY)),
                        requireNotNull(service.getCharacteristic(OminidaptBleUuids.LFP_STREAM)),
                        requireNotNull(service.getCharacteristic(OminidaptBleUuids.ACK)),
                        requireNotNull(service.getCharacteristic(OminidaptBleUuids.IMPEDANCE)),
                    ),
                )
                subscribeNext(gatt)
            }
            OminidaptBleUuids.IMPEDANCE -> {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    decodeNotificationFrame(value)?.let { frame ->
                        runCatching { OminidaptBleProtocol.decodeImpedance(frame.payload) }
                            .onSuccess { impedance ->
                                mutableSnapshot.update { it.copy(impedance = impedance) }
                                mutableImpedanceMeasurements.tryEmit(impedance)
                            }
                    }
                }
                finishConnection(gatt)
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun subscribeNext(gatt: BluetoothGatt) {
        val characteristic = pendingSubscriptions.removeFirstOrNull()
        if (characteristic == null) {
            if (!impedanceReadRequested) {
                impedanceReadRequested = true
                val impedance = gatt.getService(OminidaptBleUuids.SERVICE)
                    ?.getCharacteristic(OminidaptBleUuids.IMPEDANCE)
                if (impedance != null && gatt.readCharacteristic(impedance)) return
            }
            finishConnection(gatt)
            return
        }
        if (!gatt.setCharacteristicNotification(characteristic, true)) {
            rejectGatt(gatt, "无法启用模拟器通知")
            return
        }
        val descriptor = characteristic.getDescriptor(OminidaptBleUuids.CLIENT_CONFIGURATION)
            ?: run { rejectGatt(gatt, "模拟器通知描述符缺失"); return }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            gatt.writeDescriptor(descriptor, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)
        } else {
            @Suppress("DEPRECATION")
            descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            @Suppress("DEPRECATION")
            gatt.writeDescriptor(descriptor)
        }
    }

    private fun handleNotification(uuid: java.util.UUID, bytes: ByteArray) {
        val frame = decodeNotificationFrame(bytes) ?: return
        when (uuid) {
            OminidaptBleUuids.LFP_STREAM -> runCatching {
                OminidaptBleProtocol.decodeLfpPayload(frame.payload)
            }.onSuccess {
                appendRecentSamples(it.samples)
                mutableSnapshot.update { snapshot ->
                    snapshot.copy(simulatedState = it.state)
                }
                mutableLfp.tryEmit(frame to it)
            }
            OminidaptBleUuids.TELEMETRY -> runCatching {
                OminidaptBleProtocol.decodeTelemetry(frame.payload)
            }.onSuccess(::observeTelemetry)
            OminidaptBleUuids.ACK -> runCatching {
                OminidaptBleProtocol.decodeAck(frame.payload)
            }.onSuccess { mutableAcks.tryEmit(it) }
            OminidaptBleUuids.IMPEDANCE -> runCatching {
                OminidaptBleProtocol.decodeImpedance(frame.payload)
            }.onSuccess { impedance ->
                mutableSnapshot.update { it.copy(impedance = impedance) }
                mutableImpedanceMeasurements.tryEmit(impedance)
            }
        }
    }

    private fun decodeNotificationFrame(packet: ByteArray): BleFrame? {
        val logical = runCatching { reassembler.offer(packet) }.getOrElse {
            observeFrameError(it)
            return null
        } ?: return null
        val frame = runCatching { OminidaptBleProtocol.decode(logical) }.getOrElse {
            observeFrameError(it)
            return null
        }
        observeSequence(frame.sequence)
        return frame
    }

    private fun observeFrameError(error: Throwable) {
        mutableSnapshot.update { current ->
            current.copy(
                crcErrors = current.crcErrors + 1,
                lastError = "数据帧校验失败：${error.message}",
            )
        }
    }

    private fun appendRecentSamples(samples: ShortArray) {
        val old = mutableRecentSamples.value
        val maxValues = 256 * 2 * 10
        val combined = ShortArray(minOf(maxValues, old.size + samples.size))
        val oldToKeep = minOf(old.size, combined.size - samples.size)
        if (oldToKeep > 0) {
            old.copyInto(combined, 0, old.size - oldToKeep, old.size)
        }
        val sampleStart = combined.size - samples.size
        if (sampleStart >= 0) {
            samples.copyInto(combined, sampleStart)
        } else {
            samples.copyInto(combined, 0, -sampleStart, samples.size)
        }
        mutableRecentSamples.value = combined
    }

    private fun observeSequence(sequence: Long) {
        mutableSnapshot.update { current ->
            val previous = current.lastSequence
            val distance = if (previous == null) 1L else (sequence - previous) and 0xFFFF_FFFFL
            val lost = if (distance in 2..10_000) distance - 1 else 0
            current.copy(
                lastSequence = sequence,
                receivedFrames = current.receivedFrames + 1,
                lostFrames = current.lostFrames + lost,
            )
        }
    }

    private fun observeTelemetry(telemetry: DeviceTelemetry) {
        mutableSnapshot.update {
            it.copy(
                batteryPercent = telemetry.batteryPercent,
                alarm = telemetry.alarm,
                streaming = telemetry.streaming,
                parameters = telemetry.parameters,
                simulatedState = telemetry.simulatedState,
                medicationEffectPercent = telemetry.medicationEffectPercent,
                movementIntensityPercent = telemetry.movementIntensityPercent,
            )
        }
    }

    private fun isSimulatorServiceComplete(service: BluetoothGattService?): Boolean {
        val required = setOf(
            OminidaptBleUuids.DEVICE_INFO,
            OminidaptBleUuids.TELEMETRY,
            OminidaptBleUuids.LFP_STREAM,
            OminidaptBleUuids.COMMAND,
            OminidaptBleUuids.ACK,
            OminidaptBleUuids.IMPEDANCE,
        )
        return service != null && required.all { service.getCharacteristic(it) != null }
    }

    @SuppressLint("MissingPermission")
    private fun rejectGatt(gatt: BluetoothGatt, reason: String) {
        keepConnected = false
        mutableSnapshot.update { it.copy(linkState = BleLinkState.IDLE, verifiedSimulator = false, lastError = reason) }
        gatt.disconnect()
        gatt.close()
        if (this.gatt === gatt) this.gatt = null
    }

    private fun scheduleReconnect(reason: String) {
        if (!keepConnected) return
        mutableSnapshot.update {
            it.copy(
                linkState = BleLinkState.RECONNECTING,
                verifiedSimulator = false,
                reconnects = it.reconnects + 1,
                lastError = reason,
            )
        }
        reconnectJob?.cancel()
        reconnectJob = scope.launch {
            delay(2_000)
            if (keepConnected) connect()
        }
    }

    private fun fail(reason: String, reconnect: Boolean = false) {
        mutableSnapshot.update { it.copy(linkState = BleLinkState.IDLE, lastError = reason) }
        if (reconnect) scheduleReconnect(reason)
    }

    @SuppressLint("MissingPermission")
    private fun discoverServicesOnce(gatt: BluetoothGatt) {
        if (servicesDiscoveryRequested || this.gatt !== gatt) return
        servicesDiscoveryRequested = true
        gatt.discoverServices()
    }

    private fun finishConnection(gatt: BluetoothGatt) {
        if (this.gatt !== gatt) return
        mutableSnapshot.update { it.copy(linkState = BleLinkState.CONNECTED, lastError = null) }
        startStream()
    }
}

private object BluetoothDeviceTransport {
    const val LE: Int = 2
}
