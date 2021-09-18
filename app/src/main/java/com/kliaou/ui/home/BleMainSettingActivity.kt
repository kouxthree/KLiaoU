package com.kliaou.ui.home

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.kliaou.MY_NICKNAME_DEFAULT
import com.kliaou.databinding.BleMainSettingBinding
import com.kliaou.datastore.proto.MyChars
import com.kliaou.datastore.proto.SEX
import kotlinx.coroutines.launch

class BleMainSettingActivity : AppCompatActivity() {
    private lateinit var settingViewModel: BleMainSettingViewModel
    private var _binding: BleMainSettingBinding? = null
    private val binding get() = _binding!!
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = BleMainSettingBinding.inflate(layoutInflater)
        setContentView(_binding!!.root)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)//menu action
        settingViewModel = ViewModelProvider(
            this,SettingViewModelFactory(application)
        )[BleMainSettingViewModel::class.java]
        //read from datastore
        settingViewModel.mynickname.observe(this, {
            if(it == null || it.isEmpty()) {
                binding.txtMyNickname.setText(MY_NICKNAME_DEFAULT)
            } else {
                binding.txtMyNickname.setText(it)
            }
        })
        settingViewModel.mysex.observe(this, {
            when(it) {
                SEX.MALE -> {
                    binding.rdbMySexMale.isChecked = true
                }
                SEX.FEMALE -> {
                    binding.rdbMySexFemale.isChecked = true
                }
                else -> {
                    binding.rdbMySexOther.isChecked = true
                }
            }
        })
        settingViewModel.remotesex.observe(this, {
            when(it) {
                SEX.MALE -> {
                    binding.rdbRemoteSexMale.isChecked = true
                }
                SEX.FEMALE -> {
                    binding.rdbRemoteSexFemale.isChecked = true
                }
                else -> {
                    binding.rdbRemoteSexOther.isChecked = true
                }
            }
        })
        //write to datastore
        binding.txtMyNickname.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable) {}
            override fun beforeTextChanged(
                s: CharSequence, start: Int,count: Int, after: Int
            ) {}
            override fun onTextChanged(
                s: CharSequence, start: Int, before: Int, count: Int
            ) {
                lifecycleScope.launch { storeMyNickname(s.toString()) }
            }
        })
        binding.rdbMySexMale.setOnClickListener {
            lifecycleScope.launch { storeMySex(SEX.MALE) }
        }
        binding.rdbMySexFemale.setOnClickListener {
            lifecycleScope.launch { storeMySex(SEX.FEMALE) }
        }
        binding.rdbMySexOther.setOnClickListener {
            lifecycleScope.launch { storeMySex(SEX.OTHER) }
        }
        binding.rdbRemoteSexMale.setOnClickListener {
            lifecycleScope.launch { storeRemoteSex(SEX.MALE) }
        }
        binding.rdbRemoteSexFemale.setOnClickListener {
            lifecycleScope.launch { storeRemoteSex(SEX.FEMALE) }
        }
        binding.rdbRemoteSexOther.setOnClickListener{
            lifecycleScope.launch { storeRemoteSex(SEX.OTHER) }
        }
    }
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        return true
    }

    //write to data store
    private suspend fun storeMyChars(mychars: MyChars) {
        applicationContext.myCharsDataStore.updateData { currentSettings ->
            currentSettings.toBuilder()
                .setMynickname(mychars.mynickname)
                .setMysex(mychars.mysex)
                .setRemotesex(mychars.remotesex)
                .build()
        }
    }
    private suspend fun storeMyNickname(mynickname: String) {
        applicationContext.myNicknameDataStore.updateData { currentSettings ->
            currentSettings.toBuilder()
                .setValue(mynickname)
                .build()
        }
        //update broadcast nickname
        BleHomeMainActivity.broadcastNickname = mynickname
    }
    private suspend fun storeMySex(sex: SEX) {
        applicationContext.mySexDataStore.updateData { currentSettings ->
            currentSettings.toBuilder()
                .setSex(sex)
                .build()
        }
    }
    private suspend fun storeRemoteSex(sex: SEX) {
        applicationContext.remoteSexDataStore.updateData { currentSettings ->
            currentSettings.toBuilder()
                .setSex(sex)
                .build()
        }
    }

}