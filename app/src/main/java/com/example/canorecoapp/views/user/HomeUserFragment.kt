package com.example.canorecoapp.views.user

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
import com.example.canorecoapp.databinding.FragmentHomeUserBinding
import com.example.canorecoapp.models.News
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.FirebaseFirestore


class HomeUserFragment : Fragment() {
    private lateinit var binding : FragmentHomeUserBinding

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
    }
    private fun getNews() {
        // Initialize the news ArrayList
        val freeItems = ArrayList<News>()
        val db = FirebaseFirestore.getInstance()
        val ref = db.collection("News")

        // Fetch data from Firestore
        ref.get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    // Retrieve data from the document and create a News object
                    val title = document.getString("Title")
                    val userAddress = document.getString("Full Description")

                    // Log the title and address
                    Log.d("Home", "Title: $title, Title: $userAddress")

//                    // Optionally, you could add the retrieved data to your list
//                    freeItems.add(News(title, userAddress))  // Assuming you have a News data class
                }
            }
            .addOnFailureListener { exception ->
                // Handle error here
                Log.e("Home", "Error getting documents: ", exception)
            }

    }

}