package com.example.canorecoapp.views.user.news

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.canorecoapp.R
import com.example.canorecoapp.adapter.NewsAdapter
import com.example.canorecoapp.adapter.NewsDetailsAdapter
import com.example.canorecoapp.databinding.FragmentNewsBinding
import com.example.canorecoapp.models.News
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase


class NewsFragment : Fragment() {
    private lateinit var binding : FragmentNewsBinding
    private lateinit var newsList: ArrayList<News>
    private lateinit var newsAdapter: NewsDetailsAdapter
    private var db  = Firebase.firestore
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentNewsBinding.inflate(layoutInflater)
        // Inflate the layout for this fragment
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        newsList = ArrayList()
        newsAdapter = NewsDetailsAdapter(requireContext(), findNavController(), newsList)
        binding.recyclerNews.adapter = newsAdapter
        getAllNews()
        binding.backButton.setOnClickListener {
            findNavController().apply {
                navigateUp()
            }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun getAllNews() {
        val collectionRef = db.collection("news")

        collectionRef.get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    val title = document.getString("Title") ?: ""
                    val image = document.getString("Image") ?: ""
                    val date = document.getString("Date") ?: ""
                    val timestamp = document.getDouble("timestamp") ?: ""

                    // Create News object and add to the list
                    val news = News(title, "","",image, timestamp.toString(),date)
                    newsList.add(news)
                }
                // Notify the adapter that the data has changed
                // Set up the adapter after retrieving data for all users
                lifecycleScope.launchWhenResumed {
                    newsAdapter = NewsDetailsAdapter(this@NewsFragment.requireContext(),findNavController(), newsList)
                    binding.recyclerNews.setHasFixedSize(true)
                    val layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
                    binding.recyclerNews.layoutManager = layoutManager
                    binding.recyclerNews.adapter = newsAdapter
                }
                newsAdapter.notifyDataSetChanged()
            }
            .addOnFailureListener { exception ->
                Log.e("NewsFragment", "Error getting documents: ", exception)
            }
    }
}