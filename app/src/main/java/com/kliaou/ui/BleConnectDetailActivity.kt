package com.kliaou.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.kliaou.R
import com.kliaou.databinding.BleActivityConnectDetailBinding
import com.kliaou.service.BleGattServer

class BleConnectDetailActivity : AppCompatActivity() {
    private lateinit var _binding: BleActivityConnectDetailBinding
    private var mDeviceName: String? = null
    private var mDeviceAddress: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = BleActivityConnectDetailBinding.inflate(layoutInflater)
        setContentView(R.layout.ble_activity_connect_detail)
        val intent = intent
        mDeviceName = intent.getStringExtra(BleChatActivity.EXTRAS_DEVICE_NAME)
        mDeviceAddress = intent.getStringExtra(BleChatActivity.EXTRAS_DEVICE_ADDRESS)

        // Sets up UI references.
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        (findViewById<View>(R.id.device_name) as TextView).text = mDeviceName
        (findViewById<View>(R.id.device_address) as TextView).text = mDeviceAddress
        supportActionBar?.title = mDeviceName

        //greeting listener
        _binding.btnGreeting.setOnClickListener {
            Log.d(TAG, "chatWith: $mDeviceAddress")
            val showChatActivity = Intent(applicationContext, BleChatActivity::class.java)
            //device name and mac address
            showChatActivity.putExtra(BleChatActivity.EXTRAS_CHAT_CALLER, ChatCaller.Server)
            showChatActivity.putExtra(BleChatActivity.EXTRAS_DEVICE_NAME, mDeviceName)
            showChatActivity.putExtra(BleChatActivity.EXTRAS_DEVICE_ADDRESS, mDeviceAddress)
            applicationContext.startActivity(showChatActivity)
        }
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

    companion object {
        private val TAG = BleConnectDetailActivity::class.java.simpleName
    }
}