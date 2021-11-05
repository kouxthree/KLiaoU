package com.kliaou.blerecycler

import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.kliaou.R
import com.kliaou.service.BleMessage

class ChatMessageRemoteViewHolder(view: View) : RecyclerView.ViewHolder(view) {

    private val messageText = itemView.findViewById<TextView>(R.id.txt_chat_message)

    fun bind(message: BleMessage.RemoteMessage) {
        messageText.text = message.text
    }
}