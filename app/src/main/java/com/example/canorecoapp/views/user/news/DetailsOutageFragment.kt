package com.example.canorecoapp.views.user.news

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.example.canorecoapp.databinding.FragmentDetailsOutageBinding
import com.example.canorecoapp.models.DeviceInfo
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.FirebaseFirestore
import com.google.type.DateTime
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


class DetailsOutageFragment : BottomSheetDialogFragment() {
    private lateinit var binding : FragmentDetailsOutageBinding
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentDetailsOutageBinding.inflate(layoutInflater)
        // Inflate the layout for this fragment
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val areaCode = arguments?.getString("areaCode")
        val from = arguments?.getString("from")
        queryFirestoreForTimestamp(areaCode!!,from)

        Log.e("from", "from details $from")

    }
    private fun queryFirestoreForTimestamp(barangayName: String, from: String?) {
        val firestore = FirebaseFirestore.getInstance()
        firestore.collection("outages")
            .whereArrayContains("selectedLocations", barangayName)
            .get()
            .addOnSuccessListener { result ->
                if (!result.isEmpty) {

                    for (document in result) {
                        val timestamp = document.getString("title")

                        timestamp?.let {
                            getNews(timestamp,from,barangayName)
                        } ?: run {
                            Toast.makeText(
                                requireContext(),
                                "Timestamp not found for $barangayName",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                } else {
                    showDataInRealTime(barangayName)
                }
            }
            .addOnFailureListener { e ->
                Log.e("Firestore", "Error fetching data: ${e.message}")
                Toast.makeText(requireContext(), "Failed to fetch data", Toast.LENGTH_SHORT).show()
            }
    }
    private fun getNews(title: String?, from: String?, barangayName: String) {
        val db = FirebaseFirestore.getInstance()
        val ref = db.collection("news")

        ref.whereEqualTo("title", title).get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    val timestampString = document.getString("timestamp") ?: ""
                    val status = document.getString("status") ?: ""
                    val startTime = document.getString("startTime") ?: ""
                    val endTime = document.getString("endTime") ?: ""
                    val date = document.getString("date") ?: ""
                    val steps = listOf("Outage Detected", "Outage Under Repair", "Power Restored")
                    binding.stepView.setSteps(steps)
                    val formattedDate = parseAndFormatDatse(timestampString)
                    if (from == "Maintenance"|| from == "future"){
                        Log.e("from", "from is $from")
                        binding.stepView.visibility = View.GONE
                        binding.tvOutageStatus.visibility = View.GONE
                        binding.tvEstimatedTimeResolution.text = "Scheduled Date of Power Interruption:"
                        binding.tvUpdated.text = "Updated As of maintenance: $formattedDate"

                    }

                    else{
                        when (status) {
                            "Outage Detected" -> {
                                binding.stepView.go(0, true)
                            }
                            "Outage Under Repair" -> {
                                binding.stepView.go(1, true)
                            }
                            "Power Restored" -> {
                                binding.stepView.go(2, true)
                            }
                            else -> {
                                binding.stepView.go(0, true)
                            }
                        }
                    }
                    binding.tvUpdated.text = "Updated As of: $formattedDate"
                    Log.e("from", "from is $from")
                    val formattedTime = formatTimeRange(startTime, endTime)
                    binding.tvAddressInfo.text = "This address may be affected by an outage. Estimated working hours: $formattedTime."
                    binding.tvTime.text = date
                }
            }
            .addOnFailureListener { exception ->
                Log.e("Home", "Error getting documents: ", exception)
            }
    }
    private fun formatTimeRange(startTime: String, endTime: String): String {
        return try {
            val inputFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
            val outputFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
            val startDate = inputFormat.parse(startTime)
            val endDate = inputFormat.parse(endTime)
            if (startDate != null && endDate != null) {
                val formattedStart = outputFormat.format(startDate)
                val formattedEnd = outputFormat.format(endDate)
                val differenceInMillis = endDate.time - startDate.time
                val differenceInMinutes = differenceInMillis / (1000 * 60)
                val hours = differenceInMinutes / 60
                val minutes = differenceInMinutes % 60
                "$formattedStart - $formattedEnd ($hours hrs $minutes mins)"
            } else {
                "$startTime - $endTime"
            }
        } catch (e: Exception) {
            "$startTime - $endTime"
        }
    }
    private fun showDataInRealTime(barangayName: String) {
        val db = FirebaseDatabase.getInstance().reference.child("devices")
        db.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val matchingDevices = mutableListOf<DeviceInfo>()

                for (child in snapshot.children) {
                    val id = child.child("id").getValue(String::class.java) ?: ""
                    val timestamp = child.child("timestamp").getValue(Double::class.java) ?: ""
                    val startTime = child.child("startTime").getValue(String::class.java) ?: ""
                    val endTime = child.child("endTime").getValue(String::class.java) ?: ""
                    val status = child.child("status").getValue(String::class.java) ?: ""

                    // Create DeviceInfo object with all fields
                    val device = DeviceInfo(
                        id = id,
                        timestamp = timestamp.toString(),
                        startTime = startTime,
                        endTime = endTime,
                        status = status
                    )
                    if (id == barangayName) {
                        val formattedTime = formatTimeRange(startTime, endTime)
                        val formattedDate = parseAndFormatDate(timestamp.toString())
                        binding.stepView.visibility = View.GONE
                        binding.tvOutageStatus.visibility = View.GONE
                        binding.tvEstimatedTimeResolution.text = "Scheduled Date of Power Interruption:"
                        binding.tvUpdated.text = "Updated As of: $formattedDate"
                        if (endTime.isNullOrEmpty() && startTime.isNullOrEmpty()) {
                            binding.tvAddressInfo.text = "This address may be affected by a sudden power outage. Please wait for the linemen to estimate the working hours. Thank you!"
                            binding.tvTime.text = ""
                        } else {
                            binding.tvAddressInfo.text = "This address may be affected by a sudden power outage. Estimated working hours: $formattedTime."
                        }

                        Log.d("DeviceData", "Device ID: $id, Timestamp: $timestamp, Start Time: $startTime, End Time: $endTime")
                        matchingDevices.add(device)
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("MapData", "Error retrieving data from Realtime Database: ${error.message}")
            }
        })
    }




    @SuppressLint("SimpleDateFormat")
    private fun parseAndFormatDate(timestampString: String): String {
        return try {

            val timestamp = timestampString.toDoubleOrNull()?.toLong() ?: return ""
            val date = Date(timestamp * 1000)
            val outputFormat = SimpleDateFormat("MMMM d, yyyy h:mm a", Locale.getDefault())
            outputFormat.format(date)
        } catch (e: NumberFormatException) {
            Log.e("Home", "Number format error: ", e)
            ""
        }
    }




    @SuppressLint("SimpleDateFormat")
     private fun parseAndFormatDatse(timestampString: String): String {
        return try {
            val timestampSeconds = timestampString.toLongOrNull() ?: return ""
            val date = Date(timestampSeconds * 1000)
            val outputFormat = SimpleDateFormat("MMMM d, yyyy h:mm a", Locale.getDefault())
            outputFormat.format(date)
        } catch (e: NumberFormatException) {
            Log.e("Home", "Number format error: ", e)
            ""
        }
    }
}