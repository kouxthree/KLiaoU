package com.kliaou.ui

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.bluetooth.*
import android.bluetooth.le.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Color
import android.location.LocationManager
import android.os.Bundle
import android.provider.Settings
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.SwitchCompat
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.kliaou.*
import com.kliaou.blerecycler.BleConnectRecyclerAdapter
import com.kliaou.blerecycler.BleRecyclerItem
import com.kliaou.blerecycler.BleScanRecyclerAdapter
import com.kliaou.databinding.BleActivityHomeMainBinding
import com.kliaou.databinding.BleHomeLocationBinding
import com.kliaou.db.Gender
import com.kliaou.db.RepSetting
import com.kliaou.service.BleAdvertiserService
import com.kliaou.service.BleGattAttributes
import com.kliaou.service.BleGattServer
import java.util.*
import kotlin.collections.ArrayList

class BleHomeActivity : AppCompatActivity() {
    private lateinit var _binding: BleActivityHomeMainBinding
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.ble_main_menu_setttings -> {
                val intent = Intent(applicationContext, BleHomeSettingActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
//                applicationContext.startActivity(intent)
                startBleMainSettingActivity.launch(intent)
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.ble_main_menu, menu)
        return true
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //init//intent was null before onCreate
        _binding = BleActivityHomeMainBinding.inflate(layoutInflater)
        //setContentView(R.layout.activity_bind)
        setContentView(_binding.root)
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)//dark mode
        supportActionBar?.setDisplayHomeAsUpEnabled(false)//not display return home arrow
        //bluetooth
        enableBluetooth()
        //location permission
        requestLocationPermission()
        //location state
        turnOnLocationState()
        //advertisement
        createAdvertisement()
        //scanner
        createScanner()
        //set my location edit dialog clicked listener
        _binding.textMyLocation.setOnClickListener(myLocationClickedListener())
        //set my location changed listener
        _binding.textMyLocation.addTextChangedListener(object:  TextWatcher{
            override fun afterTextChanged(p0: Editable?) {
                if(_binding.textMyLocation.text == getString(R.string.my_location)) {
                    broadcastLocation = ""
                } else  if(_binding.textMyLocation.text.isNullOrBlank()) {
                    _binding.textMyLocation.text = getString(R.string.my_location)
                    broadcastLocation = ""
                } else {
                    broadcastLocation = _binding.textMyLocation.text.toString()
                }
                notifyRegisteredDevices()
            }
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
        })
        //connected device
        createConnectDevice()
    }

    //start setting activity
    private val startBleMainSettingActivity =
        registerForActivityResult(StartActivityForResult()) {
            if (it.resultCode == Activity.RESULT_OK && broadcastNotifyFlag) {
//                val value = it.data?.getStringExtra("input")
                notifyRegisteredDevices()
            }
        }
    //my location edit dialog clicked listener
    private fun myLocationClickedListener() = View.OnClickListener {
//        val imm: InputMethodManager = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
//        val strBeforeEdited = _binding.textMyLocation.text //string before edited
        val alert = AlertDialog.Builder(this)
        val mBinding = BleHomeLocationBinding.inflate(layoutInflater)
        if(_binding.textMyLocation.text != getString(R.string.my_location)) {
            mBinding.txtLocation.setText(_binding.textMyLocation.text)
        }
        alert.setView(mBinding.root.rootView)
        val alertDialog = alert.create()
        alertDialog.setCanceledOnTouchOutside(true)
        //cancel button
        mBinding.btnCancel.setOnClickListener {
            alertDialog.dismiss()
        }
        //okay button
        mBinding.btnOkay.setOnClickListener {
            _binding.textMyLocation.text = mBinding.txtLocation.text
            alertDialog.dismiss()
        }
        //text send action// combine with xml setting -> android:imeOptions="actionSend"
        mBinding.txtLocation.setOnEditorActionListener { v, actionId, event ->
            var handled = false
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                _binding.textMyLocation.text = mBinding.txtLocation.text
                alertDialog.dismiss()
                handled = true
            }
            handled
        }
//        //focus changed listener
//        mBinding.txtLocation.setOnFocusChangeListener { v, hasFocus ->
//            if (hasFocus) {
//                imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0)
////                imm.showSoftInput(v, InputMethodManager.SHOW_IMPLICIT)
//            }
//        }
//        //dialog dismiss listener
//        alertDialog.setOnDismissListener {
//            imm.hideSoftInputFromWindow(_binding.textMyLocation.windowToken, 0)
//        }
        alertDialog.show()
