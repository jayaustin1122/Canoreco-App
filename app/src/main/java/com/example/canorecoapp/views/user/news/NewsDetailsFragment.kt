package com.example.canorecoapp.views.user.news

import android.os.Bundle
import android.text.Html
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.canorecoapp.R
import com.example.canorecoapp.adapter.NewsAdapter
import com.example.canorecoapp.databinding.FragmentNewsDetailsBinding
import com.example.canorecoapp.models.News
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase


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
        val timestamp = arguments?.getString("timeStamp")
        getNewsDetailsByTimestamp(timestamp)
        binding.backArrow.setOnClickListener {
            findNavController().navigateUp()
        }

    }

    private fun getNewsDetailsByTimestamp(timestamp: String?) {
        if (timestamp == null) {
            Toast.makeText(requireContext(), "Invalid timestamp!", Toast.LENGTH_SHORT).show()
            return
        }

        // Log the timestamp to ensure it's the correct value
        Log.d("Firestore", "Querying document with timestamp: $timestamp")

        // Query the "News" collection where the "timestamp" field matches the given timestamp
        val collectionRef = db.collection("news")
        val query = collectionRef.whereEqualTo("timestamp", timestamp)

        query.get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    for (document in documents) {
                        // Retrieve all the fields in the document
                        val title = document.getString("Title")
                        val gawain = document.getString("Gawain")
                        val image = document.getString("Image")
                        val lugar = document.getString("Lugar")
                        val date = document.getString("Date")
                        val oras = document.getString("Oras")
                        val petsa = document.getString("Petsa")
                        val shortDescription = document.getString("Short Description")
                        val LongDescription = document.getString("Full Description")

                        binding.newsTitle.text = title
                        binding.newsExcerpt.text = shortDescription
                        binding.newsDate.text = date
                        Glide.with(requireContext())
                            .load(image)
                            .into(binding.newsImage)
                        binding.tvGawain.text = Html.fromHtml("<b>GAWAIN:</b> $gawain", Html.FROM_HTML_MODE_LEGACY)
                        binding.tvPetsa.text = Html.fromHtml("<b>PETSA:</b> $petsa", Html.FROM_HTML_MODE_LEGACY)
                        binding.tvOras.text = Html.fromHtml("<b>ORAS:</b> $oras", Html.FROM_HTML_MODE_LEGACY)
                        binding.tvLugar.text = Html.fromHtml("<b>APEKTADONG LUGAR:</b> $lugar", Html.FROM_HTML_MODE_LEGACY)

                        binding.tvLongDesc.text = LongDescription


                    }
                } else {
                    Toast.makeText(requireContext(), "No document found with the given timestamp", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { exception ->
                Log.e("Firestore", "Error retrieving documents", exception)
                Toast.makeText(requireContext(), "Failed to retrieve documents: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }



}