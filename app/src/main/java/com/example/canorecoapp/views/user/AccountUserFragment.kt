package com.example.canorecoapp.views.user

import android.app.ProgressDialog
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import cn.pedant.SweetAlert.SweetAlertDialog
import com.bumptech.glide.Glide
import com.example.canorecoapp.R
import com.example.canorecoapp.databinding.FragmentAccountLineMenBinding
import com.example.canorecoapp.databinding.FragmentAccountUserBinding
import com.example.canorecoapp.databinding.FragmentHomeUserBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class AccountUserFragment : Fragment() {
    private lateinit var binding : FragmentAccountLineMenBinding
    private lateinit var auth : FirebaseAuth
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

        showShimmerEffect()
        Handler(Looper.getMainLooper()).postDelayed({
            loadUsersInfo()
        }, 600)

        binding.logout.setOnClickListener {
            val progressDialog = SweetAlertDialog(requireContext(), SweetAlertDialog.PROGRESS_TYPE)
            progressDialog.titleText = "Logging out..."
            progressDialog.show()

            auth.signOut()
            Handler(Looper.getMainLooper()).postDelayed({
                progressDialog.changeAlertType(SweetAlertDialog.SUCCESS_TYPE)
                progressDialog.titleText = "Logged Out!"
                progressDialog.confirmText = "OK"
                progressDialog.setConfirmClickListener {
                    findNavController().navigate(R.id.signInFragment)
                    progressDialog.dismiss()
                }
            }, 1000) // Adjust the delay as needed
        }



    }

    private fun loadUsersInfo() {
        val db = FirebaseFirestore.getInstance()
        val currentUser = FirebaseAuth.getInstance().currentUser

        currentUser?.let { user ->
            db.collection("users").document(user.uid).get()
                .addOnSuccessListener { document ->
                    hideShimmerEffect()
                    val userName = document.getString("fullName")
                    val contact = document.getString("phone")
                    val image = document.getString("image")

                    binding.username.text = userName
                    binding.contactNumber.text = contact
                    val context = context ?: return@addOnSuccessListener
                    Glide.with(context)
                        .load(image) // Load the image URL from Firestore
                        .into(binding.imgUserProfile)

                }
                .addOnFailureListener { exception ->
                    hideShimmerEffect()
                    Toast.makeText(
                        requireContext(),
                        "Error Loading User Data: ${exception.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
        } ?: run {
            hideShimmerEffect()
            Toast.makeText(
                requireContext(),
                "User not authenticated",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun showShimmerEffect() {
        binding.shimmerViewContainer.startShimmerAnimation()
        binding.shimmerViewContainer.visibility = View.VISIBLE

    }

    private fun hideShimmerEffect() {
        binding.shimmerViewContainer.stopShimmerAnimation()

        binding.userInfoCard.visibility = View.VISIBLE
    }


}