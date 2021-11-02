package com.kliaou.service

import android.app.*
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.BluetoothLeAdvertiser
import android.content.Context
import android.content.Intent
import android.os.*
import android.util.Log
import com.kliaou.*
import com.kliaou.datastore.proto.SEX
import com.kliaou.ui.BleHomeActivity
import com.kliaou.ui.mySexDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import java.util.concurrent.TimeUnit

class BleAdvertiserService : Service() {
    private var bluetoothLeAdvertiser: BluetoothLeAdvertiser? = null
    private var advertiseCallback: AdvertiseCallback? = null
    private var handler: Handler? = null
    private val TIMEOUT: Long = TimeUnit.MILLISECONDS.convert(ADVERTISE_TIMEOUT, TimeUnit.MINUTES)

    override fun onBind(intent: Intent?): IBinder? {
        return null // no binding necessary. This Service will only be started
    }
    override fun onCreate() {
        running = true
        initialize()
        startAdvertising()
        setTimeout()
        super.onCreate()
    }
    override fun onDestroy() {
        running = false
        stopAdvertising()
        handler?.removeCallbacksAndMessages(null) // this is a generic way for removing tasks
        stopForeground(true)
        super.onDestroy()
    }
    private fun initialize() {
        if (bluetoothLeAdvertiser == null) {
            val manager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            val bluetoothAdapter: BluetoothAdapter = manager.adapter
            bluetoothLeAdvertiser = bluetoothAdapter.bluetoothLeAdvertiser
        }
    }

    private fun startAdvertising() {
        goForeground()
        Log.d(TAG, "Service: Starting Advertising")
        if (advertiseCallback == null) {
            val settings: AdvertiseSettings = buildAdvertiseSettings()
            val data: AdvertiseData = buildAdvertiseData()
            advertiseCallback = bleAdvertiseCallback()
            bluetoothLeAdvertiser?.startAdvertising(settings, data, advertiseCallback)
        }
    }
    private fun stopAdvertising() = bluetoothLeAdvertiser?.stopAdvertising(advertiseCallback)
        .also { bluetoothLeAdvertiser = null }
    private fun goForeground() {
        val notificationIntent = Intent(this, BleHomeActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent,0)
        val nBuilder = when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.O -> {
                val bleNotificationChannel = NotificationChannel(
                    BLE_NOTIFICATION_CHANNEL_ID, "BLE",
                    NotificationManager.IMPORTANCE_DEFAULT
                )
                val nManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
                nManager.createNotificationChannel(bleNotificationChannel)
                Notification.Builder(this, BLE_NOTIFICATION_CHANNEL_ID)
            }
            else -> Notification.Builder(this)
        }

        val notification = nBuilder.setContentTitle(getString(R.string.bt_notif_title))
            .setContentText(getString(R.string.bt_notif_txt))
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .build()
        startForeground(FOREGROUND_NOTIFICATION_ID, notification)
    }

    private fun buildAdvertiseSettings() = AdvertiseSettings.Builder()
        .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_POWER)
        .setTimeout(0).build()
    private fun buildAdvertiseData(): AdvertiseData {
        //read my gender from datastore
        val mySexFlow: Flow<SEX>? =
            applicationContext.mySexDataStore.data.map { settings ->
                settings.sex
            }
        val mySex = runBlocking {
            mySexFlow?.first()
        }
        val data = AdvertiseData.Builder()
        data.addServiceUuid(ADVERTISE_UUID)
        when(mySex) {
            SEX.MALE -> data.addServiceData(ADVERTISE_UUID, byteArrayOf(ADVERTISE_DATA_MALE))
            SEX.FEMALE -> data.addServiceData(ADVERTISE_UUID, byteArrayOf(ADVERTISE_DATA_FEMALE))
            else -> {
                data.addServiceData(ADVERTISE_UUID, byteArrayOf(ADVERTISE_DATA_MALE, ADVERTISE_DATA_FEMALE))
            }
        }
        return data.setIncludeDeviceName(true).build()
    }
    private fun bleAdvertiseCallback() = object : AdvertiseCallback() {
        override fun onStartFailure(errorCode: Int) {
            super.onStartFailure(errorCode)
            Log.d(TAG, "Advertising failed")
            broadcastFailureIntent(errorCode)
            stopSelf()
        }

        override fun onStartSuccess(settingsInEffect: AdvertiseSettings) {
            super.onStartSuccess(settingsInEffect)
            Log.d(TAG, "Advertising successfully started")
        }
    }
    private fun broadcastFailureIntent(errorCode: Int) {
        val failureIntent = Intent().setAction(ADVERTISING_FAILED).putExtra(
            BT_ADVERTISING_FAILED_EXTRA_CODE, errorCode
        )
        sendBroadcast(failureIntent)
    }

    private fun setTimeout() {
        handler = Handler(Looper.myLooper()!!)
        val runnable = Runnable {
            Log.d(
                TAG,
                "run: BleAdvertiserService has reached timeout of $TIMEOUT milliseconds, stopping advertising."
            )
            broadcastFailureIntent(ADVERTISING_TIMED_OUT)
        }
        handler?.postDelayed(runnable, TIMEOUT)
    }

    companion object {
        val TAG = BleAdvertiserService::class.java.simpleName
        var running: Boolean = false
    }
}
