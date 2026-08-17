package com.omnidapt.simulator

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattServer
import android.bluetooth.BluetoothGattServerCallback
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothManager
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.ParcelUuid
import androidx.core.content.ContextCompat
import com.omnidapt.protocol.BleFrame
import com.omnidapt.protocol.CommandAck
import com.omnidapt.protocol.DeviceTelemetry
import com.omnidapt.protocol.LfpChunk
import com.omnidapt.protocol.ImpedanceReading
import com.omnidapt.protocol.ImpedanceSnapshot
import com.omnidapt.protocol.MessageType
import com.omnidapt.protocol.OminidaptBleProtocol
import com.omnidapt.protocol.OminidaptBleUuids
import com.omnidapt.protocol.ProtocolException
import com.omnidapt.protocol.SimulatedState
import com.omnidapt.protocol.StimulationParameters
import java.util.concurrent.CopyOnWriteArraySet
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class FaultSettings(
    val packetLossPercent: Int = 0,
    val latencyMs: Int = 0,
    val corruptNextFrame: Boolean = false,
    val rejectCommands: Boolean = false,
    val dropAcks: Boolean = false,
    val lowBattery: Boolean = false,
    val alarm: Boolean = false,
)

data class SimulatorSnapshot(
    val advertising: Boolean = false,
    val streaming: Boolean = false,
    val connectedDevices: Int = 0,
    val state: SimulatedState = SimulatedState.CONTINUOUS,
    val speed: Float = 1f,
    val sequence: Long = 0,
    val sentFrames: Long = 0,
    val droppedFrames: Long = 0,
    val lastCommand: String = "无",
    val lastError: String? = null,
    val parameters: StimulationParameters = StimulationParameters(2.0f, 130, 70, 45),
    val faults: FaultSettings = FaultSettings(),
)

