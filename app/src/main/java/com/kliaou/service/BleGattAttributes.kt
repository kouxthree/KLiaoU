package com.kliaou.service

import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattService
import java.util.*
import kotlin.collections.HashMap

class BleGattAttributes {
    companion object {
        //service
        //val DEVICE_INFO_SERVICE = "0000180a-0000-1000-8000-00805f9b34fb"
        val NAME_SERVICE = "00001800-0000-1000-8000-00805f9b34fb"
        //characteristics
        val NAME_STRING = "00002a00-0000-1000-8000-00805f9b34fb"
        //val MANUFACTURER_NAME_STRING = "00002a29-0000-1000-8000-00805f9b34fb"
        //val HEART_RATE_MEASUREMENT = "00002a37-0000-1000-8000-00805f9b34fb"
        val CLIENT_CHARACTERISTIC_CONFIG = "00002902-0000-1000-8000-00805f9b34fb"

        private val attributes: HashMap<String, String> = object : HashMap<String, String>()
        {
            init {
                //services
                //put("0000180d-0000-1000-8000-00805f9b34fb", "Heart Rate Service")
                //put(DEVICE_INFO_SERVICE, "Device Information Service")
                put(NAME_SERVICE, "Name Servce")
                //characteristics.
                //put(HEART_RATE_MEASUREMENT, "Heart Rate Measurement");
                //put(MANUFACTURER_NAME_STRING, "Manufacturer Name String")
                put(NAME_STRING, "Name String")
            }
        }

        fun lookup(uuid: String, defaultName: String): String {
            var name = attributes.get(uuid)
            return name ?: defaultName
        }

        /**
         * Construct the field values for name characteristic
         */
        fun getNameByteArray(): ByteArray {
            val field = ByteArray(12)
            field[0] = (0x00).toByte()
            field[1] = (0x01).toByte()
            field[2] = (0x02).toByte()
            field[3] = (0x03).toByte()
            field[4] = (0x04).toByte()
            field[5] = (0x05).toByte()
            field[6] = (0x06).toByte()
            field[7] = (0x07).toByte()
            field[8] = (0x08).toByte()
            field[9] = (0x09).toByte()
            field[10] = (0x09).toByte()
            field[11] = (0x0a).toByte()
            return field
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
                UUID.fromString(NAME_STRING),
                //Read-only characteristic, supports notifications
                BluetoothGattCharacteristic.PROPERTY_READ or BluetoothGattCharacteristic.PROPERTY_NOTIFY,
                BluetoothGattCharacteristic.PERMISSION_READ)
            val configDescriptor = BluetoothGattDescriptor(UUID.fromString(CLIENT_CHARACTERISTIC_CONFIG),
                //Read/write descriptor
                BluetoothGattDescriptor.PERMISSION_READ or BluetoothGattDescriptor.PERMISSION_WRITE)
            nameCharacteristic.addDescriptor(configDescriptor)

            service.addCharacteristic(nameCharacteristic)
            return service
        }
    }
}
