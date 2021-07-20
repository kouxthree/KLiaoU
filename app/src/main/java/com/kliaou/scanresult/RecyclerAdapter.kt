package com.kliaou.scanresult

import android.content.Intent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.kliaou.R
import com.kliaou.ui.BindActivity
import inflate


class RecyclerAdapter(private val recyclerItems: ArrayList<RecyclerItem>):
    RecyclerView.Adapter<RecyclerAdapter.ResultHolder>() {

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ResultHolder {
        val inflatedView = parent.inflate(R.layout.scanresult_item, false)
        return ResultHolder(inflatedView)
    }

    override fun onBindViewHolder(holder: ResultHolder, position: Int) {
        val itemRecycler = recyclerItems[position]
        holder.bindItem(itemRecycler)
    }

    override fun getItemCount() = recyclerItems.size

    class ResultHolder(v: View) : RecyclerView.ViewHolder(v), View.OnClickListener {
        private var _view: View = v
        private var _viewImgScanResult: ImageView
        private var _viewTxtScanResult: TextView
        private var _recyclerItem: RecyclerItem? = null

        init {
            v.setOnClickListener(this)
            _viewImgScanResult = _view.findViewById<View>(R.id.img_scanresult) as ImageView
            _viewTxtScanResult = _view.findViewById<View>(R.id.txt_scanresult) as TextView
        }

        override fun onClick(v: View) {
            val context = itemView.context
            val showBindActivity = Intent(context, BindActivity::class.java)
            showBindActivity.putExtra(BIND_ITEM_ADDRESS, _recyclerItem?.Address)
            context.startActivity(showBindActivity)
        }

        fun bindItem(recycleItem: RecyclerItem) {
            _recyclerItem = recycleItem
//            viewImgScanResult
            _viewTxtScanResult.text = recycleItem.Name + " " + recycleItem.Address
        }
    }

    companion object {
        const val BIND_ITEM_ADDRESS = "BIND_ITEM_ADDRESS"
    }
}