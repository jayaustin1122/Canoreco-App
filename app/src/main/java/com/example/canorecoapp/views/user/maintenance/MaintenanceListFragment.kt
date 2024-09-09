package com.example.canorecoapp.views.user.maintenance

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
import com.example.canorecoapp.adapter.MaintenanceListAdapter
import com.example.canorecoapp.adapter.NewsDetailsAdapter
import com.example.canorecoapp.databinding.FragmentMaintenanceListBinding
import com.example.canorecoapp.models.Maintenance
import com.example.canorecoapp.models.News
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase


class MaintenanceListFragment : Fragment() {

    private lateinit var binding : FragmentMaintenanceListBinding
    private lateinit var newsList: ArrayList<Maintenance>
    private lateinit var newsAdapter: MaintenanceListAdapter
    private var db  = Firebase.firestore
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentMaintenanceListBinding.inflate(layoutInflater)
        // Inflate the layout for this fragment
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        newsList = ArrayList()
        newsAdapter = MaintenanceListAdapter(requireContext(), findNavController(), newsList)
        binding.recyclerNews.adapter = newsAdapter
        getListOfMaintenance()
        binding.backButton.setOnClickListener {
            findNavController().apply {
                navigateUp()
            }
        }
    }

    private fun getListOfMaintenance() {
        val collectionRef = db.collection("news")
        collectionRef.whereEqualTo("category", "Patalastas ng Power Interruption").get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    val title = document.getString("title") ?: ""
                    val imageList = document.get("image") as? List<String> ?: emptyList()
                    val firstImage = imageList.getOrNull(0) ?: ""
                    val date = document.getString("date") ?: ""
                    val timestamp = document.getString("timestamp") ?: ""
                    val content = document.getString("content") ?: ""
                    val category = document.getString("category") ?: ""

                    // Create News object and add to the list
                    val maintenance = Maintenance(title, "","",firstImage,timestamp,date,"","","","",category)
                    newsList.add(maintenance)
                }

                lifecycleScope.launchWhenResumed {
                    newsAdapter = MaintenanceListAdapter(this@MaintenanceListFragment.requireContext(),findNavController(), newsList)
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