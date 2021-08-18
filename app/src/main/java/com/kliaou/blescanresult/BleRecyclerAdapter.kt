package com.kliaou.blescanresult

import android.bluetooth.le.ScanResult
import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.LayoutRes
import androidx.recyclerview.widget.RecyclerView
import com.kliaou.R
import com.kliaou.databinding.BleScanresultItemBinding
import com.kliaou.scanresult.RecyclerAdapter
import com.kliaou.scanresult.RecyclerItem
import com.kliaou.ui.home.HomeBindActivity

private const val TAG = "BleRecyclerAdapter"

class BleRecyclerAdapter : RecyclerView.Adapter<BleRecyclerAdapter.ResultHolder>() {

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
        val binding = BleScanresultItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ResultHolder(binding)
    }

    override fun onBindViewHolder(holder: ResultHolder, position: Int) {
        Log.d(TAG, "onBindViewHolder: called for position $position")
        holder.bind(getItem(position))
    }

    inner class ResultHolder(private val binding: BleScanresultItemBinding) :
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
            _scanResult = item
        }
        override fun onClick(v: View) {
            val context = v.context
            val showBindActivityIntent = Intent(context, HomeBindActivity::class.java)
            //scanned device mac address
            showBindActivityIntent.putExtra(BleRecyclerAdapter.BIND_ITEM_ADDRESS, _scanResult?.device?.address)
            context.startActivity(showBindActivityIntent)
        }
    }

    companion object {
        const val BIND_ITEM_ADDRESS = "BIND_ITEM_ADDRESS"
    }
}
