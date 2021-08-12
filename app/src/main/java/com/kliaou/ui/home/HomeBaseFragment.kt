package com.kliaou.ui.home

import android.content.Intent
import android.os.*
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.kliaou.databinding.FragmentHomeBaseBinding
import com.kliaou.scanresult.RecyclerAdapter
import java.util.*

class HomeBaseFragment : Fragment() {
    val homeViewModel: HomeViewModel by viewModels()
    private var _binding: FragmentHomeBaseBinding? = null
    private val binding get() = _binding!!
    private lateinit var recyclerAdapter: RecyclerAdapter
    private lateinit var scanResultLinearLayoutManager: LinearLayoutManager
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBaseBinding.inflate(inflater, container, false)
        val root: View = binding.root

        binding.btnStart.setOnClickListener {
            val showHomeMainActivityIntent = Intent(context, HomeMainActivity::class.java)
            context?.startActivity(showHomeMainActivityIntent)
        }

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}