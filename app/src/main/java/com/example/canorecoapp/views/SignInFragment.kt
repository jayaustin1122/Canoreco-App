package com.example.canorecoapp.views

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.os.Handler
import android.text.InputType
import android.util.Log
import android.util.Patterns
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.annotation.OptIn
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.viewpager2.widget.ViewPager2
import cn.pedant.SweetAlert.SweetAlertDialog
import com.example.canorecoapp.R
import com.example.canorecoapp.adapter.SignInViewPagerAdapter
import com.example.canorecoapp.databinding.DialogReviewBinding
import com.example.canorecoapp.databinding.FragmentSignInBinding
import com.example.canorecoapp.utils.DialogUtils
import com.google.android.material.badge.BadgeDrawable
import com.google.android.material.badge.BadgeUtils
import com.google.android.material.badge.ExperimentalBadgeUtils
import com.google.android.material.tabs.TabLayoutMediator
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext


class SignInFragment : Fragment() {
    private lateinit var binding: FragmentSignInBinding
    private lateinit var auth: FirebaseAuth
    private var backPressTime = 0L
    private var doubleBackToExitPressedOnce = false
    private val handler = Handler()
    private lateinit var fireStore: FirebaseFirestore
    private lateinit var imageSliderAdapter: SignInViewPagerAdapter
    private lateinit var loadingDialog: SweetAlertDialog
    private var currentPage = 0
    private val imageList = listOf(
        R.drawable.background_login,
        R.drawable.img_onboarding_three,
        R.drawable.img_onboarding_one,
        R.drawable.img_onboarding_three
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentSignInBinding.inflate(layoutInflater)
        return binding.root
    }

    private fun setupImageSlider() {
        imageSliderAdapter = SignInViewPagerAdapter(imageList)
        binding.viewpager?.adapter = imageSliderAdapter
        binding.viewpager?.orientation = ViewPager2.ORIENTATION_HORIZONTAL
        binding.tabLayout?.let { tabLayout ->
            binding.viewpager?.let { viewPager ->
                TabLayoutMediator(tabLayout, viewPager) { tab, position -> }.attach()
            }
        }

        startAutoSlide()
    }
    private fun startAutoSlide() {
        lifecycleScope.launch {
            while (isAdded) {
                delay(3000)
                if (currentPage == imageList.size) {
                    currentPage = 0
                }
                binding.viewpager?.setCurrentItem(currentPage++, true)
            }
        }
    }


    @OptIn(ExperimentalBadgeUtils::class)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        auth = FirebaseAuth.getInstance()
        fireStore = FirebaseFirestore.getInstance()
        binding.etPass.inputType =
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        binding.etUsernameLogin.inputType =
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS

        setupImageSlider()




        handler.postDelayed({ doubleBackToExitPressedOnce = false }, 2000)
        binding.buttonLoginLogin.setOnClickListener {
            loadingDialog = DialogUtils.showLoading(requireActivity())
            loadingDialog.show()
            checkUser()
        }

        binding.tvForgotPasswordLogin.setOnClickListener {
            findNavController().apply {
                navigate(R.id.forgotPasswordFragment)
            }
        }
        binding.register.setOnClickListener {
            findNavController().apply {
                navigate(R.id.dataPrivacyFragment)
            }
        }
    }

    var email = ""
    var pass = ""

    override fun onDestroyView() {
        super.onDestroyView()
        handler.removeCallbacksAndMessages(null)
    }

    override fun onResume() {
        super.onResume()
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, callback)
    }

    override fun onPause() {
        callback.remove()
        super.onPause()
    }

    private val callback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            if (doubleBackToExitPressedOnce) {
                requireActivity().finish()
            } else {
                doubleBackToExitPressedOnce = true
                Toast.makeText(requireContext(), "Press back again to exit", Toast.LENGTH_SHORT)
                    .show()
                handler.postDelayed({ doubleBackToExitPressedOnce = false }, 2000)
            }
        }
    }



    private fun checkUser() {
        val email = binding.etUsernameLogin.text?.trim().toString()
        val password = binding.etPass.text?.trim().toString()

        // Check if email and password are not empty
        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(requireContext(), "Email or password cannot be empty.", Toast.LENGTH_SHORT).show()
            return
        }

        loadingDialog = DialogUtils.showLoading(requireActivity())
        loadingDialog.show()
        // Fetch user data from Firestore based on the email
        val dbref = FirebaseFirestore.getInstance().collection("users")
        dbref.whereEqualTo("email", email).get().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val snapshot = task.result
                if (snapshot != null && !snapshot.isEmpty) {
                    // Assume the first document matches the user (in case multiple documents are returned)
                    val userDoc = snapshot.documents.first()
                    val storedPassword = userDoc.getString("password")
                    val userType = userDoc.getString("userType")
                    val access = userDoc.getBoolean("access")
                    val firstName = userDoc.getString("firstName")

                    // Check if the entered password matches the stored password
                    if (storedPassword == password) {
                        // Password matches, proceed based on user type
                        if (userType != null && firstName != null) {
                            if (userType == "linemen") {
                                if (access == false) {
                                    // Show a dialog for denied access
                                    val dialogBinding = DialogReviewBinding.inflate(layoutInflater)
                                    val dialog = Dialog(requireContext())
                                    dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
                                    dialog.setContentView(dialogBinding.root)
                                    dialog.show()

                                    // Handle sign out
                                    auth.signOut()
                                } else {
                                    // Successful login for linemen
                                    DialogUtils.showSuccessMessage(requireActivity(), "Log In Successful", "Welcome $firstName").show()
                                    findNavController().apply {
                                        popBackStack(R.id.splashFragment, false)
                                        navigate(R.id.adminHolderFragment)
                                    }
                                }
                            } else if (userType == "member") {
                                // Successful login for member
                                DialogUtils.showSuccessMessage(requireActivity(), "Log In Successful", "Welcome $firstName").show()
                                findNavController().apply {
                                    popBackStack(R.id.splashFragment, false)
                                    navigate(R.id.userHolderFragment)
                                }
                            }
                        } else {
                            Toast.makeText(requireContext(), "Error: User type or name missing.", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        // Password doesn't match
                        Toast.makeText(requireContext(), "Incorrect password.", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    // User not found
                    Toast.makeText(requireContext(), "User not found.", Toast.LENGTH_SHORT).show()
                }
            } else {
                loadingDialog.dismiss()
                Toast.makeText(requireContext(), "An error occurred while checking credentials.", Toast.LENGTH_SHORT).show()
            }
        }
    }







}