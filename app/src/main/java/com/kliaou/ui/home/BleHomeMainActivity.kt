package com.kliaou.ui.home

import android.Manifest
import android.bluetooth.*
import android.bluetooth.le.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.*
import android.provider.MediaStore
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.kliaou.*
import com.kliaou.blescanresult.BleRecyclerAdapter
import com.kliaou.databinding.BleActivityHomeMainBinding
import com.kliaou.service.BleAdvertiserService
import java.io.*

private const val TAG = "BleHomeMainActivity"

class BleHomeMainActivity : AppCompatActivity() {
    private lateinit var _binding: BleActivityHomeMainBinding
    private lateinit var scanResultLinearLayoutManager: LinearLayoutManager
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
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
        _binding = BleActivityHomeMainBinding.inflate(layoutInflater)
        //setContentView(R.layout.activity_bind)
        setContentView(_binding.root)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)//menu action
        //bluetooth
        enableBluetooth()
        //location permission
        requestLocationPermission()
        //advertisement
        createAdvertisement()
        //scanner
        createScanner()
        //my image
        createMyImg()
    }

    //bluetooth
    private fun enableBluetooth() {
        val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
        //check if bluetooth is available or not
        if (bluetoothAdapter == null) {
            _binding.textServerInfo1.text = getString(R.string.bl_non_available)
        } else {
            _binding.textServerInfo1.text = getString(R.string.bl_available)
        }
        //turn on bluetooth
        if (bluetoothAdapter?.isEnabled == true) {
            _binding.textServerInfo2.text = getString(R.string.bl_enabled)
        } else {
            _binding.textServerInfo2.text = getString(R.string.bl_non_available)
            Toast.makeText(applicationContext, "Turning On Bluetooth...", Toast.LENGTH_LONG).show()
            //intent to on bluetooth
            val intentTurnOnBt = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            val startForResultTurnOnBt =
                registerForActivityResult(StartActivityForResult()) { result: ActivityResult? ->
                    when (result?.resultCode) {
                        RESULT_OK -> {
                            Toast.makeText(applicationContext, "Bluetooth Turned On.", Toast.LENGTH_SHORT)
                                .show()
                            _binding.textServerInfo2.text = getString(R.string.bl_enabled)
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
    }

    //advertisement
    private lateinit var btAdvertisingFailureReceiver: BroadcastReceiver
    override fun onResume() {
        super.onResume()
        _binding.advertiseSwitch.isChecked = BleAdvertiserService.running
        val failureFilter = IntentFilter(ADVERTISING_FAILED)
        registerReceiver(btAdvertisingFailureReceiver, failureFilter)
    }
    override fun onPause() {
        super.onPause()
        unregisterReceiver(btAdvertisingFailureReceiver)
    }
    private fun createAdvertisement() {
        btAdvertisingFailureReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                val errorCode = intent?.getIntExtra(BT_ADVERTISING_FAILED_EXTRA_CODE, INVALID_CODE)
                _binding.advertiseSwitch.isChecked = false
                var errMsg = when (errorCode) {
                    AdvertiseCallback.ADVERTISE_FAILED_ALREADY_STARTED -> getString(R.string.already_started)
                    AdvertiseCallback.ADVERTISE_FAILED_DATA_TOO_LARGE -> getString(R.string.data_too_large)
                    AdvertiseCallback.ADVERTISE_FAILED_FEATURE_UNSUPPORTED -> getString(R.string.not_supported)
                    AdvertiseCallback.ADVERTISE_FAILED_INTERNAL_ERROR -> getString(R.string.inter_err)
                    AdvertiseCallback.ADVERTISE_FAILED_TOO_MANY_ADVERTISERS -> "Too many advertisers."
                    ADVERTISING_TIMED_OUT -> "Timed out."
                    else -> "Error unknown."
                }
                errMsg = "Start advertising failed: $errMsg"
                Toast.makeText(applicationContext, errMsg, Toast.LENGTH_LONG).show()
            }
        }
        _binding.advertiseSwitch.setOnClickListener {
            val view = it as SwitchCompat
            if (view.isChecked) startAdvertising() else stopAdvertising()
            Log.d(TAG, "onViewCreated: switch clicked ")
        }
    }
    private fun startAdvertising() {
        applicationContext.startService(createServiceIntent())
    }
    private fun stopAdvertising() {
        applicationContext.stopService(createServiceIntent())
        _binding.advertiseSwitch.isChecked = false
    }
    private fun createServiceIntent(): Intent =
        Intent(applicationContext, BleAdvertiserService::class.java)

    //scan
    private lateinit var bleRecyclerAdapter: BleRecyclerAdapter
    private var scanCallback: ScanCallback? = null
    private var bluetoothLeScanner: BluetoothLeScanner? = null
    //private var handler: Handler? = null
    private fun createScanner() {
        //handler = Handler(Looper.myLooper()!!)
        if (bluetoothLeScanner == null) {
            val manager =
                applicationContext.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            val bluetoothAdapter = manager.adapter
            bluetoothLeScanner = bluetoothAdapter.bluetoothLeScanner
        }
        //scan result list
        scanResultLinearLayoutManager = LinearLayoutManager(this)
        _binding.listviewScanresult.layoutManager = scanResultLinearLayoutManager
        bleRecyclerAdapter = BleRecyclerAdapter()
        _binding.listviewScanresult.adapter = bleRecyclerAdapter
        //btn_search
        setBtnSearchBkColor(false)
        _binding.btnSearch.setOnClickListener {
            if (!getScanningState()) {
                bleRecyclerAdapter.clearItems()
                requestLocationPermission()
                startScanning()
                setBtnSearchBkColor(true)
            } else {
                stopScanning()
                setBtnSearchBkColor(false)
            }
        }
    }
    private fun buildScanSettings() = ScanSettings.Builder()
        .setScanMode(ScanSettings.SCAN_MODE_LOW_POWER).build()
    private fun startScanning() {
        Log.d(TAG, "startScanning")
        if (scanCallback != null) {
            Log.d(TAG, "startScanning: already scanning")
            Toast.makeText(applicationContext, getString(R.string.bt_scanning), Toast.LENGTH_LONG).show()
            return
        }
        //handler?.postDelayed({ stopScanning() }, SCAN_PERIOD_IN_MILLIS)
        scanCallback = BleScanCallback()
        bluetoothLeScanner?.startScan(buildScanFilters(), buildScanSettings(), scanCallback)
    }
    private fun stopScanning() {
        Log.d(TAG, "stopScanning")
        bluetoothLeScanner?.stopScan(scanCallback)
        scanCallback = null
        // update 'last seen' times even though there are no new results
        bleRecyclerAdapter.notifyDataSetChanged()
    }
    private fun getScanningState(): Boolean {
        var isScanning = false
        try {
            //biding is null when fragment not active
            val btn = findViewById<FloatingActionButton>(R.id.btn_search)
            isScanning = btn?.backgroundTintList == ColorStateList.valueOf(Color.RED)
        } catch (e: Exception) {
        }
        return isScanning
    }
    private fun setBtnSearchBkColor(isScanning: Boolean) {
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
    private fun buildScanFilters(): List<ScanFilter> {
        val scanFilter = ScanFilter.Builder()
            .setServiceUuid(ADVERTISE_UUID)
            .build()
        Log.d(TAG, "buildScanFilters")
        return listOf(scanFilter)
    }
    inner class BleScanCallback : ScanCallback() {
        override fun onBatchScanResults(results: MutableList<ScanResult>?) {
            super.onBatchScanResults(results)
            Log.d(TAG, "onBatchScanResults size: ${results?.size}")
            results?.let { bleRecyclerAdapter.setItems(it) }
        }
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            super.onScanResult(callbackType, result)
            Log.d(TAG, "onScanResult: single")
            bleRecyclerAdapter.addSingleItem(result)
        }
        override fun onScanFailed(errorCode: Int) {
            super.onScanFailed(errorCode)
            Log.d(TAG, "onScanFailed: errorCode $errorCode")
            Toast.makeText(
                applicationContext,
                "Scan failed with error code $errorCode",
                Toast.LENGTH_LONG
            ).show()
        }
    }
    //location
    private val LocationPermission = 1
    private fun requestLocationPermission() {
        if (ActivityCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
            != PackageManager.PERMISSION_GRANTED
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
                        ) ==
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
                        "Location Permission Is Necessary",
                        Toast.LENGTH_LONG
                    ).show()
                }
                return
            }
        }
    }

    //my image
    private fun createMyImg() {
        //deleteMyImgFile()//delete exists
        val takePhotoIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        val myimgfiletemp: File = getMyImgFileTemp()
        val providerFile =
            FileProvider.getUriForFile(
                applicationContext,
                "com.kliaou.fileprovider",
                myimgfiletemp
            )
        takePhotoIntent.putExtra(MediaStore.EXTRA_OUTPUT, providerFile)
        val startForResult =
            registerForActivityResult(StartActivityForResult()) { result: ActivityResult? ->
                when (result?.resultCode) {
                    RESULT_OK -> {
                        //val imageBitmap = result.data?.extras?.get("data") as Bitmap
                        var imageBitmap = BitmapFactory.decodeFile(myimgfiletemp.absolutePath)
                        //compress image
                        imageBitmap = compressImage(imageBitmap)
                        //show image
                        _binding.myImg.setImageBitmap(imageBitmap)
                        //save to file
                        saveBitmapToFile(imageBitmap)
                    }
                    RESULT_CANCELED -> {
                    }
                }
            }
        _binding.myImg.setOnClickListener {
            startForResult.launch(takePhotoIntent)
        }
    }
    private fun getMyImgFileTemp(): File {
        return File.createTempFile(MY_IMG_FILE_NAME, ".jpg", getExternalFilesDir(Environment.DIRECTORY_PICTURES))
    }
    private fun getMyImgFile():File {
        return File(getExternalFilesDir(Environment.DIRECTORY_PICTURES)!!.path + "/" + MY_IMG_FILE_NAME + ".jpg")
     }
    private fun compressImage(image: Bitmap): Bitmap? {
        val baos = ByteArrayOutputStream()
        var options = 100
        //read data
        image.compress(Bitmap.CompressFormat.JPEG, options, baos)
        //compress to 300kb
        while ((baos.toByteArray().size / 1024) > 300 ) {
            baos.reset()
            options -= 10
            image.compress(Bitmap.CompressFormat.JPEG, options, baos)
            if(options <= 10) break
        }
        val isBm = ByteArrayInputStream(baos.toByteArray())
        return BitmapFactory.decodeStream(isBm,null,null)
    }
    private fun saveBitmapToFile(image: Bitmap) {
        try {
            var myimg = getMyImgFile()
            if(!myimg.exists()) {
                myimg.createNewFile()
            }
            val fOut = FileOutputStream(myimg)
            image.compress(Bitmap.CompressFormat.JPEG, 100, fOut)
            fOut.flush()
            fOut.close()
        } catch (e: Exception) {
            Log.e(TAG, "Could not save file $MY_IMG_FILE_NAME")
        }
    }
}

