package com.kliaou.service

import kotlin.collections.HashMap

class BleGattAttributes {
    init {
        //services
        attributes.put("0000180d-0000-1000-8000-00805f9b34fb", "Heart Rate Service")
        attributes.put("0000180a-0000-1000-8000-00805f9b34fb", "Device Information Service")
        //characteristics.
        attributes.put(HEART_RATE_MEASUREMENT, "Heart Rate Measurement");
        attributes.put("00002a29-0000-1000-8000-00805f9b34fb", "Manufacturer Name String");
    }

    companion object {
        private val attributes : HashMap<String, String> = HashMap()
        val HEART_RATE_MEASUREMENT = "00002a37-0000-1000-8000-00805f9b34fb"
        val CLIENT_CHARACTERISTIC_CONFIG = "00002902-0000-1000-8000-00805f9b34fb"

        fun lookup(uuid: String , defaultName: String): String {
            var name = attributes.get(uuid)
            return name ?: defaultName
        }
    }
}
