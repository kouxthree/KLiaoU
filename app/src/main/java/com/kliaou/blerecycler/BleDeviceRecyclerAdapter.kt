package com.kliaou.blerecycler

import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.kliaou.databinding.BleDeviceItemBinding
import com.kliaou.ui.BleChatActivity
import com.kliaou.ui.BleDeviceDetailActivity

class BleDeviceRecyclerAdapter : RecyclerView.Adapter<BleDeviceRecyclerAdapter.ResultHolder>() {

    private var itemsList: MutableList<BleDeviceRecyclerItem> = arrayListOf()

    fun clearItems() {
        itemsList.clear()
        notifyDataSetChanged()
    }

    fun setItems(mutableList: MutableList<BleDeviceRecyclerItem>) {
        if (mutableList != itemsList) {
            itemsList = mutableList
            notifyDataSetChanged()
        }
    }

    fun addSingleItem(item: BleDeviceRecyclerItem) {
        itemsList.removeAll {
            it.Name == item.Name && it.Address == item.Address
        }
        itemsList.add(item)
        notifyDataSetChanged()
    }

    fun removeSingleItem(item: BleDeviceRecyclerItem) {
        itemsList.removeAll {
            it.Name == item.Name && it.Address == item.Address
        }
        notifyDataSetChanged()
    }

    override fun getItemCount() = itemsList.size

    private fun getItem(position: Int): BleDeviceRecyclerItem? = if (itemsList.isEmpty()) null else itemsList[position]

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ResultHolder {
        val binding = BleDeviceItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ResultHolder(binding)
    }

    override fun onBindViewHolder(holder: ResultHolder, position: Int) {
        Log.d(TAG, "onBindViewHolder: called for position $position")
        holder.bind(getItem(position))
    }

    inner class ResultHolder(private val binding: BleDeviceItemBinding) :
        RecyclerView.ViewHolder(binding.root), View.OnClickListener {

        private var _item: BleDeviceRecyclerItem? = null

        init {
            binding.root.setOnClickListener(this)
        }

        fun bind(item: BleDeviceRecyclerItem?) {
            item?.let {
                binding.deviceName.text = it.Name
                binding.deviceAddress.text = it.Address
                binding.lastSeen.text = it.Timestamp.toString()
            }
            //this.itemView.setBackgroundColor(Color.parseColor("#ff4d4d"))
            _item = item
        }
        override fun onClick(v: View) {
            val context = v.context
            val showActivityIntent = Intent(context, BleDeviceDetailActivity::class.java)
            //scanned device name and mac address
            showActivityIntent.putExtra(BleChatActivity.EXTRAS_DEVICE_NAME, _item?.Name)
            showActivityIntent.putExtra(BleChatActivity.EXTRAS_DEVICE_ADDRESS, _item?.Address)
            showActivityIntent.putExtra(BleChatActivity.EXTRAS_REMOTE_GENDER, _item?.AdvertiseUuid)
            showActivityIntent.putExtra(BleChatActivity.EXTRAS_CHAT_CALLER, _item?.Caller)
            context.startActivity(showActivityIntent)
        }
    }

    companion object {
        private val TAG = BleDeviceRecyclerAdapter::class.java.simpleName
    }
}
