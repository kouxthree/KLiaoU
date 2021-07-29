package com.kliaou.ui.home

import androidx.lifecycle.*
import com.kliaou.scanresult.RecyclerItem

class HomeViewModel(private val state: SavedStateHandle): ViewModel() {
    private val _text = MutableLiveData<String>().apply {
        value = "点击右上角搜寻按钮开始找人"
    }
    val text: LiveData<String> = _text

//    val lstAddress = MutableLiveData<ArrayList<String>>()
//    fun saveScannedResults(scanResults: ArrayList<RecyclerItem>){
//        val lsts = ArrayList<String>()
//        scanResults.forEach{device -> lsts.add(device.Address)}
//        lstAddress.value = lsts
//    }

    companion object {
        val SCAN_RESULT_ADDRESSES = "SCAN_RESULT_ADDRESSES"
    }
    private val savedStateHandle = state
    val lstAddress: MutableLiveData<ArrayList<String>>
        = state.getLiveData<ArrayList<String>>(SCAN_RESULT_ADDRESSES, ArrayList<String>())
    fun saveLstAddress(scanResults: ArrayList<RecyclerItem>) {
        val lsts = ArrayList<String>()
        scanResults.forEach{device -> lsts.add(device.Address)}
        savedStateHandle.set(SCAN_RESULT_ADDRESSES, lsts)
    }
    fun getLstAddress(): ArrayList<String> {
        return savedStateHandle.get(SCAN_RESULT_ADDRESSES)?: ArrayList<String>()
    }

}