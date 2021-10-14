package com.kliaou.service

import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattService
import com.kliaou.ui.home.BleHomeMainActivity
import java.util.*
import kotlin.collections.HashMap

class BleGattAttributes {
    companion object {
        //service
        const val NAME_SERVICE = "00001800-0000-1000-8000-00805f9b34fb"
        const val INFO_SERVICE = "0000180a-0000-1000-8000-00805f9b34fb"
        //characteristics
        const val NAME_CHAR = "00002a00-0000-1000-8000-00805f9b34fb"
        const val NICKNAME_CHAR = "00002af8-0000-1000-8000-00805f9b34fb"
        const val LOCATION_CHAR = "00002af7-0000-1000-8000-00805f9b34fb"
        //val MANUFACTURER_NAME_STRING = "00002a29-0000-1000-8000-00805f9b34fb"
        //val HEART_RATE_MEASUREMENT = "00002a37-0000-1000-8000-00805f9b34fb"
        const val CLIENT_CHARACTERISTIC_NOTIFY = "00002902-0000-1000-8000-00805f9b34fb"
        const val CLIENT_USER_DESCRIPTOR = "00002901-0000-1000-8000-00805f9b34fb"

        private val attributes: HashMap<String, String> = object : HashMap<String, String>()
        {
            init {
                //services
                //put("0000180d-0000-1000-8000-00805f9b34fb", "Heart Rate Service")
                //put(DEVICE_INFO_SERVICE, "Device Information Service")
                put(NAME_SERVICE, "Name Service")
                put(INFO_SERVICE, "Info Service")
                //characteristics.
                //put(HEART_RATE_MEASUREMENT, "Heart Rate Measurement");
                //put(MANUFACTURER_NAME_STRING, "Manufacturer Name String")
                put(NAME_CHAR, "Name Char")
                put(NICKNAME_CHAR, "Nickname Char")
                put(LOCATION_CHAR, "Location Char")
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
            val nickname = BleHomeMainActivity.broadcastNickname
            return nickname.toByteArray()
        }
        /**
         * Construct the field values for location characteristic
         */
        fun getLocationByteArray(): ByteArray {
            val location = BleHomeMainActivity.broadcastLocation
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
    }
}
