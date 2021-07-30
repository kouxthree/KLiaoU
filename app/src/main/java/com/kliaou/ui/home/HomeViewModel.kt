package com.kliaou.ui.home

import androidx.lifecycle.*
import com.kliaou.scanresult.RecyclerItem

class HomeViewModel(private val state: SavedStateHandle): ViewModel() {
    private val _text = MutableLiveData<String>().apply {
        value = "点击右上角搜寻按钮开始找人"
    }
    val text: LiveData<String> = _text
}