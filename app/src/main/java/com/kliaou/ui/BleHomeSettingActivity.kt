package com.kliaou.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.kliaou.databinding.BleHomeSettingBinding
import com.kliaou.db.Gender
import kotlinx.coroutines.launch

class BleHomeSettingActivity : AppCompatActivity() {
    private lateinit var settingViewModel: BleHomeSettingViewModel
    private var _binding: BleHomeSettingBinding? = null
    private val binding get() = _binding!!
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = BleHomeSettingBinding.inflate(layoutInflater)
        setContentView(_binding!!.root)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)//menu action
        settingViewModel = ViewModelProvider(
            this, SettingViewModelFactory(application)
        )[BleHomeSettingViewModel::class.java]
        //read from db
        settingViewModel.mynickname.observe(this, {
            binding.txtMyNickname.setText(it)
        })
        settingViewModel.mygender.observe(this, {
            when (it) {
                Gender.MALE -> {
                    binding.rdbMyGenderMale.isChecked = true
                }
                Gender.FEMALE -> {
                    binding.rdbMyGenderFemale.isChecked = true
                }
                else -> {
                    binding.rdbMyGenderOther.isChecked = true
                }
            }
        })
        settingViewModel.remoteGender.observe(this, {
            when (it) {
                Gender.MALE -> {
                    binding.rdbRemoteGenderMale.isChecked = true
                }
                Gender.FEMALE -> {
                    binding.rdbRemoteGenderFemale.isChecked = true
                }
                else -> {
                    binding.rdbRemoteGenderOther.isChecked = true
                }
            }
        })
        //write to db
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
        binding.rdbMyGenderMale.setOnClickListener {
            lifecycleScope.launch { storeMyGender(Gender.MALE) }
        }
        binding.rdbMyGenderFemale.setOnClickListener {
            lifecycleScope.launch { storeMyGender(Gender.FEMALE) }
        }
        binding.rdbMyGenderOther.setOnClickListener {
            lifecycleScope.launch { storeMyGender(Gender.OTHER) }
        }
        binding.rdbRemoteGenderMale.setOnClickListener {
            lifecycleScope.launch { storeRemoteGender(Gender.MALE) }
        }
        binding.rdbRemoteGenderFemale.setOnClickListener {
            lifecycleScope.launch { storeRemoteGender(Gender.FEMALE) }
        }
        binding.rdbRemoteGenderOther.setOnClickListener{
            lifecycleScope.launch { storeRemoteGender(Gender.OTHER) }
        }
        //init broadcast notify flag
        BleHomeActivity.broadcastNotifyFlag = false
    }
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            val data = Intent()
//            data.putExtra("streetkey", "streetname")
            setResult(Activity.RESULT_OK, data)
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        return true
    }

    //write to db
    private suspend fun storeMyNickname(mynickname: String) {
        settingViewModel.entitySetting?.nickName = mynickname
        settingViewModel.updateCurrent(settingViewModel.entitySetting)
        //update broadcast nickname
        BleHomeActivity.broadcastNickname = mynickname
        BleHomeActivity.broadcastNotifyFlag = true
    }
    private suspend fun storeMyGender(mygender: Int) {
        settingViewModel.entitySetting?.myGender = mygender
        settingViewModel.updateCurrent(settingViewModel.entitySetting)
    }
    private suspend fun storeRemoteGender(remotegender: Int) {
        settingViewModel.entitySetting?.remoteGender = remotegender
        settingViewModel.updateCurrent(settingViewModel.entitySetting)
    }
}