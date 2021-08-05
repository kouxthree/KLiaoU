package com.kliaou.ui.home

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.*
import android.provider.MediaStore
import android.util.Log
import android.view.*
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.kliaou.R
import com.kliaou.databinding.ActivityHomeMainBinding
import com.kliaou.scanresult.RecyclerAdapter
import com.kliaou.scanresult.RecyclerItem
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.collections.ArrayList
import android.graphics.Bitmap
import java.io.*

class HomeMainActivity : AppCompatActivity() {
    val homeViewModel:HomeViewModel by viewModels()
    private lateinit var _binding: ActivityHomeMainBinding
    private lateinit var recyclerAdapter: RecyclerAdapter
    private lateinit var scanResultLinearLayoutManager: LinearLayoutManager
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
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //init//intent was null before onCreate
        _binding = ActivityHomeMainBinding.inflate(layoutInflater)
        //setContentView(R.layout.activity_bind)
        setContentView(_binding.root)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)//menu action

        val textView: TextView = _binding.textHome
        homeViewModel.text.observe(this, { textView.text = it })

        //scan result list
        scanResultLinearLayoutManager = LinearLayoutManager(this)
        _binding.listviewScanresult.layoutManager = scanResultLinearLayoutManager
        recyclerAdapter = RecyclerAdapter(scanResults)
        _binding.listviewScanresult.adapter = recyclerAdapter
        //location
        requestLocationPermission()
        //bluetooth
        createBl()
        //restore scan results
        restoreScanResults()
        //my image
        createMyImg()
    }

    //bluetooth
    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private val scanResults: ArrayList<RecyclerItem> by lazy() { ArrayList() }
    private var isScanning = (bluetoothAdapter?.isDiscovering == true)
    private var isBroadcasting = (bluetoothAdapter?.getScanMode() == BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE)
    private var broadcastThread: BroadcastThread? = null
    private var blState = HomeBindActivity.BL_STATE_NONE
    private fun createBl() {
        //check if bluetooth is available or not
        if (bluetoothAdapter == null) {
            _binding.textServerInfo1.text = "Bluetooth is not available"
        } else {
            _binding.textServerInfo1.text = "Bluetooth is available"
        }
        //turn on bluetooth
        if (bluetoothAdapter?.isEnabled == true) {
            _binding.textServerInfo2.text = "enabled"
        } else {
            _binding.textServerInfo2.text = "disabled"
            Toast.makeText(applicationContext, "Turning On Bluetooth...", Toast.LENGTH_LONG).show()
            //intent to on bluetooth
            val intentTurnOnBt = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            val startForResultTurnOnBt =
                registerForActivityResult(StartActivityForResult()) { result: ActivityResult? ->
                    when (result?.resultCode) {
                        RESULT_OK -> {
                            Toast.makeText(applicationContext, "Bluetooth Turned On.", Toast.LENGTH_SHORT)
                                .show()
                            _binding.textServerInfo2.text = "enabled"
                        }
                        RESULT_CANCELED -> {
                            Toast.makeText(
                                applicationContext,
                                "Bluetooth Cannot Be Turned On.",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }
            startForResultTurnOnBt.launch(intentTurnOnBt)
        }
        //btn_search
        setBtnSearchBkColor()
        _binding.btnSearch.setOnClickListener {
            if (bluetoothAdapter?.isDiscovering == false) {
                startScan()
                scanResults.clear()//rescan
                //paired devices
                val pairedDevices: Set<BluetoothDevice>? = bluetoothAdapter?.bondedDevices
                pairedDevices?.forEach { device ->
                    addToScanResults(device)
//                    val deviceName = device.name
//                    val deviceHardwareAddress = device.address // MAC address
                }
                recyclerAdapter.notifyDataSetChanged()
            } else {
               stopScan()
            }
            setBtnSearchBkColor()
        }
        //make bluetooth discoverable
        val intentTurnOnBtDiscoverable = Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE)
        intentTurnOnBtDiscoverable.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, BROADCAST_TIMEOUT);
        val startForResultTurnOnBtDiscoverable =
            registerForActivityResult(StartActivityForResult()) { result: ActivityResult? ->
                when (result?.resultCode) {
                    RESULT_OK -> {
                        Toast.makeText(
                            applicationContext,
                            "Bluetooth Discoverable.",
                            Toast.LENGTH_LONG
                        )
                            .show()

                    }
                    RESULT_CANCELED -> {
                        Toast.makeText(
                            applicationContext,
                            "Bluetooth NOT Discoverable",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        //make bluetooth non-discoverable
        val intentTurnOffBtDiscoverable = Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE)
        intentTurnOffBtDiscoverable.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 1);
        val startForResultTurnOffBtDiscoverable =
            registerForActivityResult(StartActivityForResult()) { result: ActivityResult? ->
                Toast.makeText(
                    applicationContext,
                    "Bluetooth NOT Discoverable",
                    Toast.LENGTH_LONG
                ).show()
                _binding.textServerInfo3.text = "not discoverable"
            }
        //btn_broadcast
        setBtnBroadcastBkColor()
        _binding.btnBroadcast.setOnClickListener {
            if (!isBroadcasting) {
                isBroadcasting = true
                //turn on bluetooth discoverable
                if(bluetoothAdapter?.getScanMode() != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
                    startForResultTurnOnBtDiscoverable.launch(intentTurnOnBtDiscoverable)
                }
                //run broadcast thread
                stopBroadcasting()
                broadcastThread = BroadcastThread(HomeBindActivity.MALE_UUID)
                broadcastThread?.start()
            } else {
                isBroadcasting = false
                startForResultTurnOffBtDiscoverable.launch(intentTurnOffBtDiscoverable)
                stopBroadcasting()
            }
            setBtnBroadcastBkColor()
        }
    }
    //for data communication
    private fun restoreScanResults() {
    }
    //scan
    private fun startScan() {
        val filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
        filter.addAction((BluetoothDevice.ACTION_UUID))
        registerReceiver(bScanResult, filter)
        bluetoothAdapter?.startDiscovery()
        isScanning = true
    }
    private fun stopScan() {
        unregisterReceiver(bScanResult)
        bluetoothAdapter?.cancelDiscovery()
        isScanning = false
    }
    private fun setBtnSearchBkColor() {
        try {
            //biding is null when fragment not active
            val btn = findViewById<FloatingActionButton>(R.id.btn_search)
            if (isScanning) btn?.backgroundTintList = ColorStateList.valueOf(Color.RED)
            else {
                btn?.backgroundTintList = ColorStateList.valueOf(Color.GRAY)
            }
        } catch (e: Exception) {
        }
    }
    private val bScanResult = object: BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            if(action == BluetoothDevice.ACTION_FOUND) {
                //bluetooth device
                val device =
                    intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                if (device != null) {
//                    var addflg = false//add specific devices only//bluetooth uuids
//                    val uuidExtra = intent.getParcelableArrayExtra(BluetoothDevice.EXTRA_UUID)
//                    if(uuidExtra != null) {
//                        for (i in uuidExtra!!.indices) {
//                            val uuid = uuidExtra[i].toString()
//                            addflg = (HomeBindActivity.MALE_UUID.toString().compareTo(uuid) == 0)
//                            if (addflg) break
//                        }
//                    }
//                    if(addflg) addToScanResults(device)
                    addToScanResults(device)
                }
            } else if(action == BluetoothAdapter.ACTION_DISCOVERY_FINISHED) {
                stopScan()
                setBtnSearchBkColor()
            } else if(action == BluetoothDevice.ACTION_UUID) {
            }
        }
    }
    private fun addToScanResults(device: BluetoothDevice) {
        if (!scanResults.any { it.Address.compareTo(device.address) == 0 }) {
            val recyclerItem = RecyclerItem(null, device.name?:"", device.address)
            scanResults.add(recyclerItem)
            recyclerAdapter.notifyItemInserted(scanResults.size - 1)
        }
    }
    //broadcast
    private val BROADCAST_TIMEOUT: Long = TimeUnit.MILLISECONDS.convert(10, TimeUnit.MINUTES)
    private fun setBtnBroadcastBkColor() {
        try {
            //biding is null when fragment not active
            val btn = findViewById<FloatingActionButton>(R.id.btn_broadcast)
            val txt3 = findViewById<TextView>(R.id.text_server_info3)
            if (isBroadcasting){
                btn?.backgroundTintList = ColorStateList.valueOf(Color.RED)
                txt3?.text = "discoverable"
            } else {
                btn?.backgroundTintList = ColorStateList.valueOf(Color.GRAY)
                txt3?.text = "NOT discoverable"
            }
        } catch (e: Exception) {
        }
    }
    private fun stopBroadcasting() {
        if(broadcastThread != null) {
            broadcastThread?.cancel()
        }
    }
    private val mHandler: Handler = object : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            if(blState != HomeBindActivity.BL_STATE_LISTEN) return
            when (msg.what) {
                HomeBindActivity.MESSAGE_READ -> {
                    val readBuf = msg.obj as ByteArray
                    val readMessage = String(readBuf, 0, msg.arg1)
                    try {
                        _binding.textReceivedMsg.text = (readMessage)
                    } catch (e: Exception) {
                        Log.e(TAG, "received message: $readMessage")
                    }
                }
            }
        }
    }
    private inner class BroadcastThread(uuid: UUID) : Thread() {
        private val mmServerSocket: BluetoothServerSocket? by lazy(LazyThreadSafetyMode.NONE) {
            bluetoothAdapter?.listenUsingRfcommWithServiceRecord(HomeBindActivity.NAME, uuid)
        }
        private lateinit var mmInStream: InputStream
        init {
            Log.d(TAG, "create BroadcastThread")
            blState = HomeBindActivity.BL_STATE_LISTEN
        }

        override fun run() {
            var shouldLoop = true
            while (shouldLoop) {
                val socket: BluetoothSocket? = try {
                    mmServerSocket?.accept()
                } catch (e: IOException) {
                    Log.e(TAG, "Socket's accept() method failed", e)
                    shouldLoop = false
                    null
                }
                socket?.also {
                    manageAcceptedSocket(it)
                    mmServerSocket?.close()
                    shouldLoop = false
                }
            }
        }

        private fun manageAcceptedSocket(socket: BluetoothSocket) {
            Log.i(TAG, "BEGIN manageAcceptedSocket")
            var tmpIn: InputStream? = null
            // Get input streams
            try {
                tmpIn = socket.inputStream
                mmInStream = tmpIn
            } catch (e: IOException) {
                Log.e(TAG, "temp sockets not created", e)
            }
            val buffer = ByteArray(1024)
            var bytes: Int
            // Keep listening to the InputStream while connected
            while (blState === HomeBindActivity.BL_STATE_LISTEN) {
                try {
                    // Read from the InputStream
                    bytes = mmInStream!!.read(buffer)
                    // Send the obtained bytes to the UI Activity
                    mHandler.obtainMessage(HomeBindActivity.MESSAGE_READ, bytes, -1, buffer)
                        .sendToTarget()
                } catch (e: IOException) {
                    Log.e(TAG, "disconnected", e)
                    cancel()
                    break
                }
            }
        }

        fun cancel() {
            try {
                mmServerSocket?.close()
                blState == HomeBindActivity.BL_STATE_NONE
            } catch (e: IOException) {
                Log.e(TAG, "Could not close the connect socket", e)
            }
        }
    }

    //location
    private val LocationPermission = 1
    private fun requestLocationPermission() {
        if (ActivityCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
            !== PackageManager.PERMISSION_GRANTED
        ) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), LocationPermission
                )
            } else {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), LocationPermission
                )
            }
        }
    }
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>,
                                            grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            LocationPermission -> {
                if (grantResults.isNotEmpty() && grantResults[0] ==
                    PackageManager.PERMISSION_GRANTED
                ) {
                    if ((ContextCompat.checkSelfPermission(
                            this,
                            Manifest.permission.ACCESS_FINE_LOCATION
                        ) ===
                                PackageManager.PERMISSION_GRANTED)
                    ) {
                        Toast.makeText(
                            applicationContext,
                            "Location Permission Granted",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    Toast.makeText(
                        applicationContext,
                        "Location Permission Is Necesary",
                        Toast.LENGTH_LONG
                    ).show()
                }
                return
            }
        }
    }

    //my image
    private fun createMyImg() {
        val takePhotoIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        val myimgfile: File = getPhotoFile(MY_IMG_FILE_NAME)
        val providerFile =
            FileProvider.getUriForFile(
                applicationContext,
                "com.kliaou.fileprovider",
                myimgfile
            )
        takePhotoIntent.putExtra(MediaStore.EXTRA_OUTPUT, providerFile)
        val startForResult =
            registerForActivityResult(StartActivityForResult()) { result: ActivityResult? ->
                when (result?.resultCode) {
                    RESULT_OK -> {
                        //val imageBitmap = result.data?.extras?.get("data") as Bitmap
                        var imageBitmap = BitmapFactory.decodeFile(myimgfile.absolutePath)
                        //compress image
                        imageBitmap = compressImage(imageBitmap)
                        //show image
                        _binding.myImg.setImageBitmap(imageBitmap)
                    }
                    RESULT_CANCELED -> {
                    }
                }
            }
        _binding.myImg.setOnClickListener {
            startForResult.launch(takePhotoIntent)
        }
    }
    private fun getPhotoFile(fileName: String): File {
        val directoryStorage = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(fileName, ".jpg", directoryStorage)
    }
    private fun compressImage(image: Bitmap): Bitmap? {
        val baos = ByteArrayOutputStream()
        var options = 100
        //read data
        image.compress(Bitmap.CompressFormat.JPEG, options, baos)
        //compress to 300kb
        while ((baos.toByteArray().size / 1024) > 300) {
            baos.reset()
            options -= 10
            image.compress(Bitmap.CompressFormat.JPEG, options, baos)
        }
        val isBm = ByteArrayInputStream(baos.toByteArray())
        return BitmapFactory.decodeStream(isBm, null, null)
    }

    companion object {
        val MY_IMG_FILE_NAME = "myimg"
    }
}

