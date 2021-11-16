package com.kliaou.ui

import android.app.AlertDialog
import android.bluetooth.BluetoothDevice
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.kliaou.R
import com.kliaou.blerecycler.ChatMessageAdapter
import com.kliaou.databinding.BleActivityChatBinding
import com.kliaou.service.BleGattServer
import com.kliaou.service.BleMessage


class ChatCaller {
    companion object {
        const val Client = 0
        const val Server = 1
    }
}

class BleChatActivity : AppCompatActivity() {
    private lateinit var _binding: BleActivityChatBinding
    private var mChatCaller: Int? = null
    private var mDeviceName: String? = null
    private var mDeviceAddress: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = BleActivityChatBinding.inflate(layoutInflater)
        setContentView(_binding.root)
        mChatCaller = intent.getIntExtra(EXTRAS_CHAT_CALLER, ChatCaller.Server)
        mDeviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME)
        mDeviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS)

        // Sets up UI references.
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
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
    override fun onDestroy() {
        super.onDestroy()
        //remove chat service
        BleGattServer.removeChatService()
    }
    //chat view
    private val chatMessageAdapter = ChatMessageAdapter()
    private val chatMessageObserver = Observer<BleMessage> { message ->
        Log.d(TAG, "Have message ${message.text}")
        chatMessageAdapter.addMessage(message)
    }
    private val disConnectionRequestObserver = Observer<BluetoothDevice> { device ->
        if(device.address == mDeviceAddress) {
            Log.d(TAG, "DisConnection request observer: have device $device")
            //close chat activity
            val alert = AlertDialog.Builder(this)
            alert.setTitle(getString(R.string.chat_hangup_title))
            alert.setMessage(getString(R.string.chat_hangup_message))
            alert.setPositiveButton(android.R.string.ok) { _, _ ->
                finish()
            }
            alert.show()
        }
    }
    private fun createChatView() {
        //add chat service
        BleGattServer.addChatService()
        //chat message recycler adapter
        Log.d(TAG, "chatWith: set adapter $chatMessageAdapter")
        _binding.txtConversation.layoutManager = LinearLayoutManager(this)
        _binding.txtConversation.adapter = chatMessageAdapter
        //send message listener
        _binding.btnSendMsg.setOnClickListener {
            val message = _binding.txtChatMessage.text.toString()
            // only send message if it is not empty
            if (message.isNotEmpty()) {
                BleGattServer.clientSendMessage(message)
                // clear message
                _binding.txtChatMessage.setText("")
            }
        }
        //set disconnection request observer
        BleGattServer.disConnectionRequest.observe(this, disConnectionRequestObserver)
        //set message observer
        BleGattServer.messages.observe(this, chatMessageObserver)
    }

    companion object {
        private val TAG = BleChatActivity::class.java.simpleName
        const val EXTRAS_CHAT_CALLER = "CALLER"//who opened chat activity(server or client)
        const val EXTRAS_DEVICE_NAME = "DEVICE_NAME"
        const val EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS"
        const val EXTRAS_REMOTE_GENDER = "REMOTE_GENDER"
    }
}