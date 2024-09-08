package com.example.canorecoapp.views.user.news

import HomeUserFragment
import android.annotation.SuppressLint
import android.os.Bundle
import android.text.Html
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.HorizontalScrollView
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.canorecoapp.R
import com.example.canorecoapp.adapter.NewsAdapter
import com.example.canorecoapp.adapter.NewsImagesAdapter
import com.example.canorecoapp.databinding.FragmentNewsDetailsBinding
import com.example.canorecoapp.models.Images
import com.example.canorecoapp.models.News
import com.google.android.play.integrity.internal.al
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import org.bouncycastle.asn1.x500.style.RFC4519Style.title
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


class NewsDetailsFragment : Fragment() {
    private lateinit var binding: FragmentNewsDetailsBinding
    private var db  = Firebase.firestore

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentNewsDetailsBinding.inflate(layoutInflater)
        // Inflate the layout for this fragment
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val category = arguments?.getString("category")

        Log.d("Firestore", "Querying document with category of: $category")
        getNewsDetailsByTimestamp(category)
        binding.backArrow.setOnClickListener {
            findNavController().navigateUp()
        }
    }
    private fun setupRecyclerView(images: List<String>) {
        val recyclerView = binding.imagesRV
        recyclerView.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        recyclerView.adapter = NewsImagesAdapter(requireContext(),findNavController(),images)
        Log.d("NewsImagesAdapter", "Loading image from URL: $images")
    }
    private fun getNewsDetailsByTimestamp(category: String?) {
        if (category.isNullOrEmpty()) {
            Toast.makeText(requireContext(), "Invalid title!", Toast.LENGTH_SHORT).show()
            Log.d("Firestore", "Invalid title provided: $category")
            return
        }
        Log.d("maintenancedetails", "Querying document with title: '$category'")
        val collectionRef = db.collection("news")
        val query = collectionRef.whereEqualTo("category", category)
        val selectedLocations = mutableSetOf<String>()
        query.get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    for (document in documents) {

                        val docTitle = document.getString("title") ?: ""
                        val images = document.get("image") as? List<String> ?: emptyList()
                        val content = document.getString("content") ?: ""
                        val gawain = document.getString("gawain") ?: ""
                        val date = document.getString("date") ?: ""
                        val startTime = document.getString("startTime") ?: ""
                        val endTime = document.getString("endTime") ?: ""
                        val timestamp = document.getString("timestamp") ?: ""
                        val selectedLocationsList = document.get("selectedLocations") as? List<*>
                        selectedLocationsList?.let {
                            selectedLocations.addAll(it.filterIsInstance<String>())
                        }
                        val formattedDate = parseAndFormatDate(timestamp)
                        val formattedTime = formatTimeRange(startTime, endTime)
                        binding.tvOras.text = Html.fromHtml("<b>ORAS:</b> $formattedTime.")
                        binding.newsTitle.text = docTitle
                        binding.newsDate.text = formattedDate
                        binding.tvGawain.text = Html.fromHtml("<b>GAWAIN:</b> $gawain")
                        binding.tvPetsa.text = Html.fromHtml("<b>PETSA:</b> $date")
                        binding.tvContent.text = content

                        if (category == "Patalastas ng Power Interruption") {
                            binding.viewInMapButton.visibility = View.VISIBLE
                            binding.tvLugar.text = Html.fromHtml("<b>APEKTADONG LUGAR:</b> ${selectedLocations.joinToString(", ")}")

                            binding.viewInMapButton.setOnClickListener {
                                if (selectedLocations.isNotEmpty()) {
                                    val detailsFragment = ViewMapsWithAreasFragment()
                                    val bundle = Bundle()
                                    bundle.putString("Areas", selectedLocations.joinToString(","))
                                    detailsFragment.arguments = bundle
                                    findNavController().navigate(R.id.viewMapsWithAreasFragment, bundle)
                                } else {
                                    Toast.makeText(requireContext(), "No areas to show", Toast.LENGTH_SHORT).show()
                                }
                            }
                        } else {
                            binding.viewInMapButton.visibility = View.GONE
                            binding.tvLugar.visibility = View.GONE
                            binding.tvGawain.visibility = View.GONE
                            binding.tvPetsa.visibility = View.GONE
                            binding.tvOras.visibility = View.GONE
                            binding.ss.visibility = View.GONE
                        }
                        setupRecyclerView(images)
                    }
                } else {
                    Log.d("Firestore", "No document found with the given title")
                    Toast.makeText(requireContext(), "No document found with the given title", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { exception ->
                Log.e("Firestore", "Error retrieving documents", exception)
                Toast.makeText(requireContext(), "Failed to retrieve documents: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }

    @SuppressLint("SimpleDateFormat")
     private fun parseAndFormatDate(timestampString: String): String {
        return try {
            val timestampSeconds = timestampString.toLongOrNull() ?: return ""
            val date = Date(timestampSeconds * 1000)
            val outputFormat = SimpleDateFormat("MMMM d, yyyy        h:mm a", Locale.getDefault())
            outputFormat.format(date)
        } catch (e: NumberFormatException) {
            Log.e("Home", "Number format error: ", e)
            ""
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

}