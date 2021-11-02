package com.kliaou.service

import android.app.Application
import android.bluetooth.*
import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.kliaou.bleresult.BleRecyclerItem
import java.util.*

private const val TAG = "BleGattService"
object BleGattService {

    // hold reference to app context to run the chat server
    private var app: Application? = null
    //gatt server
    private lateinit var bluetoothManager: BluetoothManager
    private var bluetoothGattServer: BluetoothGattServer? = null
    /* Collection of notification subscribers */
    private val _registeredDevices = mutableSetOf<BluetoothDevice>()
    val registeredDevices = _registeredDevices as LiveData<MutableSet<BluetoothDevice>>
    // LiveData for reporting the messages sent to the device
    private val _messages = MutableLiveData<BleMessage>()
    val messages = _messages as LiveData<BleMessage>
    // LiveData for reporting connection requests
    private val _connectionRequest = MutableLiveData<BluetoothDevice>()
    val connectionRequest = _connectionRequest as LiveData<BluetoothDevice>
    // LiveData for reporting the messages sent to the device
    private val _requestEnableBluetooth = MutableLiveData<Boolean>()
    val requestEnableBluetooth = _requestEnableBluetooth as LiveData<Boolean>
    /**
     * Initialize the GATT server instance with the services/characteristics
     */
    fun startGattServer() {
        //init bluetooth manager
        bluetoothManager = app?.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        //gatt server
        bluetoothGattServer = bluetoothManager.openGattServer(app, gattServerCallback)
        //name service
        bluetoothGattServer?.addService(BleGattAttributes.createNameService())
            ?: Log.w(TAG, "Unable to create GATT name server")
        //info service
        bluetoothGattServer?.addService(BleGattAttributes.createInfoService())
            ?: Log.w(TAG, "Unable to create GATT info server")
        //chat service
        bluetoothGattServer?.addService(BleGattAttributes.createChatService())
            ?: Log.w(TAG, "Unable to create GATT chat server")
    }
    /**
     * Shut down the GATT server.
     */
    fun stopGattServer() {
        bluetoothGattServer?.close()
    }
    /**
     * Send a service notification to any devices that are subscribed
     * to the characteristic.
     */
    fun notifyRegisteredDevices() {
        if (_registeredDevices.isEmpty()) {
            Log.i(TAG, "No subscribers registered")
            return
        }
        val nickname = BleGattAttributes.getNicknameByteArray()
        val location = BleGattAttributes.getLocationByteArray()
        Log.i(TAG, "Sending update to ${_registeredDevices.size} subscribers")
        for (device in _registeredDevices) {
            val nicknameCharacteristic = bluetoothGattServer
                ?.getService(UUID.fromString(BleGattAttributes.INFO_SERVICE))
                ?.getCharacteristic(UUID.fromString((BleGattAttributes.NICKNAME_CHAR)))
            nicknameCharacteristic?.value = nickname
            bluetoothGattServer?.notifyCharacteristicChanged(device, nicknameCharacteristic, false)
            val locationCharacteristic = bluetoothGattServer
                ?.getService(UUID.fromString(BleGattAttributes.INFO_SERVICE))
                ?.getCharacteristic(UUID.fromString((BleGattAttributes.LOCATION_CHAR)))
            locationCharacteristic?.value = location
            bluetoothGattServer?.notifyCharacteristicChanged(device, locationCharacteristic, false)
        }
    }
    /**
     * Callback to handle incoming requests to the GATT server.
     * All read/write requests for characteristics and descriptors are handled here.
     */
    private val gattServerCallback = object : BluetoothGattServerCallback() {
        override fun onConnectionStateChange(device: BluetoothDevice, status: Int, newState: Int) {
            super.onConnectionStateChange(device, status, newState)
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.i(TAG, "BluetoothDevice CONNECTED: $device")
                //add any device connected
                _registeredDevices.add(device)
                _connectionRequest.postValue(device)
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.i(TAG, "BluetoothDevice DISCONNECTED: $device")
                //Remove device from any active subscriptions
                _registeredDevices.remove(device)
            }
        }

