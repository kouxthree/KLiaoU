package com.kliaou.ui.home

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.ContentValues
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.kliaou.R
import com.kliaou.databinding.ActivityHomeBindBinding
import com.kliaou.databinding.BleActivityConnectDetailBinding
import com.kliaou.service.BleGattAttributes
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.*

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
    override fun onResume() {
        super.onResume()
    }
    override fun onPause() {
        super.onPause()
    }
    override fun onDestroy() {
        super.onDestroy()
    }

    //chat
    private var blState = BL_STATE_NONE
    private lateinit var bluetoothAdapter: BluetoothAdapter
    private lateinit var remoteDevice: BluetoothDevice
    //chat view
    private fun createChatView() {
        //init bluetooth manager
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.getAdapter()
        //connect btn listener
        _binding.btnConnect.setOnClickListener {
            try {
                remoteDevice = bluetoothAdapter?.getRemoteDevice(mDeviceAddress)!!
                if (bluetoothAdapter.isDiscovering == true) {
                    bluetoothAdapter.cancelDiscovery()
                }
                //run chat connect thread
                stopChatConnecting()
                chatConnectThread = ChatConnectThread(remoteDevice)
                chatConnectThread?.start()
                blState = BL_STATE_CONNECTING
            } catch (e: IOException) {
                val msg = "Could not get remote device"
                Toast.makeText(applicationContext, msg, Toast.LENGTH_LONG).show()
                Log.e(ContentValues.TAG, msg, e)
            }
        }
        //send message listener
        _binding.btnSendMsg.setOnClickListener {
            //not connected
            if(!checkConnectState()) return@setOnClickListener
            //send message
            chatSendMessage(_binding.txtOut.text.toString())
        }
    }
    //chat connect and connected thread
    private var chatConnectThread: ChatConnectThread? = null
    private var chatConnectedThread: ChatConnectedThread? = null
    private fun stopChatConnecting() {
        if(chatConnectThread != null) {
            chatConnectThread?.cancel()
        }
        if(chatConnectedThread != null) {
            chatConnectThread?.cancel()
        }
    }
    private inner class ChatConnectThread(device: BluetoothDevice) : Thread() {
        private var mmSocket: BluetoothSocket

        init{
            mmSocket = device.createRfcommSocketToServiceRecord(CHAT_UUID)
        }

        override fun run() {
            bluetoothAdapter?.cancelDiscovery()
            try {
                mmSocket.connect()
                // Start the connected thread
                chatConnectedThread = ChatConnectedThread(mmSocket)
                chatConnectedThread?.start()
            } catch (e: IOException) {
                Log.e(ContentValues.TAG, "Could not connect socket", e)
                cancel()
            }
        }

        fun cancel() {
            try {
                mmSocket.close()
                blState = BL_STATE_NONE
            } catch (e: IOException) {
                Log.e(ContentValues.TAG, "Could not close the client socket", e)
            }
        }
    }
    private inner class ChatConnectedThread(socket: BluetoothSocket) : Thread() {
        private val mmSocket: BluetoothSocket
        private val mmInStream: InputStream?
        private val mmOutStream: OutputStream?
        override fun run() {
            Log.i(ContentValues.TAG, "BEGIN mConnectedThread")
            val buffer = ByteArray(1024)
            var bytes: Int
            // Keep listening to the InputStream while connected
            while (blState == BL_STATE_CONNECTED) {
                try {
                    // Read from the InputStream
                    bytes = mmInStream!!.read(buffer)
                    // Send the obtained bytes to the UI Activity
                    mHandler.obtainMessage(MESSAGE_READ, bytes, -1, buffer)
                        .sendToTarget()
                } catch (e: IOException) {
                    Log.e(ContentValues.TAG, "disconnected", e)
                    cancel()
                    break
                }
            }
        }

        fun write(buffer: ByteArray?, msgtype: Int) {
            try {
                mmOutStream!!.write(buffer)
                // Share the sent message back to the UI Activity
                mHandler.obtainMessage(msgtype, -1, -1, buffer)
                    .sendToTarget()
            } catch (e: IOException) {
                Log.e(ContentValues.TAG, "Exception during write", e)
            }
        }

        fun cancel() {
            try {
                mmSocket.close()
                blState = BL_STATE_NONE
            } catch (e: IOException) {
                Log.e(ContentValues.TAG, "close() of connect socket failed", e)
            }
        }

        init {
            // Get the BluetoothSocket input and output streams
            Log.d(ContentValues.TAG, "create ConnectedThread")
            mmSocket = socket
            var tmpIn: InputStream? = null
            var tmpOut: OutputStream? = null
            try {
                tmpIn = socket.inputStream
                tmpOut = socket.outputStream
            } catch (e: IOException) {
                Log.e(ContentValues.TAG, "temp sockets not created", e)
            }
            mmInStream = tmpIn
            mmOutStream = tmpOut
            blState = BL_STATE_CONNECTED
        }
    }
    private fun setStatus(subTitle: CharSequence) {
        supportActionBar?.subtitle = subTitle
    }
    private val mHandler: Handler = object : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            when (msg.what) {
                MESSAGE_STATE_CHANGE -> when (msg.arg1) {
                    BL_STATE_CONNECTED -> {
                        setStatus("CONNECTED")
                        _binding.txtConversation.setText(null)
                    }
                    BL_STATE_CONNECTING -> setStatus("CONNECTING")
                    BL_STATE_LISTEN, BL_STATE_NONE -> setStatus("NO CONNECT")
                }
                MESSAGE_WRITE -> {
                    val writeBuf = msg.obj as ByteArray
                    val writeMessage = String(writeBuf)
                    _binding.txtConversation.append("\nMe:  $writeMessage")
                }
                MESSAGE_READ -> {
                    val readBuf = msg.obj as ByteArray
                    val readMessage = String(readBuf, 0, msg.arg1)
                    _binding.txtConversation.append("\nRemote:  $readMessage")
                }
            }
        }
    }
    //send
    private fun checkConnectState(): Boolean {
        if (blState != BL_STATE_CONNECTED) {
            Toast.makeText(applicationContext, "Not Connected", Toast.LENGTH_SHORT).show()
            return false
        }
        return true
    }
    private fun chatSendMessage(message: String) {
        if (message.length > 0) {
            val sendmsg = message.toByteArray()
            chatConnectedThread?.write(sendmsg, MESSAGE_WRITE)
            _binding.txtOut.setText(null)
        }
    }

    companion object {
        private val TAG = BleConnectDetailActivity::class.java.simpleName
        private val CHAT_UUID = UUID.fromString(BleGattAttributes.NAME_CHAR)
        const val EXTRAS_DEVICE_NAME = "DEVICE_NAME"
        const val EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS"

        val BL_STATE_NONE = 0
        val BL_STATE_LISTEN = 1
        val BL_STATE_CONNECTING = 2
        val BL_STATE_CONNECTED = 3
        val MESSAGE_STATE_CHANGE = 1
        val MESSAGE_READ = 2
        val MESSAGE_WRITE = 3
    }
}