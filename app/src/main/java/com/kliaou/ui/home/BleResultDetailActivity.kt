package com.kliaou.ui.home

import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattService
import android.content.*
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ExpandableListView
import android.widget.ExpandableListView.OnChildClickListener
import android.widget.SimpleExpandableListAdapter
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.kliaou.R
import com.kliaou.service.BleGattAttributes
import com.kliaou.service.BleGattService
import java.util.*

class BleResultDetailActivity : AppCompatActivity() {
    private var mConnectionState: TextView? = null
    private var mDataField: TextView? = null
    private var mDeviceName: String? = null
    private var mDeviceAddress: String? = null
    private var mGattServicesList: ExpandableListView? = null
    private var mBleGattService: BleGattService? = null
    private var mGattCharacteristics: ArrayList<ArrayList<BluetoothGattCharacteristic>>? =
        ArrayList()
    private var mConnected = false
    private var mNotifyCharacteristic: BluetoothGattCharacteristic? = null
    private val LIST_NAME = "NAME"
    private val LIST_UUID = "UUID"

    // Code to manage Service lifecycle.
    private val mServiceConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(componentName: ComponentName, service: IBinder) {
            mBleGattService = (service as BleGattService.LocalBinder).getService()
            if (!mBleGattService!!.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth")
                finish()
            }
            // Automatically connects to the device upon successful start-up initialization.
            mBleGattService!!.connect(mDeviceAddress)
        }
        override fun onServiceDisconnected(componentName: ComponentName) {
            mBleGattService = null
        }
    }
    /*
    // Handles various events fired by the Service.
    // ACTION_GATT_CONNECTED: connected to a GATT server.
    // ACTION_GATT_DISCONNECTED: disconnected from a GATT server.
    // ACTION_GATT_SERVICES_DISCOVERED: discovered GATT services.
    // ACTION_DATA_AVAILABLE: received data from the device.  This can be a result of read
    //                        or notification operations.
     */
    private val mGattUpdateReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                BleGattService.ACTION_GATT_CONNECTED -> {
                    mConnected = true
                    updateConnectionState(R.string.connected)
                    invalidateOptionsMenu()
                }
                BleGattService.ACTION_GATT_DISCONNECTED -> {
                    mConnected = false
                    updateConnectionState(R.string.disconnected)
                    invalidateOptionsMenu()
                    clearUI()
                }
                BleGattService.ACTION_GATT_SERVICES_DISCOVERED -> {
                    // Show all the supported services and characteristics on the user interface.
                    displayGattServices(mBleGattService?.getSupportedGattServices())
                }
                BleGattService.ACTION_DATA_AVAILABLE -> {
                    displayData(intent.getStringExtra(BleGattService.EXTRA_DATA))
                }
            }
        }
    }
    /*
    // If a given GATT characteristic is selected, check for supported features.
    // hear we check 'Read' and 'Notify' features.
    // See http://d.android.com/reference/android/bluetooth/BluetoothGatt.html for the complete
    // list of supported characteristic features.
    */
    private val servicesListClickListner =
        OnChildClickListener { parent, v, groupPosition, childPosition, id ->
            if (mGattCharacteristics != null) {
                val characteristic = mGattCharacteristics!![groupPosition][childPosition]
                val charaProp = characteristic.properties
                if (charaProp or BluetoothGattCharacteristic.PROPERTY_READ > 0) {
                    // If there is an active notification on a characteristic, clear
                    // it first so it doesn't update the data field on the user interface.
                    if (mNotifyCharacteristic != null) {
                        mBleGattService?.setCharacteristicNotification(
                            mNotifyCharacteristic!!, false
                        )
                        mNotifyCharacteristic = null
                    }
                    mBleGattService?.readCharacteristic(characteristic)
                }
                if (charaProp or BluetoothGattCharacteristic.PROPERTY_NOTIFY > 0) {
                    mNotifyCharacteristic = characteristic
                    mBleGattService?.setCharacteristicNotification(
                        characteristic, true
                    )
                }
                return@OnChildClickListener true
            }
            false
        }

    private fun clearUI() {
        mGattServicesList!!.setAdapter(null as SimpleExpandableListAdapter?)
        mDataField!!.setText(R.string.no_data)
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.ble_activity_result_detail)

        val intent = intent
        mDeviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME)
        mDeviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS)

        // Sets up UI references.
        (findViewById<View>(R.id.device_address) as TextView).text = mDeviceAddress
        mGattServicesList = findViewById<View>(R.id.gatt_services_list) as ExpandableListView
        mGattServicesList!!.setOnChildClickListener(servicesListClickListner)
        mConnectionState = findViewById<View>(R.id.connection_state) as TextView
        mDataField = findViewById<View>(R.id.data_value) as TextView
        supportActionBar?.title = mDeviceName
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        val gattServiceIntent = Intent(this, BleGattService::class.java)
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE)
    }
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.gatt_services, menu)
        if (mConnected) {
            menu.findItem(R.id.menu_connect).isVisible = false
            menu.findItem(R.id.menu_disconnect).isVisible = true
        } else {
            menu.findItem(R.id.menu_connect).isVisible = true
            menu.findItem(R.id.menu_disconnect).isVisible = false
        }
        return true
    }
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_connect -> {
                mBleGattService?.connect(mDeviceAddress)
                return true
            }
            R.id.menu_disconnect -> {
                mBleGattService?.disconnect()
                return true
            }
            android.R.id.home -> {
                onBackPressed()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onResume() {
        super.onResume()
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter())
        if (mBleGattService != null) {
            val result: Boolean = mBleGattService?.connect(mDeviceAddress) == true
            Log.d(
                TAG,
                "Connect request result=$result"
            )
        }
    }
    override fun onPause() {
        super.onPause()
        unregisterReceiver(mGattUpdateReceiver)
    }
    override fun onDestroy() {
        super.onDestroy()
        unbindService(mServiceConnection)
        mBleGattService = null
    }

    private fun updateConnectionState(resourceId: Int) {
        runOnUiThread { mConnectionState!!.setText(resourceId) }
    }
    private fun displayData(data: String?) {
        if (data != null) {
            mDataField!!.text = data
        }
    }
    /*
    // Iterate through the supported GATT Services/Characteristics.
    // Populate the data structure that is bound to the ExpandableListView
     */
    private fun displayGattServices(gattServices: List<BluetoothGattService>?) {
        if (gattServices == null) return
        var uuid: String?
        val unknownServiceString = resources.getString(R.string.unknown_service)
        val unknownCharaString = resources.getString(R.string.unknown_characteristic)
        val gattServiceData = ArrayList<HashMap<String, String?>>()
        val gattCharacteristicData = ArrayList<ArrayList<HashMap<String, String?>>>()
        mGattCharacteristics = ArrayList()

        // Loops through available GATT Services.
        for (gattService in gattServices) {
            val currentServiceData = HashMap<String, String?>()
            uuid = gattService.uuid.toString()
            currentServiceData[LIST_NAME] = BleGattAttributes.lookup(uuid, unknownServiceString)
            if(currentServiceData[LIST_NAME] == unknownServiceString) continue
            currentServiceData[LIST_UUID] = uuid
            gattServiceData.add(currentServiceData)
            val gattCharacteristicGroupData = ArrayList<HashMap<String, String?>>()
            val gattCharacteristics = gattService.characteristics
            val charas = ArrayList<BluetoothGattCharacteristic>()

            // Loops through available Characteristics.
            for (gattCharacteristic in gattCharacteristics) {
                charas.add(gattCharacteristic)
                val currentCharaData = HashMap<String, String?>()
                uuid = gattCharacteristic.uuid.toString()
                currentCharaData[LIST_NAME] = BleGattAttributes.lookup(uuid, unknownCharaString)
                if(currentCharaData[LIST_NAME] == unknownCharaString) continue
                currentCharaData[LIST_UUID] = uuid
                gattCharacteristicGroupData.add(currentCharaData)
            }
            mGattCharacteristics!!.add(charas)
            gattCharacteristicData.add(gattCharacteristicGroupData)
        }
        val gattServiceAdapter = SimpleExpandableListAdapter(
            this,
            gattServiceData,
            android.R.layout.simple_expandable_list_item_2,
            arrayOf(LIST_NAME, LIST_UUID),
            intArrayOf(android.R.id.text1, android.R.id.text2),
            gattCharacteristicData,
            android.R.layout.simple_expandable_list_item_2,
            arrayOf(LIST_NAME, LIST_UUID),
            intArrayOf(android.R.id.text1, android.R.id.text2)
        )
        mGattServicesList!!.setAdapter(gattServiceAdapter)
    }

    companion object {
        private val TAG = BleResultDetailActivity::class.java.simpleName
        const val EXTRAS_DEVICE_NAME = "DEVICE_NAME"
        const val EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS"
        private fun makeGattUpdateIntentFilter(): IntentFilter {
            val intentFilter = IntentFilter()
            intentFilter.addAction(BleGattService.ACTION_GATT_CONNECTED)
            intentFilter.addAction(BleGattService.ACTION_GATT_DISCONNECTED)
            intentFilter.addAction(BleGattService.ACTION_GATT_SERVICES_DISCOVERED)
            intentFilter.addAction(BleGattService.ACTION_DATA_AVAILABLE)
            return intentFilter
        }
    }
}