        override fun onCharacteristicReadRequest(
            device: BluetoothDevice, requestId: Int, offset: Int,
            characteristic: BluetoothGattCharacteristic
        ) {
            when (characteristic.uuid) {
                UUID.fromString(BleGattAttributes.NAME_CHAR) -> {
                    Log.i(TAG, "Read Name")
                    bluetoothGattServer?.sendResponse(
                        device,
                        requestId,
                        BluetoothGatt.GATT_SUCCESS,
                        0,
                        null
                    )
                }
                UUID.fromString(BleGattAttributes.NICKNAME_CHAR) -> {
                    Log.i(TAG, "Read Nickname")
                    bluetoothGattServer?.sendResponse(
                        device,
                        requestId,
                        BluetoothGatt.GATT_SUCCESS,
                        0,
                        BleGattAttributes.getNicknameByteArray()
                    )
                }
                UUID.fromString(BleGattAttributes.LOCATION_CHAR) -> {
                    Log.i(TAG, "Read Location")
                    bluetoothGattServer?.sendResponse(
                        device,
                        requestId,
                        BluetoothGatt.GATT_SUCCESS,
                        0,
                        BleGattAttributes.getLocationByteArray()
                    )
                }
                else -> {
                    // Invalid characteristic
                    Log.w(TAG, "Invalid Characteristic Read: " + characteristic.uuid)
                    bluetoothGattServer?.sendResponse(
                        device,
                        requestId,
                        BluetoothGatt.GATT_FAILURE,
                        0,
                        null
                    )
                }
            }
        }

        override fun onCharacteristicWriteRequest(
            device: BluetoothDevice,
            requestId: Int,
            characteristic: BluetoothGattCharacteristic,
            preparedWrite: Boolean,
            responseNeeded: Boolean,
            offset: Int,
            value: ByteArray?
        ) {
            super.onCharacteristicWriteRequest(
                device,
                requestId,
                characteristic,
                preparedWrite,
                responseNeeded,
                offset,
                value
            )
            if (characteristic.uuid == UUID.fromString(BleGattAttributes.CHAT_MESSAGE_CHAR)) {
                bluetoothGattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, null)
                val message = value?.toString(Charsets.UTF_8)
                Log.d(TAG, "onCharacteristicWriteRequest: Have message: \"$message\"")
                message?.let {
                    _messages.postValue(BleMessage.RemoteMessage(it))
                }
            }
        }

        override fun onDescriptorReadRequest(
            device: BluetoothDevice, requestId: Int, offset: Int,
            descriptor: BluetoothGattDescriptor
        ) {
            if (UUID.fromString(BleGattAttributes.CLIENT_CHARACTERISTIC_NOTIFY) == descriptor.uuid) {
                Log.d(TAG, "Config descriptor read")
                val returnValue = if (_registeredDevices.contains(device)) {
                    BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                } else {
                    BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE
                }
                bluetoothGattServer?.sendResponse(
                    device,
                    requestId,
                    BluetoothGatt.GATT_SUCCESS,
                    0,
                    returnValue
                )
            } else {
                Log.w(TAG, "Unknown descriptor read request")
                bluetoothGattServer?.sendResponse(
                    device,
                    requestId,
                    BluetoothGatt.GATT_FAILURE,
                    0, null
                )
            }
        }

        override fun onDescriptorWriteRequest(
            device: BluetoothDevice, requestId: Int,
            descriptor: BluetoothGattDescriptor,
            preparedWrite: Boolean, responseNeeded: Boolean,
            offset: Int, value: ByteArray
        ) {
            if (UUID.fromString(BleGattAttributes.CLIENT_CHARACTERISTIC_NOTIFY) == descriptor.uuid) {
                if (Arrays.equals(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE, value)) {
                    Log.d(TAG, "Subscribe device to notifications: $device")
                    _registeredDevices.add(device)
                } else if (Arrays.equals(
                        BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE,
                        value
                    )
                ) {
                    Log.d(TAG, "Unsubscribe device from notifications: $device")
                    _registeredDevices.remove(device)
                }
                if (responseNeeded) {
                    bluetoothGattServer?.sendResponse(
                        device,
                        requestId,
                        BluetoothGatt.GATT_SUCCESS,
                        0, null
                    )
                }
            } else {
                Log.w(TAG, "Unknown descriptor write request")
                if (responseNeeded) {
                    bluetoothGattServer?.sendResponse(
                        device,
                        requestId,
                        BluetoothGatt.GATT_FAILURE,
                        0, null
                    )
                }
            }
        }
    }

    // Properties for current chat device connection
    private var gatt: BluetoothGatt? = null
    private var messageCharacteristic: BluetoothGattCharacteristic? = null
    fun sendMessage(message: String): Boolean {
        Log.d(TAG, "Send a message")
        messageCharacteristic?.let { characteristic ->
            characteristic.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT

            val messageBytes = message.toByteArray(Charsets.UTF_8)
            characteristic.value = messageBytes
            gatt?.let {
                val success = it.writeCharacteristic(messageCharacteristic)
                Log.d(TAG, "onServicesDiscovered: message send: $success")
                if (success) {
                    _messages.value = BleMessage.LocalMessage(message)
                }
            } ?: run {
                Log.d(TAG, "sendMessage: no gatt connection to send a message with")
            }
        }
        return false
    }

}