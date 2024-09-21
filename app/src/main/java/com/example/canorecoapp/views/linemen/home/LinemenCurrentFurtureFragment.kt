package com.example.canorecoapp.views.linemen.home

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.example.canorecoapp.R
import com.example.canorecoapp.databinding.FragmentLinemenCurrentFurtureBinding
import com.example.canorecoapp.views.user.outages.CurrentOutagesMapFragment
import com.example.canorecoapp.views.user.outages.FutureOutagesMapFragment
import com.google.android.material.tabs.TabLayout


class LinemenCurrentFurtureFragment : Fragment() {
    private lateinit var binding : FragmentLinemenCurrentFurtureBinding
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        binding = FragmentLinemenCurrentFurtureBinding.inflate(layoutInflater)
        // Inflate the layout for this fragment
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Set up the tabs
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("Current Outages"))
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("Future Outages"))

        if (savedInstanceState == null) {
            replaceFragment(HomeLineMenFragment())
        }

        // Handle tab selection
        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                when (tab.position) {
                    0 -> replaceFragment(HomeLineMenFragment())
                    1 -> replaceFragment(FutureOutagesMapFragment())
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