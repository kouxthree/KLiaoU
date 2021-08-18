package com.kliaou

import android.Manifest
import android.os.ParcelUuid
import java.util.*

const val BT_ADVERTISING_FAILED_EXTRA_CODE = "bt_adv_failure_code"
const val INVALID_CODE = -1
const val ADVERTISING_TIMED_OUT = 6
const val BLE_NOTIFICATION_CHANNEL_ID = "bleChl"
const val FOREGROUND_NOTIFICATION_ID = 3
const val ADVERTISING_FAILED = "com.kliaou.advertising_failed"
const val REQUEST_ENABLE_BT = 11
const val PERMISSION_REQUEST_LOCATION = 101
const val LOCATION_FINE_PERM = Manifest.permission.ACCESS_FINE_LOCATION
const val ADVERTISE_TIMEOUT: Long = 10//minutes
const val SCAN_PERIOD_IN_MILLIS: Long = 90_000
const val MY_IMG_FILE_NAME = "myimg"
val ADVERTISE_UUID = ParcelUuid.fromString("00000000-a000-0000-0000-000000000000")
