package com.kliaou.ui

import android.bluetooth.*
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.kliaou.R
import com.kliaou.service.BleGattAttributes
import com.kliaou.service.BleGattClientService
import com.kliaou.service.BleGattServer

class BleConnectDetailActivity : AppCompatActivity() {
    private var mDeviceName: String? = null
    private var mDeviceAddress: String? = null
    private var mBluetoothManager: BluetoothManager? = null
    private var mBluetoothAdapter: BluetoothAdapter? = null
    private var mBluetoothGatt: BluetoothGatt? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.ble_activity_connect_detail)
        mDeviceName = intent.getStringExtra(BleChatActivity.EXTRAS_DEVICE_NAME)
        mDeviceAddress = intent.getStringExtra(BleChatActivity.EXTRAS_DEVICE_ADDRESS)

        // Sets up UI references.
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        (findViewById<View>(R.id.device_name) as TextView).text = mDeviceName
        (findViewById<View>(R.id.device_address) as TextView).text = mDeviceAddress
        supportActionBar?.title = mDeviceName

        //greeting listener
        findViewById<View>(R.id.btn_greeting)!!.setOnClickListener {
            Log.d(TAG, "chatWith: $mDeviceAddress")
            val showChatActivity = Intent(this, BleChatActivity::class.java)
            //device name and mac address
            showChatActivity.putExtra(BleChatActivity.EXTRAS_CHAT_CALLER, ChatCaller.Server)
            showChatActivity.putExtra(BleChatActivity.EXTRAS_DEVICE_NAME, mDeviceName)
            showChatActivity.putExtra(BleChatActivity.EXTRAS_DEVICE_ADDRESS, mDeviceAddress)
            startActivity(showChatActivity)
        }

        //add chat service
        BleGattServer.addChatService()

        //connect to gatt server
        connectToGattServer()
    }
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }
    override fun onDestroy() {
        super.onDestroy()
        //remove chat service
        BleGattServer.removeChatService()
    }

    private fun connectToGattServer() {
        if (mBluetoothManager == null) {
            mBluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            if (mBluetoothManager == null) {
                Log.e(TAG, "Unable to initialize BluetoothManager.")
                return
            }
        }
        mBluetoothAdapter = mBluetoothManager!!.adapter
        if (mBluetoothAdapter == null) {
            Log.e(TAG, "Unable to obtain a BluetoothAdapter.")
            return
        }
        val device: BluetoothDevice? = mBluetoothAdapter!!.getRemoteDevice(mDeviceAddress)
        if (device == null) {
            Log.w(TAG, "Device not found.  Unable to connect.")
            return
        }
        mBluetoothGatt = device.connectGatt(this, false, mGattCallback)
        Log.d(TAG, "Trying to create a new connection.")
    }
    private val mGattCallback: BluetoothGattCallback = object: BluetoothGattCallback() {
        override fun onConnectionStateChange(
            gatt: BluetoothGatt, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.i(TAG, "Connected to GATT server.")
                BleGattServer.gattForClientUse = mBluetoothGatt
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.i(TAG, "Disconnected from GATT server.")
                BleGattServer.gattForClientUse = null
            }
        }
        override fun onServicesDiscovered(
            gatt: BluetoothGatt, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                setChatChar(mBluetoothGatt!!.services)
            } else {
                Log.w(TAG, "onServicesDiscovered received: $status")
            }
        }
        override fun onServiceChanged(
            gatt: BluetoothGatt) {
            setChatChar(mBluetoothGatt!!.services)
        }
    }
    private fun setChatChar(gattServices: List<BluetoothGattService>?) {
        if (gattServices == null) return
        var uuid: String?
        // Loops through available GATT Services.
        for (gattService in gattServices) {
            val currentServiceData = HashMap<String, String?>()
            uuid = gattService.uuid.toString()
            val gattCharacteristics = gattService.characteristics
            val chars = ArrayList<BluetoothGattCharacteristic>()
            // Loops through available Characteristics.
            for (gattCharacteristic in gattCharacteristics) {
                uuid = gattCharacteristic.uuid.toString()
                when (uuid) {
                    BleGattAttributes.CHAT_MESSAGE_CHAR -> {
                        //chat message char
                        BleGattServer.chatMessageChar = gattCharacteristic
                        return
                    }
                }
            }
        }
    }

    companion object {
        private val TAG = BleConnectDetailActivity::class.java.simpleName
    }
}