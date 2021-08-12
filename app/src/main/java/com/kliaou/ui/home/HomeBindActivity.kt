package com.kliaou.ui.home

import android.R
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.ContentValues.TAG
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.*
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.kliaou.databinding.ActivityHomeBindBinding
import com.kliaou.scanresult.RecyclerAdapter
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.*
import java.util.*

class HomeBindActivity : AppCompatActivity() {
    private lateinit var _binding: ActivityHomeBindBinding
    private lateinit var _mac: String
    private var _myimgbitmap: Bitmap? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //init//intent was null before onCreate
        _binding = ActivityHomeBindBinding.inflate(layoutInflater)
        //setContentView(R.layout.activity_bind)
        setContentView(_binding.root)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)//menu action

        //remote mac address
        _mac = intent.getStringExtra(RecyclerAdapter.BIND_ITEM_ADDRESS).toString()
        device = bluetoothAdapter?.getRemoteDevice(_mac)!!
        _binding.txtUuid.text = _mac.toString()

        //my image bitmap
        val myimgfile = File(getExternalFilesDir(Environment.DIRECTORY_PICTURES)!!.path + "/" + HomeMainActivity.MY_IMG_FILE_NAME + ".jpg")
        if(myimgfile.exists()) _myimgbitmap = BitmapFactory.decodeFile(myimgfile.absolutePath)

        //bluetooth
        createBl()
        //send view
        createSendView()

    }
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.getItemId() == R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        return true
    }

    //bluetooth
    companion object {
        val NAME = "kliaouservice"
        val MALE_UUID = UUID.fromString("fa87c0d0-afac-11de-8a39-0800200c9a66")
        val FEMALE_UUID = UUID.fromString("8ce255c0-200a-11e0-ac64-0800200c9a66")

        val BL_STATE_NONE = 0
        val BL_STATE_LISTEN = 1
        val BL_STATE_CONNECTING = 2
        val BL_STATE_CONNECTED = 3

        val MESSAGE_STATE_CHANGE = 1
        val MESSAGE_READ = 2
        val MESSAGE_WRITE = 3
        val MESSAGE_DEVICE_NAME = 4
        val MESSAGE_TOAST = 5
        val IMAGE_WRITE = 6

        val IMAGE_RECEIVED = 7
    }
    private var blState = BL_STATE_NONE
    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private lateinit var device: BluetoothDevice
    private lateinit var _uuid: UUID
    private fun createBl() {
        _binding.btnReq.setOnClickListener {
            try {
                device = bluetoothAdapter?.getRemoteDevice(_mac)!!
                _uuid = MALE_UUID
                if (bluetoothAdapter.isDiscovering == true) {
                    bluetoothAdapter.cancelDiscovery()
                }
                //run connect thread
                stopConnecting()
                connectThread = ConnectThread(device)
                connectThread?.start()
                blState = BL_STATE_CONNECTING
            } catch (e: IOException) {
                val msg = "Could not get remote device"
                Toast.makeText(applicationContext, msg, Toast.LENGTH_LONG).show()
                Log.e(TAG, msg, e)
            }
        }
    }
    //connect and connected thread
    private var connectThread: ConnectThread? = null
    private var connectedThread: ConnectedThread? = null
    private fun stopConnecting() {
        if(connectThread != null) {
            connectThread?.cancel()
        }
        if(connectedThread != null) {
            connectedThread?.cancel()
        }
    }
    private inner class ConnectThread(device: BluetoothDevice) : Thread() {
//        private val mmSocket: BluetoothSocket? by lazy(LazyThreadSafetyMode.NONE) {
//            device.createRfcommSocketToServiceRecord(_uuid)
//        }
        private var mmSocket: BluetoothSocket

        init{
            mmSocket = device.createRfcommSocketToServiceRecord(_uuid)
        }

        override fun run() {
            bluetoothAdapter?.cancelDiscovery()
            try {
                mmSocket.connect()
                // Start the connected thread
                connectedThread = ConnectedThread(mmSocket)
                connectedThread?.start()
            } catch (e: IOException) {
                Log.e(TAG, "Could not connect socket", e)
                cancel()
            }
        }

        fun cancel() {
            try {
                mmSocket.close()
                blState = BL_STATE_NONE
            } catch (e: IOException) {
                Log.e(TAG, "Could not close the client socket", e)
            }
        }
    }
    private inner class ConnectedThread(socket: BluetoothSocket) : Thread() {
        private val mmSocket: BluetoothSocket
        private val mmInStream: InputStream?
        private val mmOutStream: OutputStream?
        override fun run() {
            Log.i(TAG, "BEGIN mConnectedThread")
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
                    Log.e(TAG, "disconnected", e)
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
                Log.e(TAG, "Exception during write", e)
            }
        }

        fun cancel() {
            try {
                mmSocket.close()
                blState = BL_STATE_NONE
            } catch (e: IOException) {
                Log.e(TAG, "close() of connect socket failed", e)
            }
        }

        init {
            // Get the BluetoothSocket input and output streams
            Log.d(TAG, "create ConnectedThread")
            mmSocket = socket
            var tmpIn: InputStream? = null
            var tmpOut: OutputStream? = null
            try {
                tmpIn = socket.inputStream
                tmpOut = socket.outputStream
            } catch (e: IOException) {
                Log.e(TAG, "temp sockets not created", e)
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
                IMAGE_WRITE -> {
                    _binding.txtConversation.append("\nMe:  image sending")
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
    private fun createSendView() {
        _binding.btnSendMsg.setOnClickListener {
            //not connected
            if(!checkConnectState()) return@setOnClickListener
            //send message
            sendMessage(_binding.txtOut.text.toString())
        }
        //my image
        _binding.btnSendImg.isEnabled = false
        _myimgbitmap?.let {
            _binding.myImg.setImageBitmap(_myimgbitmap)
            _binding.btnSendImg.isEnabled = true
        }
        _binding.btnSendImg.setOnClickListener {
            //not connected
            if(!checkConnectState()) return@setOnClickListener
            //send image
            _myimgbitmap?.let { sendImage(it) }
        }
    }
    private fun sendMessage(message: String) {
        if (message.length > 0) {
            val sendmsg = message.toByteArray()
            connectedThread?.write(sendmsg, MESSAGE_WRITE)
            _binding.txtOut.setText(null)
        }
    }
    private fun sendImage(image: Bitmap) {
        lifecycleScope.launch {
            //image -> inputstream
            val imageByteOutputStream = ByteArrayOutputStream()
            image.compress(Bitmap.CompressFormat.JPEG, 100, imageByteOutputStream)
            val imageByteArray = imageByteOutputStream.toByteArray()
            val imageByteInputStream = ByteArrayInputStream(imageByteArray)
            //inpustream -> write to remote
            val BUF_SIZE = 1024
            val buffer = ByteArray(BUF_SIZE)
            //sending start
            Log.i(TAG, "BEGIN image send")
            connectedThread?.write(HomeMainActivity.MY_IMG_SEND_START.toByteArray(), MESSAGE_WRITE)
            delay(500L)
            //sending size
            Log.i(TAG, "image size: ${imageByteArray.size}")
            connectedThread?.write(
                HomeMainActivity.MY_IMG_SEND_SEPERATOR.toByteArray(),
                MESSAGE_WRITE
            )
            var sizestring = imageByteArray.size.toString().padStart(12, '0')
            connectedThread?.write(sizestring.toByteArray(), MESSAGE_WRITE)
            connectedThread?.write(
                HomeMainActivity.MY_IMG_SEND_SEPERATOR.toByteArray(),
                MESSAGE_WRITE
            )
            //sending main
            Log.i(TAG, "start sending")
            while (imageByteInputStream.read(buffer, 0, BUF_SIZE) != -1) {
                connectedThread?.write(buffer, IMAGE_WRITE)
            }
//            //sending end
//            Log.i(TAG, "END image send")
//            connectedThread?.write(HomeMainActivity.MY_IMG_SEND_END.toByteArray(), MESSAGE_WRITE)
        }
    }

}
