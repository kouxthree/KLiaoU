package com.kliaou.parts

import android.view.View
import android.view.ViewGroup
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
        holder.bindPhoto(itemRecycler)
    }

    override fun getItemCount() = recyclerItems.size

    class ResultHolder(v: View) : RecyclerView.ViewHolder(v), View.OnClickListener {
//        private var view: View = v
        private var recyclerItem: RecyclerItem? = null

        init {
            v.setOnClickListener(this)
        }

        override fun onClick(v: View) {
//            val context = itemView.context
//            val showPhotoIntent = Intent(context, PhotoActivity::class.java)
//            showPhotoIntent.putExtra(PHOTO_KEY, recyclerItem?.Img)
//            context.startActivity(showPhotoIntent)
        }
//
//        companion object {
//            private const val PHOTO_KEY = "PHOTO"
//        }

        fun bindPhoto(recycleItem: RecyclerItem) {
            this.recyclerItem = recycleItem
        }
    }
}