class BlePeripheralController(
    private val context: Context,
    private val replaySource: ReplaySource,
) {
    private val manager = context.getSystemService(BluetoothManager::class.java)
    private val adapter get() = manager.adapter
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val devices = CopyOnWriteArraySet<BluetoothDevice>()
    private val sequence = AtomicLong(0)
    private val mutableSnapshot = MutableStateFlow(SimulatorSnapshot())
    val snapshot: StateFlow<SimulatorSnapshot> = mutableSnapshot

    private var gattServer: BluetoothGattServer? = null
    private var streamJob: Job? = null
    private lateinit var telemetryCharacteristic: BluetoothGattCharacteristic
    private lateinit var lfpCharacteristic: BluetoothGattCharacteristic
    private lateinit var ackCharacteristic: BluetoothGattCharacteristic
    private lateinit var impedanceCharacteristic: BluetoothGattCharacteristic

    private val callback = object : BluetoothGattServerCallback() {
        override fun onConnectionStateChange(device: BluetoothDevice, status: Int, newState: Int) {
            if (newState == BluetoothGatt.STATE_CONNECTED) devices.add(device) else devices.remove(device)
            mutableSnapshot.update { it.copy(connectedDevices = devices.size) }
        }

        @SuppressLint("MissingPermission")
        override fun onCharacteristicReadRequest(
            device: BluetoothDevice,
            requestId: Int,
            offset: Int,
            characteristic: BluetoothGattCharacteristic,
        ) {
            val value = when (characteristic.uuid) {
                OminidaptBleUuids.DEVICE_INFO ->
                    """{"serial":"SIM-P001-001","serialNumber":"SIM-P001-001","name":"Ominidapt Android Simulator","firmwareVersion":"2.0.0-sim","protocolVersion":2,"protocol":2,"sampleRateHz":256,"channelCount":2,"contacts":[0,1,2,3,4,5,6,7],"capabilities":["lfp","telemetry","impedance","parameters","scenario"],"safetyRuleVersion":"sim-v1","simulated":true,"clinicalUse":false}"""
                        .encodeToByteArray()
                OminidaptBleUuids.TELEMETRY -> telemetryFrame()
                OminidaptBleUuids.IMPEDANCE -> impedanceFrame()
                else -> ByteArray(0)
            }
            gattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, value)
        }

        @SuppressLint("MissingPermission")
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
                gattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, value)
            }
        }

        @SuppressLint("MissingPermission")
        override fun onCharacteristicWriteRequest(
            device: BluetoothDevice,
            requestId: Int,
            characteristic: BluetoothGattCharacteristic,
            preparedWrite: Boolean,
            responseNeeded: Boolean,
            offset: Int,
            value: ByteArray,
        ) {
            if (characteristic.uuid != OminidaptBleUuids.COMMAND) {
                gattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_REQUEST_NOT_SUPPORTED, 0, null)
                return
            }
            val frame = runCatching { OminidaptBleProtocol.decode(value) }.getOrElse {
                mutableSnapshot.update { current -> current.copy(lastError = "命令帧解析失败：${it.message}") }
                gattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_FAILURE, 0, null)
                return
            }
            if (responseNeeded) {
                gattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, null)
            }
            handleCommand(device, frame)
        }
    }

    private val advertiserCallback = object : AdvertiseCallback() {
        override fun onStartSuccess(settingsInEffect: AdvertiseSettings) {
            mutableSnapshot.update { it.copy(advertising = true, lastError = null) }
        }

        override fun onStartFailure(errorCode: Int) {
            mutableSnapshot.update { it.copy(advertising = false, lastError = "BLE广播启动失败：$errorCode") }
        }
    }

    fun hasPermissions(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        return ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_ADVERTISE) ==
            PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) ==
            PackageManager.PERMISSION_GRANTED
    }

    @SuppressLint("MissingPermission")
    fun startPeripheral() {
        if (!hasPermissions()) {
            mutableSnapshot.update { it.copy(lastError = "缺少蓝牙广播/连接权限") }
            return
        }
        if (!adapter.isEnabled) {
            mutableSnapshot.update { it.copy(lastError = "请先开启系统蓝牙") }
            return
        }
        if (!adapter.isMultipleAdvertisementSupported) {
            mutableSnapshot.update { it.copy(lastError = "本手机不支持BLE外设广播模式") }
            return
        }
        if (gattServer == null) {
            gattServer = manager.openGattServer(context, callback)
            gattServer?.addService(createService())
        }
        val settings = AdvertiseSettings.Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
            .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
            .setConnectable(true)
            .build()
        val data = AdvertiseData.Builder()
            .setIncludeDeviceName(false)
            .addServiceUuid(ParcelUuid(OminidaptBleUuids.SERVICE))
            .build()
        adapter.bluetoothLeAdvertiser?.startAdvertising(settings, data, advertiserCallback)
    }

    @SuppressLint("MissingPermission")
    fun stopPeripheral() {
        stopStreaming()
        adapter.bluetoothLeAdvertiser?.stopAdvertising(advertiserCallback)
        devices.forEach { gattServer?.cancelConnection(it) }
        devices.clear()
        gattServer?.close()
        gattServer = null
        mutableSnapshot.update { it.copy(advertising = false, connectedDevices = 0) }
    }

    fun startStreaming() {
        if (streamJob?.isActive == true) return
        mutableSnapshot.update { it.copy(streaming = true) }
        streamJob = scope.launch {
            while (isActive) {
                val current = mutableSnapshot.value
                val nextSequence = sequence.incrementAndGet() and 0xFFFF_FFFFL
                mutableSnapshot.update { it.copy(sequence = nextSequence) }
                val dropped = (nextSequence % 100) < current.faults.packetLossPercent
                if (dropped) {
                    mutableSnapshot.update { it.copy(droppedFrames = it.droppedFrames + 1) }
                } else {
                    if (current.faults.latencyMs > 0) delay(current.faults.latencyMs.toLong())
                    val samples = replaySource.next(
                        current.state,
                        OminidaptBleProtocol.LFP_SAMPLES_PER_CHUNK,
                    )
                    val payload = OminidaptBleProtocol.encodeLfpPayload(
                        LfpChunk(
                            sampleRateHz = 256,
                            channelCount = 2,
                            sampleCount = OminidaptBleProtocol.LFP_SAMPLES_PER_CHUNK,
                            state = current.state,
                            samples = samples,
                        ),
                    )
                    var frame = OminidaptBleProtocol.encode(
                        BleFrame(MessageType.LFP_DATA, nextSequence, System.currentTimeMillis(), payload),
                    )
                    if (current.faults.corruptNextFrame) {
                        frame = frame.copyOf()
                        frame[frame.lastIndex] = (frame.last().toInt() xor 0x55).toByte()
                        mutableSnapshot.update { it.copy(faults = it.faults.copy(corruptNextFrame = false)) }
                    }
                    notify(lfpCharacteristic, frame)
                    mutableSnapshot.update { it.copy(sentFrames = it.sentFrames + 1) }
                    if (nextSequence % 10L == 0L) notify(telemetryCharacteristic, telemetryFrame())
                }
                val intervalMs = (98f / current.speed.coerceIn(0.5f, 2f)).toLong().coerceAtLeast(20)
                delay(intervalMs)
            }
        }
    }

    fun stopStreaming() {
        streamJob?.cancel()
        streamJob = null
        mutableSnapshot.update { it.copy(streaming = false) }
    }

    fun setState(state: SimulatedState) {
        mutableSnapshot.update { it.copy(state = state) }
    }

    fun setSpeed(speed: Float) {
        mutableSnapshot.update { it.copy(speed = speed.coerceIn(0.5f, 2f)) }
    }

    fun setFaults(faults: FaultSettings) {
        mutableSnapshot.update { it.copy(faults = faults) }
    }

    @SuppressLint("MissingPermission")
    fun disconnectAll() {
        if (!hasPermissions()) return
        devices.forEach { gattServer?.cancelConnection(it) }
    }

    private fun createService(): BluetoothGattService {
        val service = BluetoothGattService(
            OminidaptBleUuids.SERVICE,
            BluetoothGattService.SERVICE_TYPE_PRIMARY,
        )
        service.addCharacteristic(
            BluetoothGattCharacteristic(
                OminidaptBleUuids.DEVICE_INFO,
                BluetoothGattCharacteristic.PROPERTY_READ,
                BluetoothGattCharacteristic.PERMISSION_READ,
            ),
        )
        telemetryCharacteristic = notifyCharacteristic(OminidaptBleUuids.TELEMETRY, readable = true)
        lfpCharacteristic = notifyCharacteristic(OminidaptBleUuids.LFP_STREAM)
        ackCharacteristic = notifyCharacteristic(OminidaptBleUuids.ACK)
        impedanceCharacteristic = notifyCharacteristic(OminidaptBleUuids.IMPEDANCE, readable = true)
        service.addCharacteristic(telemetryCharacteristic)
        service.addCharacteristic(lfpCharacteristic)
        service.addCharacteristic(
            BluetoothGattCharacteristic(
                OminidaptBleUuids.COMMAND,
                BluetoothGattCharacteristic.PROPERTY_WRITE,
                BluetoothGattCharacteristic.PERMISSION_WRITE,
            ),
        )
        service.addCharacteristic(ackCharacteristic)
        service.addCharacteristic(impedanceCharacteristic)
        return service
    }

    private fun notifyCharacteristic(
        uuid: java.util.UUID,
        readable: Boolean = false,
    ): BluetoothGattCharacteristic {
        val characteristic = BluetoothGattCharacteristic(
            uuid,
            BluetoothGattCharacteristic.PROPERTY_NOTIFY or
                if (readable) BluetoothGattCharacteristic.PROPERTY_READ else 0,
            if (readable) BluetoothGattCharacteristic.PERMISSION_READ else 0,
        )
        characteristic.addDescriptor(
            BluetoothGattDescriptor(
                OminidaptBleUuids.CLIENT_CONFIGURATION,
                BluetoothGattDescriptor.PERMISSION_READ or BluetoothGattDescriptor.PERMISSION_WRITE,
            ),
        )
        return characteristic
    }

    private fun telemetryFrame(): ByteArray {
        val current = mutableSnapshot.value
        val payload = OminidaptBleProtocol.encodeTelemetry(
            DeviceTelemetry(
                batteryPercent = if (current.faults.lowBattery) 8 else 86,
                streaming = current.streaming,
                alarm = current.faults.alarm,
                parameters = current.parameters,
            ),
        )
        return OminidaptBleProtocol.encode(
            BleFrame(
                MessageType.TELEMETRY,
                sequence.get() and 0xFFFF_FFFFL,
                System.currentTimeMillis(),
                payload,
            ),
        )
    }

    private fun handleCommand(device: BluetoothDevice, frame: BleFrame) {
        val current = mutableSnapshot.value
        var success = !current.faults.rejectCommands
        var statusCode = if (success) 0 else 3
        when (frame.type) {
            MessageType.SET_PARAMETERS -> {
                val parameters = runCatching {
                    OminidaptBleProtocol.decodeParameters(frame.payload)
                }.getOrElse {
                    success = false
                    statusCode = 2
                    current.parameters
                }
                val safe = parameters.currentMa in 0.5f..3.5f &&
                    parameters.frequencyHz in 90..170 &&
                    parameters.pulseWidthUs in 40..90 &&
                    parameters.dutyCycle in 20..80
                if (!safe) {
                    success = false
                    statusCode = 4
                }
                if (success) {
                    mutableSnapshot.update {
                        it.copy(
                            parameters = parameters,
                            lastCommand = "${parameters.currentMa}mA / ${parameters.frequencyHz}Hz / ${parameters.pulseWidthUs}μs",
                        )
                    }
                }
            }
            MessageType.STREAM_CONTROL -> {
                if (frame.payload.firstOrNull()?.toInt() == 1) startStreaming() else stopStreaming()
            }
            MessageType.SET_SCENARIO -> {
                val scenario = runCatching {
                    OminidaptBleProtocol.decodeScenario(frame.payload)
                }.getOrElse {
                    success = false
                    statusCode = 2
                    null
                }
                if (scenario != null && success) setState(scenario.state)
            }
            MessageType.QUERY_STATE, MessageType.HEARTBEAT -> Unit
            else -> {
                success = false
                statusCode = 1
            }
        }
        val ackPayload = OminidaptBleProtocol.encodeAck(
            CommandAck(frame.sequence, success, statusCode),
        )
        val ackFrame = OminidaptBleProtocol.encode(
            BleFrame(
                MessageType.ACK,
                sequence.incrementAndGet() and 0xFFFF_FFFFL,
                System.currentTimeMillis(),
                ackPayload,
            ),
        )
        if (current.faults.dropAcks) {
            mutableSnapshot.update { it.copy(lastError = "故障注入：本次参数 ACK 已丢弃") }
            return
        }
        scope.launch {
            if (mutableSnapshot.value.faults.latencyMs > 0) {
                delay(mutableSnapshot.value.faults.latencyMs.toLong())
            }
            notify(ackCharacteristic, ackFrame, setOf(device))
        }
    }

    private fun impedanceFrame(): ByteArray =
        OminidaptBleProtocol.encode(
            BleFrame(
                MessageType.IMPEDANCE,
                sequence.get() and 0xFFFF_FFFFL,
                System.currentTimeMillis(),
                OminidaptBleProtocol.encodeImpedance(
                    ImpedanceSnapshot(
                        measurementSequence = 0,
                        readings = listOf(
                            ImpedanceReading(6, 2, 2.35f),
                            ImpedanceReading(7, 3, 2.62f),
                        ),
                    ),
                ),
            ),
        )

    @SuppressLint("MissingPermission")
    private fun notify(
        characteristic: BluetoothGattCharacteristic,
        value: ByteArray,
        targets: Set<BluetoothDevice> = devices,
    ) {
        if (!hasPermissions()) return
        targets.forEach { device ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                gattServer?.notifyCharacteristicChanged(device, characteristic, false, value)
            } else {
                @Suppress("DEPRECATION")
                characteristic.value = value
                @Suppress("DEPRECATION")
                gattServer?.notifyCharacteristicChanged(device, characteristic, false)
            }
        }
    }
}
