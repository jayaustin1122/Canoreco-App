package com.example.canorecoapp.views.user.news

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SearchView
import androidx.activity.OnBackPressedCallback
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
            findNavController().navigate(R.id.userHolderFragment)
        }
        binding.search.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                query?.let {
                    newsAdapter.filter.filter(query)
                }
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                newText?.let {
                    newsAdapter.filter.filter(newText)
                }
                return false
            }
        })
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                findNavController().navigate(R.id.userHolderFragment)
            }
        })
    }

    private fun getAllNews() {
        val collectionRef = db.collection("news")

        collectionRef.addSnapshotListener { documents, exception ->
            if (exception != null) {
                Log.e("NewsFragment", "Error getting documents: ", exception)
                return@addSnapshotListener
            }

            if (documents != null) {
                newsList.clear()
                for (document in documents) {
                    val title = document.getString("title") ?: ""
                    val imageList = document.get("image") as? List<String> ?: emptyList()
                    val firstImage = imageList.getOrNull(0) ?: ""
                    val date = document.getString("date") ?: ""
                    val category = document.getString("category") ?: ""
                    val timestamp = document.getString("timestamp") ?: ""

                    // Create News object and add to the list
                    val news = News(title, "", "", firstImage, timestamp, date, "", "", "", "", category)
                    newsList.add(news)
                }

                Log.d("NewsFragment", "Fetched ${newsList.size} news items")


                lifecycleScope.launchWhenResumed {
                        newsAdapter = NewsDetailsAdapter(this@NewsFragment.requireContext(), findNavController(), newsList)
                        binding.recyclerNews.setHasFixedSize(true)
                        val layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
                        binding.recyclerNews.layoutManager = layoutManager
                        binding.recyclerNews.adapter = newsAdapter

                }
            } else {
                Log.d("NewsFragment", "No documents found")
                binding.tvEmpty.visibility = View.VISIBLE
                binding.imgEmpty.visibility = View.VISIBLE
                binding.recyclerNews.visibility = View.GONE
            }
        }
    }


}