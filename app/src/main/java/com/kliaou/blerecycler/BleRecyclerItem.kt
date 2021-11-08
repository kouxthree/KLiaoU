package com.kliaou.blerecycler

class BleRecyclerItem(var Name: String?, val Address: String, val Timestamp: Long) {
    init {
        if(Name == null) Name = ""
    }
}