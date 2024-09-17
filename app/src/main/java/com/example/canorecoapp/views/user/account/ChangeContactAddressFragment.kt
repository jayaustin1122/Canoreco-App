package com.example.canorecoapp.views.user.account

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.example.canorecoapp.R
import com.example.canorecoapp.databinding.FragmentChangeContactAddressBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore


class ChangeContactAddressFragment : Fragment() {
    private lateinit var binding : FragmentChangeContactAddressBinding
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentChangeContactAddressBinding.inflate(layoutInflater)
        // Inflate the layout for this fragment
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadUsersInfo()
    }

    private fun loadUsersInfo() {
        val db = FirebaseFirestore.getInstance()
        val currentUser = FirebaseAuth.getInstance().currentUser

        currentUser?.let { user ->
            db.collection("users").document(user.uid).get()
                .addOnSuccessListener { document ->

                    val userName = document.getString("firstName")
                    val lastName = document.getString("lastName")
                    val contact = document.getString("phone")
                    val image = document.getString("image")
                    val email = document.getString("email")
                    val municipality = document.getString("municipality")
                    val barangay = document.getString("barangay")
                    val password = document.getString("password")
                    val street = document.getString("street")

                    binding.etContactNumber.setText(contact)
                    binding.tvMunicipality.setText(municipality)
                    binding.tvBrgy.setText(barangay)
                    binding.etStreet.setText(contact)


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

}