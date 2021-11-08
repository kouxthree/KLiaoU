package com.kliaou.ui

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.ContentValues
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.kliaou.R
import com.kliaou.blerecycler.ChatMessageAdapter
import com.kliaou.databinding.BleActivityConnectDetailBinding
import com.kliaou.service.BleGattAttributes
import com.kliaou.service.BleGattServer
import com.kliaou.service.BleMessage
import androidx.lifecycle.Observer
import java.io.IOException
import java.util.UUID

class BleConnectDetailActivity : AppCompatActivity() {
    private lateinit var _binding: BleActivityConnectDetailBinding
    private var mDeviceName: String? = null
    private var mDeviceAddress: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = BleActivityConnectDetailBinding.inflate(layoutInflater)
        setContentView(R.layout.ble_activity_connect_detail)
        val intent = intent
        mDeviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME)
        mDeviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS)

        // Sets up UI references.
        (findViewById<View>(R.id.device_name) as TextView).text = mDeviceName
        (findViewById<View>(R.id.device_address) as TextView).text = mDeviceAddress
        supportActionBar?.title = mDeviceName

        //chat area view
        createChatView()
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

    //chat view
    private lateinit var bluetoothAdapter: BluetoothAdapter
    private val chatMessageAdapter = ChatMessageAdapter()
    private val chatMessageObserver = Observer<BleMessage> { message ->
        Log.d(TAG, "Have message ${message.text}")
        chatMessageAdapter.addMessage(message)
    }
    private fun createChatView() {
        //init bluetooth manager
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.getAdapter()
        //send message listener
        _binding.btnSendMsg.setOnClickListener {
            val message = _binding.txtChatMessage.text.toString()
            // only send message if it is not empty
            if (message.isNotEmpty()) {
                BleGattServer.sendMessage(message)
                // clear message
                _binding.txtChatMessage.setText("")
            }
        }
        //set message observer
        BleGattServer.messages.observe(this, chatMessageObserver)
    }

    companion object {
        private val TAG = BleConnectDetailActivity::class.java.simpleName
        private val CHAT_UUID = UUID.fromString(BleGattAttributes.NAME_CHAR)
        const val EXTRAS_DEVICE_NAME = "DEVICE_NAME"
        const val EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS"
    }
}