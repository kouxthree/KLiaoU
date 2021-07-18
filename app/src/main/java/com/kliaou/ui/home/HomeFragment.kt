package com.kliaou.ui.home

import android.app.Activity.*
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothAdapter.ACTION_DISCOVERY_FINISHED
import android.bluetooth.BluetoothAdapter.ACTION_DISCOVERY_STARTED
import android.bluetooth.BluetoothDevice
import android.content.*
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts.*
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.kliaou.databinding.FragmentHomeBinding
import com.kliaou.parts.RecyclerAdapter
import com.kliaou.parts.RecyclerItem
import kotlinx.android.synthetic.main.fragment_home.*
import kotlinx.android.synthetic.main.scanresult_item.*
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
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
        listview_scanresult.layoutManager = scanResultLinearLayoutManager
        scanResults = ArrayList()
        recyclerAdapter = RecyclerAdapter(scanResults)
        listview_scanresult.adapter = recyclerAdapter
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
    private var isScanning = false
    private lateinit var scanResults: ArrayList<RecyclerItem>
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
        //make bluetooth discoverable
        if (bluetoothAdapter!!.isDiscovering) {
            binding.textServerInfo3.text = "discoverable"
        } else {
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
        }
        //btn_search
        setBtnSearchBkColor()
        binding.btnSearch.setOnClickListener {
            val filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
            if (!isScanning) {
                activity?.registerReceiver(bScanResult, filter)
                bluetoothAdapter.startDiscovery()
                isScanning = true
                scanResults.clear()//rescan
            } else {
                activity?.unregisterReceiver(bScanResult)
                bluetoothAdapter.cancelDiscovery()
                isScanning = false
            }
            setBtnSearchBkColor()
        }
    }
    private fun setBtnSearchBkColor(){
        if(isScanning) binding.btnSearch.backgroundTintList = ColorStateList.valueOf(Color.RED)
        else binding.btnSearch.backgroundTintList = ColorStateList.valueOf(Color.GRAY)
    }
    private val bScanResult = object: BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            if (BluetoothDevice.ACTION_FOUND == action) {
                val device =
                    intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                val recyclerItem = RecyclerItem(null,device!!.name, device.address)
                scanResults.add(recyclerItem)
                recyclerAdapter.notifyItemInserted(scanResults.size-1)
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
                        my_img.setImageBitmap(imageBitmap)
                    }
                    RESULT_CANCELED -> {
                    }
                }
            }
        my_img.setOnClickListener {
            startForResult.launch(takePhotoIntent)
        }
    }
    private fun getPhotoFile(fileName: String): File {
        val directoryStorage = activity?.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(fileName, ".jpg", directoryStorage)
    }
}