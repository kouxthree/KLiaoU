package com.kliaou.ui

import android.bluetooth.*
import android.content.*
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.kliaou.ADVERTISE_DATA_FEMALE
import com.kliaou.ADVERTISE_DATA_MALE
import com.kliaou.R
import com.kliaou.REMOTE_INFO_REFRESH_RATE
import com.kliaou.blerecycler.BleConnectRecyclerAdapter
import com.kliaou.blerecycler.ChatMessageAdapter
import com.kliaou.service.BleGattAttributes
import com.kliaou.service.BleGattAttributes.Companion.CHAT_SERVICE
import com.kliaou.service.BleGattClientService
import com.kliaou.service.BleGattServer
import com.kliaou.service.BleMessage
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

class BleResultDetailActivity : AppCompatActivity() {
    private var mConnectionState: TextView? = null
    private var mRemoteGenderView: TextView? = null
    private var mRemoteNicknameView: TextView? = null
    private var mRemoteLocationView: TextView? = null
    private var mTxtConversation: RecyclerView? = null
    private var mChatMessageView: TextView? = null
    private var mSendMsgBtnView: Button? = null
    private var mDeviceName: String? = null
    private var mDeviceAddress: String? = null
    private var mRemoteGenderBytes: ByteArray? = null
    private var mBleGattClientService: BleGattClientService? = null
    private var mGattCharacteristics: ArrayList<ArrayList<BluetoothGattCharacteristic>>? =
        ArrayList()
    private var mConnected = false
    private var mNotifyCharacteristic: BluetoothGattCharacteristic? = null
    private val LIST_NAME = "NAME"
    private val LIST_UUID = "UUID"
    private var charRemoteNickname: BluetoothGattCharacteristic? = null
    private var charRemoteLocation: BluetoothGattCharacteristic? = null
//    lateinit var remoteInfoRefreshHandler: Handler//for remote info refreshing

