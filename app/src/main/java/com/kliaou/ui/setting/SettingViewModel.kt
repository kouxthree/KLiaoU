package com.kliaou.ui.setting

import android.app.Application
import androidx.lifecycle.*
import com.kliaou.datastore.proto.SEX
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking

class SettingViewModelFactory(val application: Application):
    ViewModelProvider.AndroidViewModelFactory(application) {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return SettingViewModel(
            application
        ) as T
    }
}
class SettingViewModel(application: Application) : AndroidViewModel(application) {
    private val context = getApplication<Application>().applicationContext
    companion  object {
        val PREFS_MY_SEX_FN = "mysex.pb"
        val PREFS_REMOTE_SEX_FN = "remotesex.pb"
    }

    private val _mysex = MutableLiveData<SEX>().apply {
        val mySexFlow: Flow<SEX>? =
            context.mySexDataStore?.data?.map { settings ->
                settings.sex
            }
        value = runBlocking {
            mySexFlow?.first()
        }
    }
    val mysex: LiveData<SEX> = _mysex

    private val _remotesex = MutableLiveData<SEX>().apply {
        val remoteSexFlow: Flow<SEX>? =
            context?.remoteSexDataStore?.data?.map { settings ->
                settings.sex
            }
        value = runBlocking {
            remoteSexFlow?.first()
        }
    }
    val remotesex: LiveData<SEX> = _remotesex
}