//        mBinding.txtLocation.requestFocus()
    }

    //bluetooth
    private lateinit var bluetoothManager: BluetoothManager
    private fun enableBluetooth() {
        //init bluetooth manager
        bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.getAdapter()
        //turn on bluetooth
        if (bluetoothAdapter?.isEnabled == true) {
            _binding.textServerInfo1.text = ""
        } else {
            _binding.textServerInfo1.text = getString(R.string.bl_not_available)
            Toast.makeText(applicationContext, "Turning On Bluetooth...", Toast.LENGTH_LONG).show()
            //intent to on bluetooth
            val intentTurnOnBt = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            val startForResultTurnOnBt =
                registerForActivityResult(StartActivityForResult()) { result: ActivityResult? ->
                    when (result?.resultCode) {
                        RESULT_OK -> {
                            Toast.makeText(applicationContext, "Bluetooth Turned On.", Toast.LENGTH_SHORT)
                                .show()
                            _binding.textServerInfo1.text = ""
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
            if (view.isChecked) {
                startAdvertising()
                startGattServer()
            } else {
                stopGattServer()
                stopAdvertising()
            }
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

    //gatt server/callers to BleGattServer
    /* Collection of notification subscribers */
    private val registeredDevices = mutableSetOf<BluetoothDevice>()
    /**
     * Initialize the GATT server instance with the services/characteristics
     */
    private fun startGattServer() {
        BleGattServer.startGattServer()
        setBroadcastNickname()
        setBroadcastLocation()
    }
    //set nickname for broadcasting
    private fun setBroadcastNickname() {
        //read from db
        val repsetting = RepSetting()
        broadcastNickname = repsetting.entitySetting?.nickName.toString()
    }
    //set location for broadcasting
    private fun setBroadcastLocation() {
        broadcastLocation = _binding.textMyLocation.text.toString()
    }
    /**
     * Shut down the GATT server.
     */
    private fun stopGattServer() {
        BleGattServer.stopGattServer()
    }
    /**
     * Send a service notification to any devices that are subscribed
     * to the characteristic.
     */
    //private fun notifyRegisteredDevices(timestamp: Long, adjustReason: Byte) {
    private fun notifyRegisteredDevices() {
        BleGattServer.notifyRegisteredDevices()
    }
    //connected devices area
    private lateinit var bleConnectRecyclerAdapter: BleConnectRecyclerAdapter
    private fun createConnectDevice() {
        //connected devices list
        val connectDeviceLinearLayoutManager = LinearLayoutManager(this)
        _binding.listviewConnected.layoutManager = connectDeviceLinearLayoutManager
        bleConnectRecyclerAdapter = BleConnectRecyclerAdapter()
        _binding.listviewConnected.adapter = bleConnectRecyclerAdapter
    }
    private fun addConnectDevice(device: BluetoothDevice) {
        val item = BleRecyclerItem(
            Name = device.name,
            Address = device.address,
            Timestamp = System.currentTimeMillis()
        )
        Log.d(TAG, "add connect device: single")
        bleConnectRecyclerAdapter.addSingleItem(item)
    }
    private fun removeConnectDevice(device: BluetoothDevice) {
        val item = BleRecyclerItem(
            Name = device.name,
            Address = device.address,
            Timestamp = System.currentTimeMillis()
        )
        Log.d(TAG, "remove connect device: single")
        bleConnectRecyclerAdapter.removeSingleItem(item)
    }

    //scan
    private lateinit var bleScanRecyclerAdapter: BleScanRecyclerAdapter
    private var scanCallback: ScanCallback? = null
    private var bluetoothLeScanner: BluetoothLeScanner? = null
    //private var handler: Handler? = null
    private fun createScanner() {
        //handler = Handler(Looper.myLooper()!!)
        val scanResultLinearLayoutManager = LinearLayoutManager(this)
        if (bluetoothLeScanner == null) initBluetoothLeScanner()
        //scan result list
        _binding.listviewScanresult.layoutManager = scanResultLinearLayoutManager
        bleScanRecyclerAdapter = BleScanRecyclerAdapter()
        _binding.listviewScanresult.adapter = bleScanRecyclerAdapter
        //btn_search
        setBtnSearchBkColor(false)
        _binding.btnSearch.setOnClickListener {
            if (!getScanningState()) {
                bleScanRecyclerAdapter.clearItems()
                requestLocationPermission()
                startScanning()
                setBtnSearchBkColor(true)
            } else {
                stopScanning()
                setBtnSearchBkColor(false)
            }
        }
    }
    private fun initBluetoothLeScanner() {
        val manager =
            applicationContext.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val bluetoothAdapter = manager.adapter
        bluetoothLeScanner = bluetoothAdapter.bluetoothLeScanner
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
        if(bluetoothLeScanner == null) initBluetoothLeScanner()
        bluetoothLeScanner?.startScan(buildScanFilters(), buildScanSettings(), scanCallback)
    }
    private fun stopScanning() {
        Log.d(TAG, "stopScanning")
        bluetoothLeScanner?.stopScan(scanCallback)
        scanCallback = null
        // update 'last seen' times even though there are no new results
        bleScanRecyclerAdapter.notifyDataSetChanged()
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
        //read remote gender from db
        val repsetting = RepSetting()
        val remoteGender = repsetting.entitySetting?.remoteGender
         //male filter
        val filter1 = ScanFilter.Builder()
        filter1.setServiceUuid(ADVERTISE_UUID)
            .setServiceData(ADVERTISE_UUID, byteArrayOf(ADVERTISE_DATA_MALE))
        //female filter
        val filter2 = ScanFilter.Builder()
        filter2.setServiceUuid(ADVERTISE_UUID)
            .setServiceData(ADVERTISE_UUID, byteArrayOf(ADVERTISE_DATA_FEMALE))
        //either filter
        val filter3 = ScanFilter.Builder()
        filter3.setServiceUuid(ADVERTISE_UUID)
            .setServiceData(ADVERTISE_UUID, byteArrayOf(ADVERTISE_DATA_MALE, ADVERTISE_DATA_FEMALE))
        //return listOf(scanFilter)
        val lst = ArrayList<ScanFilter>()
        lst.add(filter3.build())//always scan either
        when(remoteGender) {
            Gender.MALE -> lst.add(filter1.build())
            Gender.FEMALE -> lst.add(filter2.build())
            else -> {
                lst.add(filter1.build())
                lst.add(filter2.build())
            }
        }
        Log.d(TAG, "buildScanFilters")
        return lst
    }
    inner class BleScanCallback : ScanCallback() {
        override fun onBatchScanResults(results: MutableList<ScanResult>?) {
            super.onBatchScanResults(results)
            Log.d(TAG, "onBatchScanResults size: ${results?.size}")
            results?.let { bleScanRecyclerAdapter.setItems(it) }
        }
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            super.onScanResult(callbackType, result)
            Log.d(TAG, "onScanResult: single")
            bleScanRecyclerAdapter.addSingleItem(result)
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
    //location permission
    private val LocationPermission = 1
    private fun requestLocationPermission() {
        _binding.textServerInfo2.text = ""
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
        _binding.textServerInfo2.text = getString(R.string.loc_not_available)
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
                        _binding.textServerInfo2.text = ""
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
    //location state
    private fun turnOnLocationState() {
        //state check
        val locationManager = applicationContext.getSystemService(LOCATION_SERVICE) as LocationManager
        val locationState = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        if (!locationState) {
            val adb = AlertDialog.Builder(this)
            adb.setTitle(getString(R.string.gps_not_available))
            adb.setIcon(android.R.drawable.ic_dialog_alert)
            adb.setMessage(getString(R.string.goto_gps_setting_msg))
            adb.setCancelable(true)
            adb.setPositiveButton("OK") { _, _ ->
                val intent1 = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                startActivity(intent1)
            }
            adb.setNegativeButton("Cancel") { _, _ ->
                Toast.makeText(
                    applicationContext,
                    "Location state NOT turned on",
                    Toast.LENGTH_LONG
                ).show()
            }
            adb.show()
        }
    }

    companion object {
        private val TAG = BleHomeActivity::class.java.simpleName
        var broadcastNickname: String = ""
        var broadcastLocation: String = ""
        var broadcastNotifyFlag: Boolean = false
    }
}

