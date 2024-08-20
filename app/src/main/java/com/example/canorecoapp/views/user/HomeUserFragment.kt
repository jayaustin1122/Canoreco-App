package com.example.canorecoapp.views.user

import android.os.Bundle
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
import com.example.canorecoapp.adapter.MaintenanceAdapter
import com.example.canorecoapp.adapter.NewsAdapter
import com.example.canorecoapp.databinding.FragmentHomeUserBinding
import com.example.canorecoapp.models.Maintenance
import com.example.canorecoapp.models.News
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.FirebaseFirestore


class HomeUserFragment : Fragment() {
    private lateinit var binding : FragmentHomeUserBinding
    private lateinit var adapter : NewsAdapter
    private lateinit var adapter2 : MaintenanceAdapter
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentHomeUserBinding.inflate(layoutInflater)
        // Inflate the layout for this fragment
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        getNews()
        getMaintenances()
        loadUsersInfo()
        binding.tvViewAllNews.setOnClickListener {
            findNavController().apply {
                navigate(R.id.newsFragment)
            }
        }
        binding.tvViewAllMaintenance.setOnClickListener {
            findNavController().apply {
                navigate(R.id.maintenanceListFragment)
            }
        }
    }
    private fun loadUsersInfo() {
        val db = FirebaseFirestore.getInstance()
        val currentUser = FirebaseAuth.getInstance().currentUser

        currentUser?.let { user ->
            db.collection("users").document(user.uid).get()
                .addOnSuccessListener { document ->
                    val userName = document.getString("image")

                    // Safely load the image using Glide
                    val context = context ?: return@addOnSuccessListener
                    Glide.with(context)
                        .load(userName)
                        .into(binding.imgProfile)


                }
                .addOnFailureListener { exception ->
                    Toast.makeText(
                        requireContext(),
                        "Error Loading User Data: ${exception.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
        } ?: run {
            Toast.makeText(
                requireContext(),
                "User not authenticated",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
    private fun getNews() {
        // Initialize the news ArrayList
        val freeItems = ArrayList<News>()
        val db = FirebaseFirestore.getInstance()
        val ref = db.collection("news")

        // Fetch data from Firestore
        ref.get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    // Retrieve data from the document and create a News object
                    val title = document.getString("Title") ?: ""
                    val shortDesc = document.getString("Short Description") ?: ""
                    val date = document.getString("Date") ?: ""
                    val timestamp = document.getString("timestamp") ?: ""
                    val image = document.getString("Image") ?: ""
                    Log.d("HOme", ": $title, $shortDesc, $date, $image")
                    freeItems.add(News(title, shortDesc, "", image, timestamp.toString(), date, "", "", "", ""))

                }
                // Set up the adapter after retrieving data for all users
                lifecycleScope.launchWhenResumed {
                    adapter = NewsAdapter(this@HomeUserFragment.requireContext(),findNavController(), freeItems)
                    binding.rvLatestNews.setHasFixedSize(true)
                    val layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
                    binding.rvLatestNews.layoutManager = layoutManager
                    binding.rvLatestNews.adapter = adapter
                }
            }
            .addOnFailureListener { exception ->
                // Handle error here
                Log.e("Home", "Error getting documents: ", exception)
            }


    }

    private fun getMaintenances() {
        // Initialize the news ArrayList
        val freeItems = ArrayList<Maintenance>()
        val db = FirebaseFirestore.getInstance()
        // Materials papalitan ko rin hehe
        val ref = db.collection("news")

        // Fetch data from Firestore
        ref.get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    // Retrieve data from the document and create a News object
                    val title = document.getString("Title") ?: ""
                    val shortDesc = document.getString("Short Description") ?: ""
                    val date = document.getString("Date") ?: ""
                    val image = document.getString("Image") ?: ""
                    Log.d("HOme", ": $title, $shortDesc, $date, $image")
                    freeItems.add(Maintenance(title, shortDesc, "", image, "", date, "", "", "", ""))

                }
                // Set up the adapter after retrieving data for all users
                lifecycleScope.launchWhenResumed {
                    adapter2 = MaintenanceAdapter(this@HomeUserFragment.requireContext(),findNavController(), freeItems)
                    binding.rvMaintenanceActivities.setHasFixedSize(true)
                    val layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
                    binding.rvMaintenanceActivities.layoutManager = layoutManager
                    binding.rvMaintenanceActivities.adapter = adapter2
                }
            }
            .addOnFailureListener { exception ->
                // Handle error here
                Log.e("Home", "Error getting documents: ", exception)
            }


    }

}