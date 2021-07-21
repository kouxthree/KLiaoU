package com.kliaou.ui

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.ContentValues.TAG
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import com.kliaou.R
import com.kliaou.scanresult.RecyclerAdapter
import kotlinx.android.synthetic.main.activity_bind.*
import java.io.IOException
import java.util.*

class BindActivity : AppCompatActivity() {
    val _mac = intent.getStringExtra(RecyclerAdapter.BIND_ITEM_ADDRESS)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bind)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        txt_uuid.text = _mac.toString()
        //bluetooth
        createBl()
    }
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.getItemId() == android.R.id.home) {
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
        val NAME = "KLIAOU"
        val MALE_UUID = UUID.fromString("fa87c0d0-afac-11de-8a39-0800200c9a66")
        val FEMALE_UUID = UUID.fromString("8ce255c0-200a-11e0-ac64-0800200c9a66")

        val BL_STATE_NONE = 0
        val BL_STATE_LISTEN = 1
        val BL_STATE_CONNECTING = 2
        val BL_STATE_CONNECTED = 3
    }
    private var blState = BL_STATE_NONE
    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private val device = bluetoothAdapter?.getRemoteDevice(_mac)
    private lateinit var bluetoothSocket: BluetoothSocket
    private lateinit var _uuid: UUID
    private lateinit var connectThread: ConnectThread
    private fun createBl() {
        btn_req.setOnClickListener {
            _uuid = MALE_UUID
            if (bluetoothAdapter?.isDiscovering == true) {
                bluetoothAdapter.cancelDiscovery()
            }
        }
        //run connect thread
        if(blState != BindActivity.BL_STATE_NONE) {
            connectThread.cancel()
        }
        connectThread = device?.let { ConnectThread(it) }!!
        connectThread.start()
    }
    private inner class ConnectThread(device: BluetoothDevice) : Thread() {
        private val mmSocket: BluetoothSocket? by lazy(LazyThreadSafetyMode.NONE) {
            device.createRfcommSocketToServiceRecord(_uuid)
        }
        init{
            blState = BindActivity.BL_STATE_CONNECTING
        }
        public override fun run() {
            bluetoothAdapter?.cancelDiscovery()
            mmSocket?.let { socket ->
                socket.connect()
                manageConnectedSocket(socket)
            }
        }
        fun cancel() {
            try {
                mmSocket?.close()
            } catch (e: IOException) {
                Log.e(TAG, "Could not close the client socket", e)
            }
        }
        fun manageConnectedSocket(socket: BluetoothSocket) {

        }
    }
}
