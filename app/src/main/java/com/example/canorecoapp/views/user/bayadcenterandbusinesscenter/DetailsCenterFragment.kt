package com.example.canorecoapp.views.user.bayadcenterandbusinesscenter

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.example.canorecoapp.databinding.FragmentDetailsCenterBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase


class DetailsCenterFragment : BottomSheetDialogFragment(){
    private lateinit var binding : FragmentDetailsCenterBinding
    private var db  = Firebase.firestore
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentDetailsCenterBinding.inflate(layoutInflater)
        // Inflate the layout for this fragment
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val markerTag = arguments?.getString("marker")
        val id = arguments?.getString("id")
        if(id == "bayadCenters"){
            val path = "bayad_centers"
            getCenterDetails(markerTag,path)
        }
        else if (id == "businessCenters"){
            val path = "business_centers"
            getCenterDetails(markerTag,path)
        }

    }
    private fun getCenterDetails(marker: String?, path: String) {
        if (marker.isNullOrEmpty()) {
            Log.d("Firestore", "Invalid title provided: $marker")
            return
        }
        val collectionRef = db.collection("$path")
        val query = collectionRef.whereEqualTo("locationName", marker)
        query.get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    for (document in documents) {

                        val barangay = document.getString("barangay")?: ""
                        val municipality = document.getString("municipality")?: ""
                        val street = document.getString("street")?: ""
                        val locationName = document.getString("locationName")?: ""
                        val contact = document.getString("mobile")?: ""
                        val additionalContact = document.getString("additionalMobile")?: ""

                        binding.title.text = locationName
                        binding.address.text = "$municipality, $barangay, $street"
                        binding.contact.text = "$contact, $additionalContact"

                    }
                } else {
                    Log.d("Firestore", "No document found with the given marker")
                    Toast.makeText(requireContext(), "No document found with the given title", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { exception ->
                Log.e("Firestore", "Error retrieving documents", exception)
                Toast.makeText(requireContext(), "Failed to retrieve documents: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }
}