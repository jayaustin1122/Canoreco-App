package com.example.canorecoapp.views.user.complaints

import android.annotation.SuppressLint
import android.os.Bundle
import android.text.format.DateFormat
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.example.canorecoapp.databinding.FragmentViewCompaintBinding
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.util.*

class ViewCompaintFragment : Fragment() {
    private lateinit var binding: FragmentViewCompaintBinding
    private var db = Firebase.firestore

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentViewCompaintBinding.inflate(layoutInflater)
        // Inflate the layout for this fragment
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val timestamp = arguments?.getString("timestamp")
        Log.d("Here", timestamp.toString())
        if (timestamp != null) {
            retrieveMyComplaint(timestamp)
        }
    }

    @SuppressLint("SetTextI18n")
    private fun retrieveMyComplaint(timestamp: String) {
        val userId = Firebase.auth.currentUser?.uid
        if (userId == null) {
            Log.e("ViewCompaintFragment", "User is not authenticated")
            return
        }

        val collectionRef = db.collection("users")
            .document(userId)
            .collection("my_complaints")

        collectionRef.whereEqualTo("timestamp", timestamp).get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    Log.e("ViewCompaintFragment", "No complaints found for the given timestamp")
                    return@addOnSuccessListener
                }

                for (document in documents) {
                    val title = document.getString("reportTitle") ?: ""
                    val address = document.getString("address") ?: ""
                    val concern = document.getString("concern") ?: ""
                    val concernDescription = document.getString("concernDescription") ?: ""
                    val status = document.getString("status") ?: ""
                    val image = document.getString("image") ?: ""
                    val timestampLong = document.getString("timestamp") ?: ""

                    // Format the timestamp to a readable date and time
                    val formattedDate = DateFormat.format("yyyy-MM-dd HH:mm:ss", Date(timestampLong.toLong() * 1000)).toString()

                    // Set data to UI elements
                    binding.complaintTitle.text = title
                    binding.complaintAddress.text = address
                    binding.complaintConcern.text = concern
                    binding.complaintDescription.text = concernDescription
                    binding.complaintStatus.text = status
                    binding.complaintTimestamp.text = "Reported on: $formattedDate"

                    // Load image using Glide
                    Glide.with(requireContext())
                        .load(image)
                        .into(binding.complaintImage)
                }
            }
            .addOnFailureListener { exception ->
                Log.e("ViewCompaintFragment", "Error getting complaint data", exception)
            }
    }
}
