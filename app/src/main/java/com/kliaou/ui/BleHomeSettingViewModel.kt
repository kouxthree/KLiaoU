package com.kliaou.ui

import android.annotation.SuppressLint
import android.app.Application
import androidx.lifecycle.*
import com.kliaou.datastore.proto.MyChars
import com.kliaou.datastore.proto.SEX
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking

@Suppress("UNCHECKED_CAST")
class SettingViewModelFactory(private val application: Application):
    ViewModelProvider.AndroidViewModelFactory(application) {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return BleMainSettingViewModel(
            application
        ) as T
    }
}
class BleMainSettingViewModel(application: Application) : AndroidViewModel(application) {
    @SuppressLint("StaticFieldLeak")
    private val context = getApplication<Application>().applicationContext
    companion  object {
        const val PREFS_MY_CHARS_FN = "mychars.pb"
        const val PREFS_MY_NICKNAME_FN = "mynickname.pb"
        const val PREFS_MY_SEX_FN = "mysex.pb"
        const val PREFS_REMOTE_SEX_FN = "remotesex.pb"
    }

    private val _mychars = MutableLiveData<MyChars>().apply {
        val myCharsFlow: Flow<MyChars> =
            context.myCharsDataStore.data.map { settings ->
                settings
            }
        value = runBlocking {
            myCharsFlow.first()
        }
    }
    val mychars: LiveData<MyChars> = _mychars

    private val _mynickname = MutableLiveData<String>().apply {
        val myNicknameFlow: Flow<String> =
            context.myNicknameDataStore.data.map { settings ->
                settings.value
            }
        value = runBlocking {
            myNicknameFlow.first()
        }
    }
    val mynickname: LiveData<String> = _mynickname

    private val _mysex = MutableLiveData<SEX>().apply {
        val mySexFlow: Flow<SEX> =
            context.mySexDataStore.data.map { settings ->
                settings.sex
            }
        value = runBlocking {
            mySexFlow.first()
        }
    }
    val mysex: LiveData<SEX> = _mysex

    private val _remotesex = MutableLiveData<SEX>().apply {
        val remoteSexFlow: Flow<SEX> =
            context.remoteSexDataStore.data.map { settings ->
                settings.sex
            }
        value = runBlocking {
            remoteSexFlow.first()
        }
    }
    val remotesex: LiveData<SEX> = _remotesex
}