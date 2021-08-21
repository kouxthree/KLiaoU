package com.kliaou.ui.home

import android.R
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import com.kliaou.blescanresult.BleRecyclerAdapter
import com.kliaou.databinding.BleActivityScanresultDetailBinding

class BleScanresultDetailActivity : AppCompatActivity() {
    private lateinit var _binding: BleActivityScanresultDetailBinding
    private lateinit var _mac: String
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //init//intent was null before onCreate
        _binding = BleActivityScanresultDetailBinding.inflate(layoutInflater)
        //setContentView(R.layout.activity_bind)
        setContentView(_binding.root)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)//menu action
        //remote mac address
        _mac = intent.getStringExtra(BleRecyclerAdapter.BLE_REMOTE_MAC).toString()
        _binding.txtMac.text = _mac.toString()
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
}