    // refresh remote info continually
    private val remoteInfoRefreshTask = object : Runnable {
        override fun run() {
            mBleGattClientService?.refresh()
//            if(remoteInfoRefreshHandler != null) remoteInfoRefreshHandler.postDelayed(this, REMOTE_INFO_REFRESH_RATE)
        }
    }

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
//                    remoteInfoRefreshHandler = Handler(Looper.getMainLooper())
//                    if(remoteInfoRefreshHandler != null) remoteInfoRefreshHandler.post(remoteInfoRefreshTask)
                }
                BleGattClientService.ACTION_GATT_DISCONNECTED -> {
                    mConnected = false
                    updateConnectionState(R.string.disconnected)
                    invalidateOptionsMenu()
                    clearUI()
//                    if(remoteInfoRefreshHandler != null) remoteInfoRefreshHandler.removeCallbacks(remoteInfoRefreshTask)
                }
                BleGattClientService.ACTION_GATT_SERVICES_DISCOVERED -> {
                    // Show all the supported services and characteristics on the user interface.
                    displayGattServices(mBleGattClientService?.getSupportedGattServices())
                }
                BleGattClientService.ACTION_GATT_SERVICES_REFRESH -> {
                    // Refresh all the supported services and characteristics on the user interface.
//                    refreshRemoteNicknameCharDisp()
                    refreshRemoteLocationCharDisp()
                }
                BleGattClientService.ACTION_DATA_AVAILABLE -> {
                    //when server notified, this event always occurred.
                    //but when connected, only first char available. -> seems time needed
                    displayGattCharInfo(intent)
                }
            }
        }
    }

    //display gatt characteristic info
    private fun displayGattCharInfo(intent: Intent) {
        when (intent.getStringExtra(BleGattClientService.EXTRA_CHAR_UUID)) {
            BleGattAttributes.NICKNAME_CHAR -> {
                val data: String = intent.getStringExtra(BleGattClientService.EXTRA_DATA) ?: return
                mRemoteNicknameView!!.text = data
            }
            BleGattAttributes.LOCATION_CHAR -> {
                val data: String = intent.getStringExtra(BleGattClientService.EXTRA_DATA) ?: return
                mRemoteLocationView!!.text = data
            }
            BleGattAttributes.CHAT_MESSAGE_CHAR -> {
                val data: String = intent.getStringExtra(BleGattClientService.EXTRA_DATA) ?: return

            }
        }
    }
    //display advertise service info
    private fun displayAdvertiseServiceInfo() {
        if (mRemoteGenderBytes == null) return
        if (mRemoteGenderBytes!!.size > 1) {
            mRemoteGenderView!!.setText(R.string.gender_other1)
            return
        }
        when (mRemoteGenderBytes!![0]) {
            ADVERTISE_DATA_MALE -> mRemoteGenderView!!.setText(R.string.gender_male)
            ADVERTISE_DATA_FEMALE -> mRemoteGenderView!!.setText(R.string.gender_female)
        }
    }

    private fun clearUI() {
//        mRemoteGenderView!!.setText(R.string.no_data)
        mRemoteNicknameView!!.setText(R.string.no_data)
        mRemoteLocationView!!.setText(R.string.no_data)
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.ble_activity_result_detail)

        val intent = intent
        mDeviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME)
        mDeviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS)
        mRemoteGenderBytes = intent.getByteArrayExtra(EXTRAS_REMOTE_GENDER)

        // Sets up UI references.
        (findViewById<View>(R.id.device_address) as TextView).text = mDeviceAddress
        mConnectionState = findViewById<View>(R.id.connection_state) as TextView
        mRemoteGenderView = findViewById<View>(R.id.remote_gender) as TextView
        mRemoteNicknameView = findViewById<View>(R.id.remote_nickname) as TextView
        mRemoteLocationView = findViewById<View>(R.id.remote_location) as TextView
        mTxtConversation = findViewById<View>(R.id.txt_conversation) as RecyclerView
        mChatMessageView = findViewById<View>(R.id.txt_chat_message) as TextView
        mSendMsgBtnView = findViewById<View>(R.id.btn_send_msg) as Button
        supportActionBar?.title = mDeviceName
        displayAdvertiseServiceInfo()
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        val gattServiceIntent = Intent(this, BleGattClientService::class.java)
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE)
        //set characteristics clicked listener
        mRemoteNicknameView!!.setOnClickListener(remoteNicknameClickedListener())
        mRemoteLocationView!!.setOnClickListener(remoteLocationClickedListener())
        //create chat view
        createChatView()
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
            R.id.menu_refresh -> {
                mBleGattClientService?.refresh()
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
//        if(remoteInfoRefreshHandler != null) remoteInfoRefreshHandler.removeCallbacks(remoteInfoRefreshTask)
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
            if (currentServiceData[LIST_NAME] == unknownServiceString) continue
            currentServiceData[LIST_UUID] = uuid
            //check if already added
            if (isAlreadyAdded(gattServiceData, uuid)) continue
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
                if (currentCharaData[LIST_NAME] == unknownCharaString) continue
                currentCharaData[LIST_UUID] = uuid
                //check if already added
                if (isAlreadyAdded(gattCharacteristicGroupData, uuid)) continue
                gattCharacteristicGroupData.add(currentCharaData)
                when (uuid) {
                    BleGattAttributes.NICKNAME_CHAR -> {
                        //remote nickname char
                        charRemoteNickname = gattCharacteristic
                        //read characteristics from server
                        readAndNotifyChars(gattCharacteristic)
                    }
                    BleGattAttributes.LOCATION_CHAR -> {
                        //remote location char
                        charRemoteLocation = gattCharacteristic
                        //read characteristics from server
                        readAndNotifyChars(gattCharacteristic)
                    }
                    BleGattAttributes.CHAT_MESSAGE_CHAR -> {
                        //chat message char
                        BleGattServer.chatMessageChar = gattCharacteristic
                    }
                }
            }
            mGattCharacteristics!!.add(chars)
            gattCharacteristicData.add(gattCharacteristicGroupData)
        }
    }
    //check if already added
    private fun isAlreadyAdded(
        lst: ArrayList<HashMap<String, String?>>?, item: String
    ): Boolean {
        if (lst == null) return false
        lst.forEach {
            if (it[LIST_UUID] != null && it[LIST_UUID].equals(item)) return true
        }
        return false
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
    //remote characteristics clicked listener
    private fun remoteNicknameClickedListener() = View.OnClickListener {
        refreshRemoteNicknameCharDisp()
    }
    private fun remoteLocationClickedListener() = View.OnClickListener {
        refreshRemoteLocationCharDisp()
    }
    //refresh char display
    private fun refreshRemoteNicknameCharDisp() {
        if (charRemoteNickname != null) readAndNotifyChars(charRemoteNickname!!)
    }
    private fun refreshRemoteLocationCharDisp() {
        if (charRemoteLocation != null) readAndNotifyChars(charRemoteLocation!!)
    }

    //chat view
//    private lateinit var bluetoothAdapter: BluetoothAdapter
    private val chatMessageAdapter = ChatMessageAdapter()
    private val chatMessageObserver = Observer<BleMessage> { message ->
        Log.d(TAG, "Have message ${message.text}")
        chatMessageAdapter.addMessage(message)
    }
    private fun createChatView() {
//        //init bluetooth manager
//        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
//        bluetoothAdapter = bluetoothManager.adapter
        //chat message recycler adapter
        Log.d(TAG, "chatWith: set adapter $chatMessageAdapter")
        mTxtConversation?.layoutManager = LinearLayoutManager(this)
        mTxtConversation?.adapter = chatMessageAdapter
        //send message listener
        mSendMsgBtnView!!.setOnClickListener {
            val message = mChatMessageView!!.text.toString()
            // only send message if it is not empty
            if (message.isNotEmpty()) {
                BleGattServer.sendMessage(message)
                // clear message
                mChatMessageView!!.text = ""
            }
        }
        //set message observer
        BleGattServer.messages.observe(this, chatMessageObserver)
    }

    companion object {
        private val TAG = BleResultDetailActivity::class.java.simpleName
        const val EXTRAS_DEVICE_NAME = "DEVICE_NAME"
        const val EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS"
        const val EXTRAS_REMOTE_GENDER = "REMOTE_GENDER"
        private fun makeGattUpdateIntentFilter(): IntentFilter {
            val intentFilter = IntentFilter()
            intentFilter.addAction(BleGattClientService.ACTION_GATT_CONNECTED)
            intentFilter.addAction(BleGattClientService.ACTION_GATT_DISCONNECTED)
            intentFilter.addAction(BleGattClientService.ACTION_GATT_SERVICES_DISCOVERED)
            intentFilter.addAction(BleGattClientService.ACTION_GATT_SERVICES_REFRESH)
            intentFilter.addAction(BleGattClientService.ACTION_DATA_AVAILABLE)
            return intentFilter
        }
    }
}