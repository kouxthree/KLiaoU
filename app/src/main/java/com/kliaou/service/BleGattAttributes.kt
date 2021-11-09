package com.kliaou.service

import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattService
import com.kliaou.ui.BleHomeActivity
import java.util.*
import kotlin.collections.HashMap

class BleGattAttributes {
    companion object {
        //service
        const val NAME_SERVICE = "00001800-0000-1000-8000-00805f9b34fb"
        const val INFO_SERVICE = "0000180a-0000-1000-8000-00805f9b34fb"
        const val CHAT_SERVICE = "0000b81d-0000-1000-8000-00805f9b34fb"
        //characteristics
        const val NAME_CHAR = "00002a00-0000-1000-8000-00805f9b34fb"
        const val NICKNAME_CHAR = "00002af8-0000-1000-8000-00805f9b34fb"
        const val LOCATION_CHAR = "00002af7-0000-1000-8000-00805f9b34fb"
        const val CHAT_MESSAGE_CHAR = "7db3e235-3608-41f3-a03c-955fcbd2ea4b"
        const val CHAT_CONFIRM_CHAR = "36d4dc5c-814b-4097-a5a6-b93b39085928"
        //val MANUFACTURER_NAME_STRING = "00002a29-0000-1000-8000-00805f9b34fb"
        //val HEART_RATE_MEASUREMENT = "00002a37-0000-1000-8000-00805f9b34fb"
        const val CLIENT_CHARACTERISTIC_NOTIFY = "00002902-0000-1000-8000-00805f9b34fb"
        const val CLIENT_USER_DESCRIPTOR = "00002901-0000-1000-8000-00805f9b34fb"
        //chat
        const val CHAT_UUID = "fa87c0d0-afac-11de-8a39-0800200c9a66"

        private val attributes: HashMap<String, String> = object : HashMap<String, String>()
        {
            init {
                //services
                //put("0000180d-0000-1000-8000-00805f9b34fb", "Heart Rate Service")
                //put(DEVICE_INFO_SERVICE, "Device Information Service")
                put(NAME_SERVICE, "Name Service")
                put(INFO_SERVICE, "Info Service")
                put(CHAT_SERVICE, "Chat Service")
                //characteristics.
                //put(HEART_RATE_MEASUREMENT, "Heart Rate Measurement");
                //put(MANUFACTURER_NAME_STRING, "Manufacturer Name String")
                put(NAME_CHAR, "Name Char")
                put(NICKNAME_CHAR, "Nickname Char")
                put(LOCATION_CHAR, "Location Char")
                put(CHAT_MESSAGE_CHAR, "Chat Message Char")
            }
        }

        fun lookup(uuid: String, defaultName: String): String {
            val name = attributes[uuid]
            return name ?: defaultName
        }

        /**
         * Construct the field values for nickname characteristic
         */
        fun getNicknameByteArray(): ByteArray {
//            val field = ByteArray(1)
//            field[0] = (0x0c).toByte()
//            return field
            val nickname = BleHomeActivity.broadcastNickname
            return nickname.toByteArray()
        }
        /**
         * Construct the field values for location characteristic
         */
        fun getLocationByteArray(): ByteArray {
            val location = BleHomeActivity.broadcastLocation
            return location.toByteArray()
        }
        /**
         * Return a configured [BluetoothGattService] instance for the
         * Name Service.
         */
        fun createNameService(): BluetoothGattService {
            val service = BluetoothGattService(
                UUID.fromString(NAME_SERVICE),
                BluetoothGattService.SERVICE_TYPE_PRIMARY)
            // name characteristic
            val nameCharacteristic = BluetoothGattCharacteristic(
                UUID.fromString(NAME_CHAR),
                //Read-only characteristic, supports notifications
                BluetoothGattCharacteristic.PROPERTY_READ or BluetoothGattCharacteristic.PROPERTY_NOTIFY,
                BluetoothGattCharacteristic.PERMISSION_READ)
            val configDescriptor = BluetoothGattDescriptor(
                UUID.fromString(CLIENT_CHARACTERISTIC_NOTIFY),
                //Read/write descriptor
                BluetoothGattDescriptor.PERMISSION_READ or BluetoothGattDescriptor.PERMISSION_WRITE)
            nameCharacteristic.addDescriptor(configDescriptor)

            service.addCharacteristic(nameCharacteristic)
            return service
        }
        /**
         * Return a configured [BluetoothGattService] instance for the
         * Info Service.
         */
        fun createInfoService(): BluetoothGattService {
            val service = BluetoothGattService(
                UUID.fromString(INFO_SERVICE),
                BluetoothGattService.SERVICE_TYPE_PRIMARY)
            // nick name characteristic
            val nickNameCharacteristic = BluetoothGattCharacteristic(
                UUID.fromString(NICKNAME_CHAR),
                //Read-only characteristic, supports notifications
                BluetoothGattCharacteristic.PROPERTY_READ or BluetoothGattCharacteristic.PROPERTY_NOTIFY,
                BluetoothGattCharacteristic.PERMISSION_READ)
            service.addCharacteristic(nickNameCharacteristic)
            // location characteristic
            val locationCharacteristic = BluetoothGattCharacteristic(
                UUID.fromString(LOCATION_CHAR),
                //Read-only characteristic, supports notifications
                BluetoothGattCharacteristic.PROPERTY_READ or BluetoothGattCharacteristic.PROPERTY_NOTIFY,
                BluetoothGattCharacteristic.PERMISSION_READ)
            service.addCharacteristic(locationCharacteristic)

            return service
        }
        /**
         * Function to create the Chat Service.
         * GATT Server with the required characteristics and descriptors
         */
        fun createChatService(): BluetoothGattService {
            // Setup gatt service
            val service = BluetoothGattService(
                UUID.fromString(CHAT_SERVICE),
                BluetoothGattService.SERVICE_TYPE_PRIMARY)
            // need to ensure that the property is writable and has the write permission
            val messageCharacteristic = BluetoothGattCharacteristic(
                UUID.fromString(CHAT_MESSAGE_CHAR),
                BluetoothGattCharacteristic.PROPERTY_WRITE,
                BluetoothGattCharacteristic.PERMISSION_WRITE
            )
            service.addCharacteristic(messageCharacteristic)
            val confirmCharacteristic = BluetoothGattCharacteristic(
                UUID.fromString(CHAT_CONFIRM_CHAR),
                BluetoothGattCharacteristic.PROPERTY_WRITE,
                BluetoothGattCharacteristic.PERMISSION_WRITE
            )
            service.addCharacteristic(confirmCharacteristic)

            return service
        }
    }
}
