package com.example.canorecoapp.views.user.outages

import android.Manifest
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.location.Geocoder
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.example.canorecoapp.R
import com.example.canorecoapp.adapter.ViewPagerAdapter
import com.example.canorecoapp.databinding.FragmentOutagesBinding
import com.example.canorecoapp.views.user.outages.CurrentOutagesMapFragment
import com.example.canorecoapp.views.user.outages.FutureOutagesMapFragment
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.tabs.TabLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.io.IOException
import java.util.Locale

class OutagesFragment : Fragment() {
    private lateinit var binding: FragmentOutagesBinding
    private var selectedFragmentId: Int? = null
    private var from: String? = null
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentOutagesBinding.inflate(layoutInflater)
        return binding.root
    }
    private fun handleBackNavigation() {
        val bundle = Bundle().apply {
            putInt("selectedFragmentId", null ?: R.id.navigation_Home)
        }
        when (from) {
            "home" -> findNavController().navigate(R.id.userHolderFragment, bundle)
            "service" -> {
                bundle.putInt("selectedFragmentId", null ?: R.id.navigation_services)
                findNavController().navigate(R.id.userHolderFragment, bundle)
            }
        }
    }
    private fun replaceFragment(fragment: Fragment, from: String?) {
        val bundle = Bundle().apply {
            putString("from", from)
        }
        fragment.arguments = bundle

        childFragmentManager.beginTransaction()
            .replace(R.id.map_fragment_container, fragment)
            .commit()
    }
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadUsersInfo()
        arguments?.let {
            from = it.getString("from")
        }


        // Set up the tabs
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("Current Outages"))
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("Future Outages"))


        if (savedInstanceState == null) {
            replaceFragment(CurrentOutagesMapFragment(),from)
        }

        // Handle tab selection
        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                when (tab.position) {
                    0 -> replaceFragment(CurrentOutagesMapFragment(),from)
                    1 -> replaceFragment(FutureOutagesMapFragment(),from)
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


    @RequiresApi(Build.VERSION_CODES.O)
    private fun loadUsersInfo() {
        val db = FirebaseFirestore.getInstance()
        val currentUser = FirebaseAuth.getInstance().currentUser

        currentUser?.let { user ->
            db.collection("users").document(user.uid).get()
                .addOnSuccessListener { document ->
                    val userType = document.getString("userType")

                    when (userType) {
                        "member" -> {
                            binding.backButton.setOnClickListener {
                              handleBackNavigation()
                            }
                            requireActivity().onBackPressedDispatcher.addCallback(
                                viewLifecycleOwner,
                                object : OnBackPressedCallback(true) {
                                    override fun handleOnBackPressed() {
                                       handleBackNavigation()
                                    }
                                })
                        }

                        "linemen" -> {
                            binding.backButton.setOnClickListener {
                                val bundle = Bundle().apply {
                                    findNavController().navigate(R.id.adminHolderFragment)
                                }
                                findNavController().navigate(R.id.adminHolderFragment, bundle)
                            }
                            requireActivity().onBackPressedDispatcher.addCallback(
                                viewLifecycleOwner,
                                object : OnBackPressedCallback(true) {
                                    override fun handleOnBackPressed() {
                                        findNavController().navigate(R.id.adminHolderFragment)
                                    }
                                })
                        }

                        else -> {
                            Toast.makeText(
                                requireContext(),
                                "Unknown user type",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
                .addOnFailureListener { exception ->
                    Toast.makeText(
                        requireContext(),
                        "Error Loading User Data: ${exception.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
        } ?: run {
            Toast.makeText(
                requireContext(),
                "User not authenticated",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

}
