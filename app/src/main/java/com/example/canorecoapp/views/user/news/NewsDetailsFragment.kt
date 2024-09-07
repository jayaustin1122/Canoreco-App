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
        val title = arguments?.getString("title")

        Log.d("Firestore", "Querying document with timestampss: $title")
        getNewsDetailsByTimestamp(title)
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
    private fun getNewsDetailsByTimestamp(title: String?) {
        if (title.isNullOrEmpty()) {
            Toast.makeText(requireContext(), "Invalid title!", Toast.LENGTH_SHORT).show()
            Log.d("Firestore", "Invalid title provided: $title")
            return
        }
        Log.d("Firestore", "Querying document with title: '$title'")
        val collectionRef = db.collection("news")
        val query = collectionRef.whereEqualTo("title", title)
        val selectedLocations = mutableSetOf<String>()
        query.get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    for (document in documents) {

                        val docTitle = document.getString("title")?: ""
                        val images = document.get("image") as? List<String> ?: emptyList()
                        val category = document.getString("category")?: ""
                        val content = document.getString("content")?: ""
                        val timestamp = document.getString("timestamp")?: ""
                        val selectedLocationsList = document.get("selectedLocations") as? List<*>
                        selectedLocationsList?.let {
                            selectedLocations.addAll(it.filterIsInstance<String>())
                        }
                        val formattedDate = parseAndFormatDate(timestamp)
                        binding.newsExcerpt.text = "Category: $category"
                        binding.newsTitle.text = docTitle
                        binding.newsDate.text = formattedDate
                        binding.content.text = content
                        if (category == "Patalastas ng Power Interruption"){
                            binding.viewInMapButton.visibility = View.VISIBLE
                        }
                        setupRecyclerView(images)
                        binding.viewInMapButton.setOnClickListener {
                            val detailsFragment = ViewMapsWithAreasFragment()
                            val bundle = Bundle()
                            bundle.putString("Areas", selectedLocations.joinToString(","))
                            detailsFragment.arguments = bundle
                            findNavController().navigate(R.id.viewMapsWithAreasFragment, bundle)
                        }
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
    public fun parseAndFormatDate(timestampString: String): String {
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


}