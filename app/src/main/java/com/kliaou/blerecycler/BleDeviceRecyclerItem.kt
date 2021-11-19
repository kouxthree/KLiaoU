package com.kliaou.blerecycler

import com.kliaou.ui.ChatCaller

class BleDeviceRecyclerItem(
    var Caller: Int,//set caller for chat activity to indicate who called
    var AdvertiseUuid: ByteArray?,
    var Name: String?,
    val Address: String,
    val Timestamp: Long
    ) {
    init {
        if(Name == null) Name = ""
    }
}