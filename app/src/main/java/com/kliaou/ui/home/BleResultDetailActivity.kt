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
import com.kliaou.service.BleGattClientService
import java.util.*

class BleResultDetailActivity : AppCompatActivity() {
    private var mConnectionState: TextView? = null
    private var mRemoteGender: TextView? = null
    private var mRemoteNickname: TextView? = null
    private var mRemoteLocation: TextView? = null
    private var mDeviceName: String? = null
    private var mDeviceAddress: String? = null
    private var mBleGattClientService: BleGattClientService? = null
    private var mGattCharacteristics: ArrayList<ArrayList<BluetoothGattCharacteristic>>? =
        ArrayList()
    private var mConnected = false
    private var mNotifyCharacteristic: BluetoothGattCharacteristic? = null
    private val LIST_NAME = "NAME"
    private val LIST_UUID = "UUID"

    // Code to manage Service lifecycle.
    private val mServiceConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(componentName: ComponentName, service: IBinder) {
            mBleGattClientService = (service as BleGattClientService.LocalBinder).getService()
            if (!mBleGattClientService!!.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth")
                finish()
            }
            // Automatically connects to the device upon successful start-up initialization.
            mBleGattClientService!!.connect(mDeviceAddress)
        }
        override fun onServiceDisconnected(componentName: ComponentName) {
            mBleGattClientService = null
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
                BleGattClientService.ACTION_GATT_CONNECTED -> {
                    mConnected = true
                    updateConnectionState(R.string.connected)
                    invalidateOptionsMenu()
                }
                BleGattClientService.ACTION_GATT_DISCONNECTED -> {
                    mConnected = false
                    updateConnectionState(R.string.disconnected)
                    invalidateOptionsMenu()
                    clearUI()
                }
                BleGattClientService.ACTION_GATT_SERVICES_DISCOVERED -> {
                    // Show all the supported services and characteristics on the user interface.
                    displayGattServices(mBleGattClientService?.getSupportedGattServices())
                }
                BleGattClientService.ACTION_DATA_AVAILABLE -> {
                    displayData(intent)
                }
            }
        }
    }
    private fun displayData(intent: Intent) {
        val data: String = intent.getStringExtra(BleGattClientService.EXTRA_DATA) ?: return
        when(intent.getStringExtra(BleGattClientService.EXTRA_CHAR_UUID)) {
            BleGattAttributes.NICKNAME_CHAR -> mRemoteNickname!!.text = data
        }
    }

    private fun clearUI() {
        mRemoteGender!!.setText(R.string.no_data)
        mRemoteNickname!!.setText(R.string.no_data)
        mRemoteLocation!!.setText(R.string.no_data)
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.ble_activity_result_detail)

        val intent = intent
        mDeviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME)
        mDeviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS)

        // Sets up UI references.
        (findViewById<View>(R.id.device_address) as TextView).text = mDeviceAddress
        mConnectionState = findViewById<View>(R.id.connection_state) as TextView
        mRemoteGender = findViewById<View>(R.id.remote_gender) as TextView
        mRemoteNickname = findViewById<View>(R.id.remote_nickname) as TextView
        mRemoteLocation = findViewById<View>(R.id.remote_location) as TextView
        supportActionBar?.title = mDeviceName
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        val gattServiceIntent = Intent(this, BleGattClientService::class.java)
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
                mBleGattClientService?.connect(mDeviceAddress)
                return true
            }
            R.id.menu_disconnect -> {
                mBleGattClientService?.disconnect()
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
        if (mBleGattClientService != null) {
            val result: Boolean = mBleGattClientService?.connect(mDeviceAddress) == true
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
        mBleGattClientService = null
    }

    private fun updateConnectionState(resourceId: Int) {
        runOnUiThread { mConnectionState!!.setText(resourceId) }
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
            val chars = ArrayList<BluetoothGattCharacteristic>()

            // Loops through available Characteristics.
            for (gattCharacteristic in gattCharacteristics) {
                chars.add(gattCharacteristic)
                val currentCharaData = HashMap<String, String?>()
                uuid = gattCharacteristic.uuid.toString()
                currentCharaData[LIST_NAME] = BleGattAttributes.lookup(uuid, unknownCharaString)
                if(currentCharaData[LIST_NAME] == unknownCharaString) continue
                currentCharaData[LIST_UUID] = uuid
                gattCharacteristicGroupData.add(currentCharaData)
                //read characteristics from server
                readAndNotifyChars(gattCharacteristic)
            }
            mGattCharacteristics!!.add(chars)
            gattCharacteristicData.add(gattCharacteristicGroupData)
        }
    }
    /*
     we check 'Read' and 'Notify' features.
     See http://d.android.com/reference/android/bluetooth/BluetoothGatt.html for the complete
     list of supported characteristic features.
   */
    private fun readAndNotifyChars(characteristic: BluetoothGattCharacteristic) {
        val charaProp = characteristic.properties
        if (charaProp or BluetoothGattCharacteristic.PROPERTY_READ > 0) {
            // If there is an active notification on a characteristic, clear
            // it first so it doesn't update the data field on the user interface.
            if (mNotifyCharacteristic != null) {
                mBleGattClientService?.setCharacteristicNotification(
                    mNotifyCharacteristic!!, false
                )
                mNotifyCharacteristic = null
            }
            mBleGattClientService?.readCharacteristic(characteristic)
        }
        if (charaProp or BluetoothGattCharacteristic.PROPERTY_NOTIFY > 0) {
            mNotifyCharacteristic = characteristic
            mBleGattClientService?.setCharacteristicNotification(
                characteristic, true
            )
        }
    }

    companion object {
        private val TAG = BleResultDetailActivity::class.java.simpleName
        const val EXTRAS_DEVICE_NAME = "DEVICE_NAME"
        const val EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS"
        private fun makeGattUpdateIntentFilter(): IntentFilter {
            val intentFilter = IntentFilter()
            intentFilter.addAction(BleGattClientService.ACTION_GATT_CONNECTED)
            intentFilter.addAction(BleGattClientService.ACTION_GATT_DISCONNECTED)
            intentFilter.addAction(BleGattClientService.ACTION_GATT_SERVICES_DISCOVERED)
            intentFilter.addAction(BleGattClientService.ACTION_DATA_AVAILABLE)
            return intentFilter
        }
    }
}