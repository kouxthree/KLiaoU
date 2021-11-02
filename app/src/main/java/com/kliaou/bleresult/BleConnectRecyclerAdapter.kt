package com.kliaou.bleresult

import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.kliaou.databinding.BleResultItemBinding
import com.kliaou.ui.BleConnectDetailActivity
import com.kliaou.ui.BleResultDetailActivity

class BleConnectRecyclerAdapter : RecyclerView.Adapter<BleConnectRecyclerAdapter.ResultHolder>() {

    private var itemsList: MutableList<BleRecyclerItem> = arrayListOf()

    fun clearItems() {
        itemsList.clear()
        notifyDataSetChanged()
    }

    fun setItems(mutableList: MutableList<BleRecyclerItem>) {
        if (mutableList != itemsList) {
            itemsList = mutableList
            notifyDataSetChanged()
        }
    }

    fun addSingleItem(item: BleRecyclerItem) {
        itemsList.removeAll {
            it.Name == item.Name && it.Address == item.Address
        }
        itemsList.add(item)
        notifyDataSetChanged()
    }

    fun removeSingleItem(item: BleRecyclerItem) {
        itemsList.removeAll {
            it.Name == item.Name && it.Address == item.Address
        }
        notifyDataSetChanged()
    }

    override fun getItemCount() = itemsList.size

    private fun getItem(position: Int): BleRecyclerItem? = if (itemsList.isEmpty()) null else itemsList[position]

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

        private var _connectResult: BleRecyclerItem? = null

        init {
            binding.root.setOnClickListener(this)
        }

        fun bind(item: BleRecyclerItem?) {
            item?.let {
                binding.deviceName.text = it.Name
                binding.deviceAddress.text = it.Address
                binding.lastSeen.text = it.Timestamp.toString()
            }
            //this.itemView.setBackgroundColor(Color.parseColor("#ff4d4d"))
            _connectResult = item
        }
        override fun onClick(v: View) {
            val context = v.context
            val showActivityIntent = Intent(context, BleConnectDetailActivity::class.java)
            //connected device name and mac address
            showActivityIntent.putExtra(BleResultDetailActivity.EXTRAS_DEVICE_NAME, _connectResult?.Name)
            showActivityIntent.putExtra(BleResultDetailActivity.EXTRAS_DEVICE_ADDRESS, _connectResult?.Address)
            context.startActivity(showActivityIntent)
        }
    }

    companion object {
        private val TAG = BleConnectRecyclerAdapter::class.java.simpleName
    }
}
