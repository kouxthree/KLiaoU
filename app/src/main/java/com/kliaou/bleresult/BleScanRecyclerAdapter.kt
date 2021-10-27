package com.kliaou.bleresult

import android.bluetooth.le.ScanResult
import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.kliaou.ADVERTISE_UUID
import com.kliaou.databinding.BleResultItemBinding
import com.kliaou.ui.home.BleResultDetailActivity

class BleScanRecyclerAdapter : RecyclerView.Adapter<BleScanRecyclerAdapter.ResultHolder>() {

    private var itemsList: MutableList<ScanResult> = arrayListOf()

    fun clearItems() {
        itemsList.clear()
        notifyDataSetChanged()
    }

    fun setItems(mutableList: MutableList<ScanResult>) {
        if (mutableList != itemsList) {
            itemsList = mutableList
            notifyDataSetChanged()
        }
    }

    fun addSingleItem(item: ScanResult) {
        itemsList.removeAll {
            it.device.name == item.device.name && it.device.address == item.device.address
        }
        itemsList.add(item)
        notifyDataSetChanged()
    }

    override fun getItemCount() = itemsList.size

    private fun getItem(position: Int): ScanResult? = if (itemsList.isEmpty()) null else itemsList[position]

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ResultHolder {
        val binding = BleResultItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ResultHolder(binding)
    }

    override fun onBindViewHolder(holder: ResultHolder, position: Int) {
        Log.d(TAG, "onBindViewHolder: called for position $position")
        holder.bind(getItem(position))
    }

    inner class ResultHolder(private val binding: BleResultItemBinding) :
        RecyclerView.ViewHolder(binding.root), View.OnClickListener {

        private var _scanResult: ScanResult? = null

        init {
            binding.root.setOnClickListener(this)
        }

        fun bind(item: ScanResult?) {
            item?.let {
                binding.deviceName.text = it.device.name
                binding.deviceAddress.text = it.device.address
                binding.lastSeen.text = it.timestampNanos.toString()
            }
            //this.itemView.setBackgroundColor(Color.parseColor("#ff4d4d"))
            _scanResult = item
        }
        override fun onClick(v: View) {
            val context = v.context
            val showActivityIntent = Intent(context, BleResultDetailActivity::class.java)
            //scanned device name and mac address
            showActivityIntent.putExtra(BleResultDetailActivity.EXTRAS_DEVICE_NAME, _scanResult?.device?.name)
            showActivityIntent.putExtra(BleResultDetailActivity.EXTRAS_DEVICE_ADDRESS, _scanResult?.device?.address)
            showActivityIntent.putExtra(BleResultDetailActivity.EXTRAS_REMOTE_GENDER,
                _scanResult?.scanRecord?.serviceData?.get(ADVERTISE_UUID))
            context.startActivity(showActivityIntent)
        }
    }

    companion object {
        private val TAG = BleScanRecyclerAdapter::class.java.simpleName
    }
}
