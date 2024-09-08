package com.example.canorecoapp.views.user.news

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.example.canorecoapp.R
import com.example.canorecoapp.databinding.FragmentDetailsOutageBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.firebase.firestore.FirebaseFirestore
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
        queryFirestoreForTimestamp(areaCode!!)


    }
    private fun queryFirestoreForTimestamp(barangayName: String) {
        val firestore = FirebaseFirestore.getInstance()
        firestore.collection("outages")
            .whereArrayContains("selectedLocations", barangayName)
            .get()
            .addOnSuccessListener { result ->
                if (!result.isEmpty) {

                    for (document in result) {
                        val timestamp = document.getString("title")

                        timestamp?.let {
                            getNews(timestamp)
                        } ?: run {
                            Toast.makeText(
                                requireContext(),
                                "Timestamp not found for $barangayName",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                } else {
                    // No matching documents found
                    Toast.makeText(
                        requireContext(),
                        "No data found for $barangayName",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            .addOnFailureListener { e ->
                Log.e("Firestore", "Error fetching data: ${e.message}")
                Toast.makeText(requireContext(), "Failed to fetch data", Toast.LENGTH_SHORT).show()
            }
    }
    private fun getNews(title: String?) {
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
                    binding.tvUpdated.text = "Updated As of: $formattedDate"
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