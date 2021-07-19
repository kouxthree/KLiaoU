package com.kliaou.ui.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class HomeViewModel : ViewModel() {

    private val _text = MutableLiveData<String>().apply {
        value = "点击右上角搜寻按钮开始找人吧"
    }
    val text: LiveData<String> = _text
}