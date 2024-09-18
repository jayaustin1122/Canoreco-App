package com.example.canorecoapp.views.linemen.tasks

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SearchView
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.canorecoapp.adapter.DevicesAdapter
import com.example.canorecoapp.adapter.ListOfOutagesAdapter
import com.example.canorecoapp.databinding.FragmentNotificationLineMenBinding
import com.example.canorecoapp.models.Devices
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import easypay.appinvoke.utils.Log
import kotlinx.coroutines.tasks.await


class NotificationLineMenFragment : Fragment() {
    private lateinit var binding : FragmentNotificationLineMenBinding
    private lateinit var database: DatabaseReference
    private lateinit var adapter: DevicesAdapter
    private val deviceArrayLists = mutableListOf<Devices>()
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentNotificationLineMenBinding.inflate(layoutInflater)
        // Inflate the layout for this fragment
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        database = FirebaseDatabase.getInstance().getReference("devices")
        showDevices()
        binding.search.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                query?.let {
                    adapter.filter.filter(query)  // Filter the adapter data
                }
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                newText?.let {
                    adapter.filter.filter(newText)  // Filter while typing
                }
                return false
            }
        })

    }

    private fun showDevices() {
        lifecycleScope.launchWhenResumed {
            try {
                val devicesSnapshot = database.get().await()
                deviceArrayLists.clear() // Clear the list before adding new items
                for (deviceSnapshot in devicesSnapshot.children) {
                    val deviceId = deviceSnapshot.key ?: continue
                    val status = deviceSnapshot.child("status").getValue(String::class.java) ?: "Unknown"
                    val locationName = deviceSnapshot.child("locationName").getValue(String::class.java) ?: "Unknown"
                    val latitude = deviceSnapshot.child("latitude").getValue(Double::class.java)
                    val longitude = deviceSnapshot.child("longitude").getValue(Double::class.java)
                    val startTime = deviceSnapshot.child("startTime").getValue(String::class.java) ?: "Unknown"
                    val date = deviceSnapshot.child("date").getValue(String::class.java) ?: "Unknown"
                    val endTime = deviceSnapshot.child("endTime").getValue(String::class.java) ?: "Unknown"
                    val assigned = deviceSnapshot.child("assigned").getValue(String::class.java) ?: "Unknown"
                    val barangay = deviceSnapshot.child("barangay").getValue(String::class.java) ?: "Unknown"
                    val timestamp = deviceSnapshot.child("timestamp").getValue(Long::class.java) ?: System.currentTimeMillis()
                    val id = deviceSnapshot.child("id").getValue(String::class.java) ?: "Unknown"

                    Log.d("NotificationLineMen", "Device: $deviceId, Location: $locationName, Status: $status")

                    // Check for null latitude or longitude
                    if (latitude != null && longitude != null) {
                        deviceArrayLists.add(Devices(
                            assigned, barangay, date, endTime, id, latitude, locationName, longitude, startTime, status, timestamp
                        ))
                    }
                }

                adapter = DevicesAdapter(
                    requireContext(),
                    findNavController(),
                    childFragmentManager,
                    deviceArrayLists
                )
                binding.rvListOutages.setHasFixedSize(true)
                binding.rvListOutages.layoutManager = LinearLayoutManager(requireContext())
                binding.rvListOutages.adapter = adapter
            } catch (e: Exception) {
                // Handle the error appropriately
                Log.e("NotificationLineMen", "Error fetching devices", e)
                Toast.makeText(requireContext(), "Error fetching devices", Toast.LENGTH_SHORT).show()
            }
        }
    }

}