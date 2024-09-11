package com.example.canorecoapp.views.user.outages

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
import com.example.canorecoapp.adapter.ListOfOutagesAdapter
import com.example.canorecoapp.databinding.FragmentListOfFutureAndCurrentOutagesBinding
import com.google.firebase.firestore.FirebaseFirestore

class ListOfFutureAndCurrentOutagesFragment : Fragment() {
    private lateinit var binding: FragmentListOfFutureAndCurrentOutagesBinding
    private lateinit var adapter: ListOfOutagesAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentListOfFutureAndCurrentOutagesBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        retrieveAllOutagesData()
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

    private fun retrieveAllOutagesData() {
        val firestoreReference = FirebaseFirestore.getInstance().collection("outages")
        val allSelectedLocations = mutableSetOf<String>()
        firestoreReference.get().addOnSuccessListener { querySnapshot ->
            for (document in querySnapshot.documents) {
                val selectedLocationsList = document.get("selectedLocations") as? List<*>
                selectedLocationsList?.let {
                    val selectedLocations = it.filterIsInstance<String>()
                    allSelectedLocations.addAll(selectedLocations)
                }
            }
            Log.d("FirestoreData", "All Selected Locations: $allSelectedLocations")
            val sortedLocations = allSelectedLocations.toList().sorted()
            lifecycleScope.launchWhenResumed {
                adapter = ListOfOutagesAdapter(
                    this@ListOfFutureAndCurrentOutagesFragment.requireContext(),
                    findNavController(),
                    sortedLocations
                )
                binding.rvListOutages.setHasFixedSize(true)
                val layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
                binding.rvListOutages.layoutManager = layoutManager
                binding.rvListOutages.adapter = adapter
            }
        }.addOnFailureListener { exception ->
            Log.e("MapData", "Error retrieving data from Firestore: ${exception.message}")
        }
    }


}
