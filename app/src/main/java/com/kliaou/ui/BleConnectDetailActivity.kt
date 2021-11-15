package com.kliaou.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.kliaou.R

class BleConnectDetailActivity : AppCompatActivity() {
    private var mDeviceName: String? = null
    private var mDeviceAddress: String? = null

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