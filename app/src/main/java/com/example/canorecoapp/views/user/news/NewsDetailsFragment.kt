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
import org.bouncycastle.asn1.x500.style.RFC4519Style.title


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

    private fun getNewsDetailsByTimestamp(title: String?) {
        if (title.isNullOrEmpty()) {
            Toast.makeText(requireContext(), "Invalid title!", Toast.LENGTH_SHORT).show()
            Log.d("Firestore", "Invalid title provided: $title")
            return
        }
        Log.d("Firestore", "Querying document with title: '$title'")


        val collectionRef = db.collection("news")
        val query = collectionRef.whereEqualTo("title", title)

        query.get()
            .addOnSuccessListener { documents ->
                Log.d("Firestore", "Documents retrieved: ${documents.size()}")
                if (!documents.isEmpty) {
                    for (document in documents) {
                        val docTitle = document.getString("title")
                        val image = document.getString("images")
                        val category = document.getString("category")
                        val shortDescription = document.getString("Short Description")

                        if (category == "Patalastas ng Power Interruption"){
                            binding.viewInMapButton.visibility = View.VISIBLE
                        }
                        binding.newsTitle.text = docTitle
                        binding.newsExcerpt.text = shortDescription
                        Glide.with(requireContext())
                            .load(image)
                            .into(binding.newsImage)
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



}