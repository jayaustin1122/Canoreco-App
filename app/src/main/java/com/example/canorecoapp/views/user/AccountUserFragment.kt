package com.example.canorecoapp.views.user

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.example.canorecoapp.R
import com.example.canorecoapp.databinding.FragmentAccountUserBinding
import com.example.canorecoapp.databinding.FragmentHomeUserBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore


class AccountUserFragment : Fragment() {
    private lateinit var binding: FragmentAccountUserBinding
    private lateinit var auth : FirebaseAuth
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentAccountUserBinding.inflate(layoutInflater)
        // Inflate the layout for this fragment
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        auth = FirebaseAuth.getInstance()
        loadUsersInfo()
        binding.logout.setOnClickListener {
            auth.signOut()
            findNavController().apply {
                navigate(R.id.signInFragment)
            }
        }
    }

    private fun loadUsersInfo() {
        val db = FirebaseFirestore.getInstance()
        val currentUser = FirebaseAuth.getInstance().currentUser

        currentUser?.let { user ->
            db.collection("Users").document(user.uid).get()
                .addOnSuccessListener { document ->
                    val userName = document.getString("fullName")
                    val contact = document.getString("phone")
                    val image = document.getString("image")

                    binding.username.text = userName
                    binding.contactNumber.text = contact
                    // Safely load the image using Glide
                    val context = context ?: return@addOnSuccessListener
                    Glide.with(context)
                        .load(userName)
                        .into(binding.imgUserProfile)


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