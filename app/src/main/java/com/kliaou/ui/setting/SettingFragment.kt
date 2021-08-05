package com.kliaou.ui.setting

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.kliaou.databinding.FragmentSettingBinding
import com.kliaou.datastore.proto.SEX
import kotlinx.coroutines.launch

class SettingFragment : Fragment() {

    private lateinit var settingViewModel: SettingViewModel
    private var _binding: FragmentSettingBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        settingViewModel = ViewModelProvider(
            this,SettingViewModelFactory(requireActivity().application)
        ).get(SettingViewModel::class.java)
        _binding = FragmentSettingBinding.inflate(inflater, container, false)
        val root: View = binding.root
        //read from datastore
        settingViewModel.mysex.observe(viewLifecycleOwner, Observer {
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
        settingViewModel.remotesex.observe(viewLifecycleOwner, Observer {
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
        binding.rdbMySexMale.setOnClickListener() {
            lifecycleScope.launch { storeMySex(SEX.MALE) }
        }
        binding.rdbMySexFemale.setOnClickListener() {
            lifecycleScope.launch { storeMySex(SEX.FEMALE) }
        }
        binding.rdbMySexOther.setOnClickListener() {
            lifecycleScope.launch { storeMySex(SEX.OTHER) }
        }
        binding.rdbRemoteSexMale.setOnClickListener() {
            lifecycleScope.launch { storeRemoteSex(SEX.MALE) }
        }
        binding.rdbRemoteSexFemale.setOnClickListener() {
            lifecycleScope.launch { storeRemoteSex(SEX.FEMALE) }
        }
        binding.rdbRemoteSexOther.setOnClickListener() {
            lifecycleScope.launch { storeRemoteSex(SEX.OTHER) }
        }
        return root
    }

    //write to data store
    suspend fun storeMySex(sex: SEX) {
        context?.mySexDataStore?.updateData { currentSettings ->
            currentSettings.toBuilder()
                .setSex(sex)
                .build()
        }
    }
    suspend fun storeRemoteSex(sex: SEX) {
        context?.remoteSexDataStore?.updateData { currentSettings ->
            currentSettings.toBuilder()
                .setSex(sex)
                .build()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}