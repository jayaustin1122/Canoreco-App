package com.example.canorecoapp.views.user.bayadcenterandbusinesscenter

import android.os.Bundle
import android.util.Log
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
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


class BayadCentersFragment : Fragment() {
    private lateinit var binding : FragmentBayadCentersBinding
    private val firestore = FirebaseFirestore.getInstance()
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
            findNavController().navigate(R.id.userHolderFragment)
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
        binding.viewListButton.setOnClickListener {
            if (binding.tabLayout.selectedTabPosition == 0) {
                queryBayadCenters()
            } else {
                queryBusinessCenters()
            }
        }
    }

    private fun replaceFragment(fragment: Fragment) {
        childFragmentManager.beginTransaction()
            .replace(R.id.map_fragment_container, fragment)
            .commit()
    }

    private fun queryBayadCenters() {
        firestore.collection("bayad_centers")
            .get()
            .addOnSuccessListener { result ->
                val selectedLocations = mutableListOf<String>()
                for (document in result) {
                    val barangay = document.getString("barangay")
                    val municipality = document.getString("municipality")
                    val locationName = document.getString("locationName")
                    val latitude = document.getDouble("latitude")
                    val longitude = document.getDouble("longitude")

                    Log.d("FirestoreData", "Document ID: ${document.id}")
                    Log.d("FirestoreData", "Barangay: $barangay")
                    Log.d("FirestoreData", "Municipality: $municipality")
                    Log.d("FirestoreData", "Location Name: $locationName")
                    Log.d("FirestoreData", "Latitude: $latitude")
                    Log.d("FirestoreData", "Longitude: $longitude")
                }
                Log.d("FirestoreData", "Bayad Centers Selected Locations: $selectedLocations")
            }
            .addOnFailureListener { exception ->
                Log.w("FirestoreError", "Error getting documents: ", exception)
            }
    }

    private fun queryBusinessCenters() {
        firestore.collection("business_centers")
            .get()
            .addOnSuccessListener { result ->

                for (document in result) {

                    val barangay = document.getString("barangay")
                    val municipality = document.getString("municipality")
                    val locationName = document.getString("locationName")
                    val latitude = document.getDouble("latitude")
                    val longitude = document.getDouble("longitude")

                    Log.d("FirestoreData", "Document ID: ${document.id}")
                    Log.d("FirestoreData", "Barangay: $barangay")
                    Log.d("FirestoreData", "Municipality: $municipality")
                    Log.d("FirestoreData", "Location Name: $locationName")
                    Log.d("FirestoreData", "Latitude: $latitude")
                    Log.d("FirestoreData", "Longitude: $longitude")

                }

            }
            .addOnFailureListener { exception ->
                Log.w("FirestoreError", "Error getting documents: ", exception)
            }
    }

    private fun parseDate(dateString: String): Date? {
        return try {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
            dateFormat.parse(dateString)
        } catch (e: Exception) {
            null
        }
    }


}