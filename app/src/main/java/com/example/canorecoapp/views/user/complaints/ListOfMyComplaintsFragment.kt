package com.example.canorecoapp.views.user.complaints

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SearchView
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.canorecoapp.R
import com.example.canorecoapp.adapter.ListOfComplaintsAdapter
import com.example.canorecoapp.adapter.ListOfOutagesAdapter
import com.example.canorecoapp.adapter.NewsDetailsAdapter
import com.example.canorecoapp.databinding.FragmentListOfFutureAndCurrentOutagesBinding
import com.example.canorecoapp.databinding.FragmentListOfMyComplaintsBinding
import com.example.canorecoapp.models.News
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class ListOfMyComplaintsFragment : Fragment() {
    private lateinit var binding: FragmentListOfMyComplaintsBinding
    private lateinit var adapter: ListOfComplaintsAdapter
    private var db  = Firebase.firestore
    private lateinit var newsList: ArrayList<News>
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentListOfMyComplaintsBinding.inflate(layoutInflater)
        // Inflate the layout for this fragment
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        newsList = ArrayList()
        adapter = ListOfComplaintsAdapter(requireContext(), findNavController(), newsList)
        binding.rvListOutages.adapter = adapter
        retrieveAllComplaints()
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
        binding.backButton.setOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun retrieveAllComplaints() {
        val userId = Firebase.auth.currentUser?.uid
        if (userId == null) {
            Log.e("NewsFragment", "User is not authenticated")
            return
        }

        val collectionRef = db.collection("users")
            .document(userId)
            .collection("my_complaints")

        collectionRef.addSnapshotListener { documents, exception ->
            if (exception != null) {
                Log.e("NewsFragment", "Error getting documents: ", exception)
                return@addSnapshotListener
            }

            if (documents != null && !documents.isEmpty) {
                newsList.clear()
                for (document in documents) {
                    val title = document.getString("reportTitle") ?: ""
                    val timestamp = document.getDouble("timestamp") ?: ""
                    val status = document.getString("status") ?: ""

                    val complaints = News(title, "", "", "", timestamp.toString(), "", "", "", "", "", status)
                    newsList.add(complaints)
                }

                lifecycleScope.launchWhenResumed {
                    adapter = ListOfComplaintsAdapter(
                        this@ListOfMyComplaintsFragment.requireContext(),
                        findNavController(),
                        newsList
                    )
                    binding.rvListOutages.setHasFixedSize(true)
                    val layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
                    binding.rvListOutages.layoutManager = layoutManager
                    binding.rvListOutages.adapter = adapter
                }
            } else {
                Log.d("NewsFragment", "No documents found")
            }
        }
    }


}