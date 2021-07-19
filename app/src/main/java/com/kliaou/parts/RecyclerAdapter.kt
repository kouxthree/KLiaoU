package com.kliaou.parts

import android.content.Intent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.kliaou.R
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
        private var view: View = v
        private var viewImgScanResult: ImageView
        private var viewTxtScanResult: TextView
        private var recyclerItem: RecyclerItem? = null

        init {
            v.setOnClickListener(this)
            viewImgScanResult = view.findViewById<View>(R.id.img_scanresult) as ImageView
            viewTxtScanResult = view.findViewById<View>(R.id.txt_scanresult) as TextView
        }

        override fun onClick(v: View) {
            val context = itemView.context
            val showBindActivity = Intent(context, BindActivity::class.java)
            showBindActivity.putExtra(ITEM_TO_BE_BOUND, recyclerItem)
            context.startActivity(showBindActivity)
        }

        companion object {
            private const val ITEM_TO_BE_BOUND = "ITEM_TO_BE_BOUND"
        }

        fun bindItem(recycleItem: RecyclerItem) {
            viewImgScanResult
            viewTxtScanResult.setText(recycleItem.Name + " " + recycleItem.Address)
        }
    }
}