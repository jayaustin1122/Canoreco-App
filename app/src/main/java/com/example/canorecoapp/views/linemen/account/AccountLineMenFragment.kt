package com.example.canorecoapp.views.linemen.account

import android.app.ProgressDialog
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.example.canorecoapp.R
import com.example.canorecoapp.databinding.FragmentAccountLineMenBinding
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class AccountLineMenFragment : Fragment() {
    private lateinit var binding: FragmentAccountLineMenBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var progressDialog: ProgressDialog
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentAccountLineMenBinding.inflate(layoutInflater)
        // Inflate the layout for this fragment
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        auth = FirebaseAuth.getInstance()
        progressDialog = ProgressDialog(requireContext()).apply {
            setMessage("Please wait...")
            setCancelable(false)
        }

        loadUsersInfo()
        binding.logout.setOnClickListener {
            showProgressDialog()
            auth.signOut()
            Handler(Looper.getMainLooper()).postDelayed({
                hideProgressDialog()
                findNavController().apply {
                    navigate(R.id.signInFragment)
                }
            }, 3000) // 3 seconds delay
        }
        binding.imgUserProfile.setOnClickListener {
            findNavController().apply {
                navigate(R.id.testNotifFragment)
            }
        }
    }

    private fun loadUsersInfo() {
        val db = FirebaseFirestore.getInstance()
        val currentUser = FirebaseAuth.getInstance().currentUser

        currentUser?.let { user ->
            db.collection("users").document(user.uid).get()
                .addOnSuccessListener { document ->
                    val userName = document.getString("fullName")
                    val contact = document.getString("phone")
                    val image = document.getString("image")

                    binding.username.text = userName
                    binding.contactNumber.text = contact
                    // Safely load the image using Glide
                    val context = context ?: return@addOnSuccessListener
                    Glide.with(context)
                        .load(image) // Load the image URL from Firestore
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

    private fun showProgressDialog() {
        progressDialog.show()
    }

    private fun hideProgressDialog() {
        progressDialog.dismiss()
    }
}