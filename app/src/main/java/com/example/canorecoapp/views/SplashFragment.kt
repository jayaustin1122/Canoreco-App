package com.example.canorecoapp.views

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.net.ConnectivityManager
import android.os.Bundle
import android.os.Handler
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import com.example.canorecoapp.R
import com.example.canorecoapp.databinding.DialogReviewBinding
import com.example.canorecoapp.databinding.FragmentSplashBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch


class SplashFragment : Fragment() {
    private lateinit var binding: FragmentSplashBinding
    private lateinit var auth: FirebaseAuth
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentSplashBinding.inflate(inflater, container, false)
        auth = FirebaseAuth.getInstance()
        val animZoomIn = AnimationUtils.loadAnimation(requireContext(),
            R.anim.zoom_out)
        Handler().postDelayed({
            if (onBoardingFinished()) {
                binding.splashImage.startAnimation(animZoomIn)
                GlobalScope.launch(Dispatchers.Main) {
                    checkUser(animZoomIn)
                }
            } else {
                binding.splashImage.startAnimation(animZoomIn)
                findNavController().navigate(R.id.onBoardingMainFragment)
            }
        }, 5000)

        // Inflate the layout for this fragment
        return binding.root
    }

    private fun onBoardingFinished(): Boolean {
        val sharedPref = requireActivity().getSharedPreferences("onBoarding", Context.MODE_PRIVATE)
        return sharedPref.getBoolean("Finished", false)
    }

    private fun checkUser(animZoomIn: Animation) {

        GlobalScope.launch(Dispatchers.Main) {
            if (isNetworkAvailable()) {
                val firebaseUser = auth.currentUser
                if (firebaseUser == null) {
                    findNavController().navigate(R.id.signUpFragment)
                } else {
                    try {
                        handleUserInfo(animZoomIn)
                    } catch (e: Exception) {
                        // Handle exceptions, e.g., database errors
                        showToast("Error fetching user information")
                        e.printStackTrace()
                    }
                }
            } else {
                showNoInternetDialog()
            }
        }
    }

    private fun handleUserInfo(animZoomIn: Animation) {
        val userId = FirebaseAuth.getInstance().currentUser!!.uid
        val dbref = FirebaseFirestore.getInstance().collection("users")

        dbref.document(userId).get().addOnSuccessListener { document ->
            if (document != null && document.exists()) {
                val userType = document.getString("userType")
                val access = document.getBoolean("access")
                when (userType) {
                    "linemen" -> {
                        findNavController().apply {
                            popBackStack(R.id.splashFragment, false)
                            navigate(R.id.adminHolderFragment)
                        }

                    }
                    "member" -> {
                        if (access == false) {
                            showReviewDialog()
                            showToast("Your account may have a problem. Please contact the admin for assistance.")
                            auth.signOut()
                            activity?.finish()
                            findNavController().apply {
                                popBackStack(R.id.splashFragment, false)
                                navigate(R.id.signInFragment)
                            }
                        } else if (access == true) {
                            findNavController().navigate(R.id.userHolderFragment) // Navigate to UserHolderFragment

                        }
                    }
                    else -> {
                        showToast("Unknown user type")
                        auth.signOut()
                        findNavController().navigate(R.id.signInFragment)
                    }
                }
            } else {
                showToast("User not found")
                auth.signOut()
                findNavController().navigate(R.id.signInFragment)
            }
        }.addOnFailureListener { e ->
            showToast("Error fetching user information")
            e.printStackTrace()
        }
    }


    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    private fun showReviewDialog() {
        val dialogBinding = DialogReviewBinding.inflate(layoutInflater)
        val dialog = Dialog(requireContext())
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(dialogBinding.root)
        dialog.show()
    }

    @SuppressLint("ServiceCast")
    private fun isNetworkAvailable(): Boolean {
        val connectivityManager =
            requireActivity().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkInfo = connectivityManager.activeNetworkInfo
        return networkInfo != null && networkInfo.isConnected
    }

    private fun showNoInternetDialog() {
        val animZoomIn = AnimationUtils.loadAnimation(requireContext(),
            R.anim.zoom_out)
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("No Internet Connection")
            .setMessage("Please connect to the internet to continue.")
            .setPositiveButton("Exit") { _, _ ->


            }
            .setNegativeButton("Retry") { _, _ ->

                checkUser(animZoomIn)
            }
            .setCancelable(false)
            .show()
    }
}