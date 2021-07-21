package com.kliaou.ui.home

import android.Manifest
import android.app.Activity.*
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.*
import android.content.ContentValues.TAG
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts.*
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.kliaou.databinding.FragmentHomeBinding
import com.kliaou.scanresult.RecyclerAdapter
import com.kliaou.scanresult.RecyclerItem
import com.kliaou.ui.BindActivity
import java.io.File
import java.io.IOException
import java.util.*
import kotlin.collections.ArrayList

class HomeFragment : Fragment() {
    private lateinit var homeViewModel: HomeViewModel
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private lateinit var recyclerAdapter: RecyclerAdapter
    private lateinit var scanResultLinearLayoutManager: LinearLayoutManager
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        homeViewModel = ViewModelProvider(this).get(HomeViewModel::class.java)
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root
        val textView: TextView = binding.textHome
        homeViewModel.text.observe(viewLifecycleOwner, { textView.text = it })
        return root
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        //scan result list
        scanResultLinearLayoutManager = LinearLayoutManager(activity)
        binding.listviewScanresult.layoutManager = scanResultLinearLayoutManager
        scanResults = ArrayList()
        recyclerAdapter = RecyclerAdapter(scanResults)
        binding.listviewScanresult.adapter = recyclerAdapter
        //location
        requestLocationPermission()
        //bluetooth
        createBl()
        //my image
        createMyImg()
    }
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    //bluetooth
    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private lateinit var scanResults: ArrayList<RecyclerItem>
    private var isScanning = false
    private var isBroadcasting = false
    private lateinit var broadcastThread: BroadcastThread
    private var blState = BindActivity.BL_STATE_NONE
    private fun createBl() {
        //check if bluetooth is available or not
        if (bluetoothAdapter == null) {
            binding.textServerInfo1.text = "Bluetooth is not available"
        } else {
            binding.textServerInfo1.text = "Bluetooth is available"
        }
        //turn on bluetooth
        if (bluetoothAdapter?.isEnabled == true) {
            binding.textServerInfo2.text = "enabled"
        } else {
            binding.textServerInfo2.text = "disabled"
            Toast.makeText(this.context, "Turning On Bluetooth...", Toast.LENGTH_LONG).show()
            //intent to on bluetooth
            val intent1 = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            val startForResult1 =
                registerForActivityResult(StartActivityForResult()) { result: ActivityResult? ->
                    when (result?.resultCode) {
                        RESULT_OK -> {
                            Toast.makeText(this.context, "Bluetooth Turned On.", Toast.LENGTH_LONG)
                                .show()
                            binding.textServerInfo2.text = "enabled"
                        }
                        RESULT_CANCELED -> {
                            Toast.makeText(
                                this.context,
                                "Bluetooth Cannot Be Turned On.",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }
            startForResult1.launch(intent1)
        }
        //btn_search
        setBtnSearchBkColor()
        binding.btnSearch.setOnClickListener {
            if (bluetoothAdapter?.isDiscovering == false) {
                val filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
                filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
                context?.registerReceiver(bScanResult, filter)
                bluetoothAdapter.startDiscovery()
                scanResults.clear()//rescan
                recyclerAdapter.notifyDataSetChanged()
//                val pairedDevices: Set<BluetoothDevice>? = bluetoothAdapter?.bondedDevices
//                pairedDevices?.forEach { device ->
//                    val deviceName = device.name
//                    val deviceHardwareAddress = device.address // MAC address
//                }
                isScanning = true
            } else {
                context?.unregisterReceiver(bScanResult)
                bluetoothAdapter?.cancelDiscovery()
                isScanning = false
            }
            setBtnSearchBkColor()
        }
        //btn_broadcast
        //make bluetooth discoverable
        binding.textServerInfo3.text = "NOT discoverable"
        Toast.makeText(this.context, "Making Your Device Discoverable", Toast.LENGTH_LONG)
            .show()
        val intent2 = Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE)
        val startForResult2 =
            registerForActivityResult(StartActivityForResult()) { result: ActivityResult? ->
                when (result?.resultCode) {
                    RESULT_OK -> {
                        Toast.makeText(
                            this.context,
                            "Bluetooth Discoverable.",
                            Toast.LENGTH_LONG
                        )
                            .show()
                        binding.textServerInfo3.text = "discoverable"
                    }
                    RESULT_CANCELED -> {
                        Toast.makeText(
                            this.context,
                            "Bluetooth NOT Discoverable",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        startForResult2.launch(intent2)
        setBtnBroadcastBkColor()
        binding.btnBroadcast.setOnClickListener {
            if (!isBroadcasting) {
                isBroadcasting = true
                //run broadcast thread
                if(blState != BindActivity.BL_STATE_NONE){
                    broadcastThread.cancel()
                }
                broadcastThread = BroadcastThread(BindActivity.MALE_UUID)
                broadcastThread.start()
            } else {
                isBroadcasting = false
                if(blState != BindActivity.BL_STATE_NONE){
                    broadcastThread.cancel()
                }
            }
            setBtnBroadcastBkColor()
        }
    }
    //scan
    private fun setBtnSearchBkColor() {
        if (isScanning) binding.btnSearch.backgroundTintList = ColorStateList.valueOf(Color.RED)
        else binding.btnSearch.backgroundTintList = ColorStateList.valueOf(Color.GRAY)
    }
    private val bScanResult = object: BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            if(action == BluetoothDevice.ACTION_FOUND) {
                val device =
                    intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                if (device != null) {
                    addToScanResults(device)
                }
            } else if(action == BluetoothAdapter.ACTION_DISCOVERY_FINISHED) {
                bluetoothAdapter?.cancelDiscovery()
                isScanning = false
                setBtnSearchBkColor()
            }
        }
    }
    private fun addToScanResults(device: BluetoothDevice) {
        if (!scanResults.any { it.Address === device.address }) {
            val recyclerItem = RecyclerItem(null, device.name, device.address)
            scanResults.add(recyclerItem)
            recyclerAdapter.notifyItemInserted(scanResults.size - 1)
        }
    }
    //broadcast
    private fun setBtnBroadcastBkColor() {
        if (isBroadcasting) {
            binding.btnBroadcast.backgroundTintList = ColorStateList.valueOf(Color.RED)
        } else {
            binding.btnBroadcast.backgroundTintList = ColorStateList.valueOf(Color.GRAY)
        }
    }
    private inner class BroadcastThread(uuid: UUID) : Thread() {
        private val mmServerSocket: BluetoothServerSocket? by lazy(LazyThreadSafetyMode.NONE) {
            bluetoothAdapter?.listenUsingRfcommWithServiceRecord(BindActivity.NAME, uuid)
        }

        init {
            blState = BindActivity.BL_STATE_LISTEN
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

        fun cancel() {
            try {
                mmServerSocket?.close()
            } catch (e: IOException) {
                Log.e(TAG, "Could not close the connect socket", e)
            }
        }

        private fun manageAcceptedSocket(socket: BluetoothSocket) {

        }
    }

    //location
    private val LocationPermission = 1
    private fun requestLocationPermission() {
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            )
            !== PackageManager.PERMISSION_GRANTED
        ) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(
                    requireActivity(),
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
            ) {
                ActivityCompat.requestPermissions(
                    requireActivity(),
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), LocationPermission
                )
            } else {
                ActivityCompat.requestPermissions(
                    requireActivity(),
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), LocationPermission
                )
            }
        }
    }
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>,
                                            grantResults: IntArray) {
        when (requestCode) {
            LocationPermission -> {
                if (grantResults.isNotEmpty() && grantResults[0] ==
                    PackageManager.PERMISSION_GRANTED
                ) {
                    if ((ContextCompat.checkSelfPermission(
                            requireActivity(),
                            Manifest.permission.ACCESS_FINE_LOCATION
                        ) ===
                                PackageManager.PERMISSION_GRANTED)
                    ) {
                        Toast.makeText(
                            this.context,
                            "Location Permission Granted",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                } else {
                    Toast.makeText(
                        this.context,
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
        val MY_IMG_FILE_NAME: String = "myimg"
        val takePhotoIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        val myimgfile: File = getPhotoFile(MY_IMG_FILE_NAME)
        val providerFile =
            FileProvider.getUriForFile(
                this.requireContext(),
                "com.kliaou.fileprovider",
                myimgfile
            )
        takePhotoIntent.putExtra(MediaStore.EXTRA_OUTPUT, providerFile)
        val startForResult =
            registerForActivityResult(StartActivityForResult()) { result: ActivityResult? ->
                when (result?.resultCode) {
                    RESULT_OK -> {
//                        val imageBitmap = result.data?.extras?.get("data") as Bitmap
                        val imageBitmap = BitmapFactory.decodeFile(myimgfile.absolutePath)
                        binding.myImg.setImageBitmap(imageBitmap)
                    }
                    RESULT_CANCELED -> {
                    }
                }
            }
        binding.myImg.setOnClickListener {
            startForResult.launch(takePhotoIntent)
        }
    }
    private fun getPhotoFile(fileName: String): File {
        val directoryStorage = activity?.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(fileName, ".jpg", directoryStorage)
    }
}