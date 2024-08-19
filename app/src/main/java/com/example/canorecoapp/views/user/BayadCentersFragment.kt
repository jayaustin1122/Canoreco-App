package com.example.canorecoapp.views.user

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.example.canorecoapp.R
import com.example.canorecoapp.databinding.FragmentBayadCentersBinding
import com.example.canorecoapp.databinding.FragmentOutagesBinding
import com.example.canorecoapp.views.user.bayadcenterandbusinesscenter.BayadFragmentOne
import com.example.canorecoapp.views.user.bayadcenterandbusinesscenter.BusinessCenterFragmentTwo
import com.example.canorecoapp.views.user.outages.CurrentOutagesMapFragment
import com.example.canorecoapp.views.user.outages.FutureOutagesMapFragment
import com.google.android.material.tabs.TabLayout


class BayadCentersFragment : Fragment() {
    private lateinit var binding : FragmentBayadCentersBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentBayadCentersBinding.inflate(layoutInflater)
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.backButton.setOnClickListener {
            findNavController().navigateUp()
        }
        // Set up the tabs
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("Bayad Centers"))
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("Business Centers"))

        // Display the first fragment by default
        if (savedInstanceState == null) {
            replaceFragment(BayadFragmentOne())
        }

        // Handle tab selection
        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                when (tab.position) {
                    0 -> replaceFragment(BayadFragmentOne())
                    1 -> replaceFragment(BusinessCenterFragmentTwo())
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab) {
                // No action needed
            }

            override fun onTabReselected(tab: TabLayout.Tab) {
                // No action needed
            }
        })
    }

    private fun replaceFragment(fragment: Fragment) {
        childFragmentManager.beginTransaction()
            .replace(R.id.map_fragment_container, fragment)
            .commit()
    }
}