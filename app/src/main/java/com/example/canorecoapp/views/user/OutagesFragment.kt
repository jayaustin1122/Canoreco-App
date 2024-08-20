package com.example.canorecoapp.views.user

import android.Manifest
import android.content.pm.PackageManager
import android.location.Geocoder
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.navigation.fragment.findNavController
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
import com.google.firebase.firestore.FirebaseFirestore
import java.io.IOException
import java.util.Locale

class OutagesFragment : Fragment() {
    private lateinit var binding: FragmentOutagesBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentOutagesBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.backButton.setOnClickListener {
            findNavController().navigateUp()
        }
        binding.viewListButton.setOnClickListener {
            val coordinates = listOf(
                mapOf("lat" to 14.08446, "lng" to 122.88797),
                mapOf("lat" to 14.08500, "lng" to 122.88800),
                mapOf("lat" to 14.08400, "lng" to 122.88900)
            )

            addBarangayBoundary("Daet", coordinates, true)

        }
        // Set up the tabs
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("Current Outages"))
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("Future Outages"))

        // Display the first fragment by default
        if (savedInstanceState == null) {
            replaceFragment(CurrentOutagesMapFragment())
        }

        // Handle tab selection
        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                when (tab.position) {
                    0 -> replaceFragment(CurrentOutagesMapFragment())
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
    fun addBarangayBoundary(name: String, coordinates: List<Map<String, Double>>, isAffected: Boolean) {
        val firestore = FirebaseFirestore.getInstance()
        val barangayData = hashMapOf(
            "name" to name,
            "coordinates" to coordinates,
            "isAffected" to isAffected
        )

        firestore.collection("barangay_boundaries")
            .add(barangayData)
            .addOnSuccessListener {
                Log.d("Firestore", "DocumentSnapshot successfully written!")
            }
            .addOnFailureListener { e ->
                Log.w("Firestore", "Error writing document", e)
            }
    }
}
