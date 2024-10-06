package com.example.canorecoapp.views.user.bayadcenterandbusinesscenter

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.canorecoapp.R
import com.example.canorecoapp.adapter.NewsAdapter
import com.example.canorecoapp.databinding.FragmentBayadCentersBinding
import com.example.canorecoapp.databinding.FragmentOutagesBinding
import com.example.canorecoapp.models.Centers
import com.example.canorecoapp.models.News
import com.example.canorecoapp.views.user.bayadcenterandbusinesscenter.BayadFragmentOne
import com.example.canorecoapp.views.user.bayadcenterandbusinesscenter.BusinessCenterFragmentTwo
import com.example.canorecoapp.views.user.news.NewsDetailsFragment
import com.example.canorecoapp.views.user.outages.CurrentOutagesMapFragment
import com.example.canorecoapp.views.user.outages.FutureOutagesMapFragment
import com.google.android.material.tabs.TabLayout
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


class BayadCentersFragment : Fragment() {
    private lateinit var binding : FragmentBayadCentersBinding
    private val firestore = FirebaseFirestore.getInstance()
    private var selectedFragmentId: Int? = null
    private var from: String? = null
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentBayadCentersBinding.inflate(layoutInflater)
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
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        arguments?.let {
            selectedFragmentId = it.getInt("selectedFragmentId", R.id.navigation_services)
            from = it.getString("from")
        }
        binding.backButton.setOnClickListener {
            handleBackNavigation()
        }
        // Handle back button press
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                handleBackNavigation()
            }
        })
        // Set up the tabs
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("Bayad Centers"))
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("Business Centers"))

        // Display the first fragment by default
        if (savedInstanceState == null) {
            replaceFragment(BayadFragmentOne(),from)
        }

        // Handle tab selection
        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                when (tab.position) {
                    0 -> replaceFragment(BayadFragmentOne(),from)
                    1 -> replaceFragment(BusinessCenterFragmentTwo(),from)
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab) {
                // No action needed
            }

            override fun onTabReselected(tab: TabLayout.Tab) {
                // No action needed
            }
        })
        binding.viewListButton.setOnClickListener {
            if (binding.tabLayout.selectedTabPosition == 0) {
                queryBayadCenters()
            } else {
                queryBusinessCenters()
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

    private fun queryBayadCenters() {
        val centers = ArrayList<Centers>()
        firestore.collection("bayad_centers")
            .get()
            .addOnSuccessListener { result ->
                val selectedLocations = mutableListOf<String>()
                for (document in result) {
                    val barangay = document.getString("barangay") ?: ""
                    val municipality = document.getString("municipality") ?: ""
                    val locationName = document.getString("locationName") ?: ""
                    val latitude = document.getDouble("latitude") ?: 0
                    val longitude = document.getDouble("longitude") ?: 0

                    Log.d("FirestoreData", "Document ID: ${document.id}")
                    Log.d("FirestoreData", "Barangay: $barangay")
                    Log.d("FirestoreData", "Municipality: $municipality")
                    Log.d("FirestoreData", "Location Name: $locationName")
                    Log.d("FirestoreData", "Latitude: $latitude")
                    Log.d("FirestoreData", "Longitude: $longitude")
                    centers.add(Centers(
                        "",
                        barangay,
                        latitude.toString(),
                        locationName,
                        longitude.toString(),
                        "",
                        municipality,
                        "",
                        ""

                    ))
                }
                val bundle = Bundle().apply {
                    putParcelableArrayList("data", java.util.ArrayList(centers))
                    putString("from","List Of Bayad Centers")
                    putString("from2","List Of Bayad Centers")
                }
                findNavController().navigate(R.id.listOfCentersFragment, bundle)
                Log.d("FirestoreData", "Bayad Centers Selected Locations: $selectedLocations")
            }
            .addOnFailureListener { exception ->
                Log.w("FirestoreError", "Error getting documents: ", exception)
            }
    }

    private fun queryBusinessCenters() {
        val centers = ArrayList<Centers>()
        firestore.collection("business_centers")
            .get()
            .addOnSuccessListener { result ->

                for (document in result) {

                    val barangay = document.getString("barangay") ?: ""
                    val municipality = document.getString("municipality") ?: ""
                    val locationName = document.getString("locationName") ?: ""
                    val latitude = document.getDouble("latitude") ?: 0
                    val longitude = document.getDouble("longitude") ?: 0
                    val mobile = document.getString("mobile") ?: ""

                    Log.d("FirestoreData", "Document ID: ${document.id}")
                    Log.d("FirestoreData", "Barangay: $barangay")
                    Log.d("FirestoreData", "Municipality: $municipality")
                    Log.d("FirestoreData", "Location Name: $locationName")
                    Log.d("FirestoreData", "Latitude: $latitude")
                    Log.d("FirestoreData", "Longitude: $longitude")
                    centers.add(Centers(
                        "",
                        barangay,
                        latitude.toString(),
                        locationName,
                        longitude.toString(),
                        mobile,
                        municipality,
                        "",
                        ""

                    ))
                }
                val bundle = Bundle().apply {
                    putParcelableArrayList("data", java.util.ArrayList(centers))
                    putString("from","List Of Business Centers")
                    putString("from2","List Of Bayad Centers")
                }
                findNavController().navigate(R.id.listOfCentersFragment, bundle)

            }
            .addOnFailureListener { exception ->
                Log.w("FirestoreError", "Error getting documents: ", exception)
            }
    }




}