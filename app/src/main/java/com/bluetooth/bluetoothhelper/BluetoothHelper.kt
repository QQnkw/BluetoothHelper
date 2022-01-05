package com.bluetooth.bluetoothhelper

import android.content.Context
import android.net.MacAddress
import android.util.Log
import com.welie.blessed.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableSharedFlow
import timber.log.Timber
import java.util.*

class BluetoothHelper private constructor(context: Context) {
    val scanDeviceFlow = MutableSharedFlow<Triple<String, String, String>>(
        0,
        Int.MAX_VALUE,
        BufferOverflow.DROP_OLDEST
    )
    val connectStateFlow = MutableSharedFlow<Pair<ConnectionState, String>>(
        0,
        Int.MAX_VALUE,
        BufferOverflow.DROP_OLDEST
    )

    fun startScanning(name: String) {
        bluetoothCentralManager.scanForPeripheralsWithNames(
            arrayOf(name)
        ) { peripheral, scanResult ->
            scanDeviceFlow.tryEmit(
                Triple(
                    peripheral.name,
                    peripheral.address,
                    scanResult.rssi.toString()
                )
            )
        }
    }

    fun stopScanning() {
        bluetoothCentralManager.stopScan()
    }

    suspend fun connectDevice(macAddress: String) {
        try {
            Timber.d("connectDevice")
            val device = bluetoothCentralManager.getPeripheral(macAddress)
            bluetoothCentralManager.connectPeripheral(device)
        } catch (connectionFailed: ConnectionFailedException) {
            Timber.d("connection failed")
            connectStateFlow.tryEmit(Pair(ConnectionState.DISCONNECTED, macAddress))
        }
    }

    fun disconnectDevice() {
        scope.launch {
            Timber.d("disconnectDevice")
            bluetoothPeripheral?.let { device ->
                bluetoothCentralManager.cancelConnection(device)
            }
        }
    }

    fun readDataFromDevice(uuidPart: String, function: (ByteArray) -> Unit) {
        scope.launch {
            bluetoothPeripheral?.let { device ->
                val uuid = uuidPart.toUUID()
                val service = device.services.find { service ->
                    val result = service.characteristics.find { characteristic ->
                        characteristic.uuid.toString() == uuid.toString()
                    }
                    result != null
                }
                service?.let {
                    val result = device.readCharacteristic(it.uuid, uuid)
                    function(result)
                }

            }
        }
    }

    fun writeDataToDevice(uuidPart: String, data: String, function: (ByteArray) -> Unit) {
        scope.launch {
            bluetoothPeripheral?.let { device ->
                val uuid = uuidPart.toUUID()
                val service = device.services.find { service ->
                    val result = service.characteristics.find { characteristic ->
                        characteristic.uuid.toString() == uuid.toString()
                    }
                    result != null
                }
                service?.let {
                    try {
                        val result = device.writeCharacteristic(
                            it.uuid,
                            uuid,
                            data.hexStringToByteArray(),
                            WriteType.WITH_RESPONSE
                        )
                        function(result)
                    } catch (ex: Exception) {
                        Timber.d(ex)
                    }
                }
            }
        }
    }

    fun setBluetoothPair(uuidPart: String, function: (ByteArray) -> Unit) {
        scope.launch {
            bluetoothPeripheral?.let { device ->
                val uuid = uuidPart.toUUID()
                val service = device.services.find { service ->
                    val result = service.characteristics.find { characteristic ->
                        characteristic.uuid.toString() == uuid.toString()
                    }
                    result != null
                }
                service?.let {
                    val addressBytes = device.address.hexStringToByteArray()
                    val result = device.writeCharacteristic(
                        it.uuid,
                        uuid,
                        byteArrayOf(
                            *("01a1".hexStringToByteArray()),
                            addressBytes[3],
                            addressBytes[4],
                            addressBytes[5]
                        ),
                        WriteType.WITH_RESPONSE
                    )
                    function(result)
                }
            }
        }
    }

    fun getDeviceAddress(): String {
        return bluetoothPeripheral?.address ?: ""
    }

    companion object {
        private var instance: BluetoothHelper? = null

        @Synchronized
        fun getInstance(context: Context): BluetoothHelper {
            if (instance == null) {
                instance = BluetoothHelper(context.applicationContext)
            }
            return requireNotNull(instance)
        }
    }

    private var bluetoothCentralManager: BluetoothCentralManager = BluetoothCentralManager(context)
    private var bluetoothPeripheral: BluetoothPeripheral? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    init {
        bluetoothCentralManager.observeConnectionState { peripheral, state ->
            bluetoothPeripheral = if (state == ConnectionState.CONNECTED) {
                peripheral.observeBondState {
                    Timber.d("Bond state is $it")
                }
                peripheral
            } else {
                null
            }
            Timber.d("ConnectionState-->1.${state.name}")
            connectStateFlow.tryEmit(Pair(state, peripheral.address))
        }
    }

}