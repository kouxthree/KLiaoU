package com.kliaou.blerecycler

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.kliaou.R
import com.kliaou.service.BleMessage
import java.lang.IllegalArgumentException

class ChatMessageAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private val messages = mutableListOf<BleMessage>()
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        Log.d(TAG, "onCreateViewHolder: ")
        val inflater = LayoutInflater.from(parent.context)
        return when(viewType) {
            REMOTE_MESSAGE -> {
                val view = inflater.inflate(R.layout.item_chat_message_remote, parent, false)
                ChatMessageRemoteViewHolder(view)
            }
            LOCAL_MESSAGE -> {
                val view = inflater.inflate(R.layout.item_chat_message_local, parent, false)
                ChatMessageLocalViewHolder(view)
            }
            else -> {
                throw IllegalArgumentException("Unknown MessageAdapter view type")
            }
        }
    }
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        Log.d(TAG, "onBindViewHolder: ")
        val message = messages[position]
        when(message) {
            is BleMessage.RemoteMessage -> {
                (holder as ChatMessageRemoteViewHolder).bind(message)
            }
            is BleMessage.LocalMessage -> {
                (holder as ChatMessageLocalViewHolder).bind(message)
            }
        }
    }
    override fun getItemCount(): Int {
        Log.d(TAG, "getItemCount: ")
        return messages.size
    }
    override fun getItemViewType(position: Int): Int {
        Log.d(TAG, "getItemViewType: ")
        return when(messages[position]) {
            is BleMessage.RemoteMessage -> REMOTE_MESSAGE
            is BleMessage.LocalMessage -> LOCAL_MESSAGE
        }
    }

    // Add messages to the top of the list so they're easy to see
    fun addMessage(message: BleMessage) {
        Log.d(TAG, "addMessage: ")
        messages.add(0, message)
        notifyDataSetChanged()
    }

    companion object {
        private val TAG = ChatMessageAdapter::class.java.simpleName
        private const val REMOTE_MESSAGE = 0
        private const val LOCAL_MESSAGE = 1
    }
}