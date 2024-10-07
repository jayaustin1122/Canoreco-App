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
            validateData()
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

    private fun validateData() {
        email = binding.etUsernameLogin.text.toString().trim()
        pass = binding.etPass.text.toString().trim()

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            //invalid email
            Toast.makeText(
                this.requireContext(),
                "Email Is Empty or Invalid Email Format",
                Toast.LENGTH_SHORT
            ).show()
            loadingDialog.dismiss()

        } else if (pass.isEmpty()) {
            Toast.makeText(
                this.requireContext(),
                "Empty Fields are not allowed",
                Toast.LENGTH_SHORT
            ).show()
            loadingDialog.dismiss()
        } else {
            loginUser()
        }
    }

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

    private fun loginUser() {
        val email = binding.etUsernameLogin.text.toString()
        val password = binding.etPass.text.toString()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                auth.signInWithEmailAndPassword(email, password).await()
                withContext(Dispatchers.Main) {
                    checkUser()
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {

                    Toast.makeText(
                        this@SignInFragment.requireContext(),
                        "${e.message} Check Email or Password",
                        Toast.LENGTH_SHORT
                    ).show()
                    loadingDialog.dismiss()
                }


            }
        }
    }

    private fun checkUser() {
        val firebaseUser = auth.currentUser

        // Dismiss any loading dialogs
        loadingDialog.dismiss()

        if (firebaseUser != null) {
            val dbref = FirebaseFirestore.getInstance().collection("users").document(firebaseUser.uid)
            dbref.get().addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val snapshot = task.result
                    if (snapshot != null && snapshot.exists()) {
                        val userType = snapshot.getString("userType")
                        val access = snapshot.getBoolean("access")
                        val name = snapshot.getString("firstName")

                        // Check email verification status before proceeding
                        if (firebaseUser.isEmailVerified) {
                            when (userType) {
                                "linemen" -> {
                                    loadingDialog.dismiss()
                                    if (access == false) {
                                        // Show a dialog for denied access
                                        val dialogBinding = DialogReviewBinding.inflate(layoutInflater)
                                        val dialog = Dialog(this@SignInFragment.requireContext())
                                        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
                                        dialog.setContentView(dialogBinding.root)
                                        dialog.show()

                                        // Sign out the user
                                        auth.signOut()
                                    } else {
                                        // Successful login
                                        DialogUtils.showSuccessMessage(
                                            requireActivity(),
                                            "Log In Successful",
                                            "Welcome $name"
                                        ).show()

                                        findNavController().apply {
                                            popBackStack(R.id.splashFragment, false)
                                            navigate(R.id.adminHolderFragment)
                                        }
                                    }
                                }
                                "member" -> {
                                    loadingDialog.dismiss()
                                    // Navigate if verified
                                    findNavController().apply {
                                        DialogUtils.showSuccessMessage(
                                            requireActivity(),
                                            "Log In Successful",
                                            "Welcome $name"
                                        ).show()
                                        popBackStack(R.id.splashFragment, false)
                                        navigate(R.id.userHolderFragment)
                                    }
                                }
                                else -> {
                                    // Handle unknown user type
                                }
                            }
                        } else {
                            // Email is not verified, sign out the user
                            verifyEmail(firebaseUser)
                        }
                    } else {
                        Toast.makeText(
                            this@SignInFragment.requireContext(),
                            "User not found.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    Toast.makeText(
                        this@SignInFragment.requireContext(),
                        "An error occurred. Please try again later.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        } else {
            Toast.makeText(
                this@SignInFragment.requireContext(),
                "User not authenticated.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
    override fun onStart() {
        super.onStart()

        val firebaseUser = auth.currentUser
        if (firebaseUser != null && !firebaseUser.isEmailVerified) {
            auth.signOut()
            Toast.makeText(requireContext(), "Please verify your email to continue", Toast.LENGTH_LONG).show()
            findNavController().navigate(R.id.signInFragment)
        }
    }


    @SuppressLint("MissingInflatedId")
    private fun showVerificationDialog(user: FirebaseUser) {
        loadingDialog.dismiss()
        DialogUtils.showLoading(requireActivity()).dismiss()
        val dialogBuilder = AlertDialog.Builder(requireContext())
        val inflater = layoutInflater
        val dialogView = inflater.inflate(R.layout.dialog_verification, null)
        dialogBuilder.setView(dialogView)
        val btnContinue = dialogView.findViewById<Button>(R.id.btnContinue)
        val btnResend = dialogView.findViewById<TextView>(R.id.btnResend)

        val dialog = dialogBuilder.create()
        dialog.setCancelable(false)
        dialog.show()

        btnContinue.isEnabled = false
        btnResend.setOnClickListener {
            verifyEmail(user)
            auth.signOut()
        }

        lifecycleScope.launch {
            while (auth.currentUser?.isEmailVerified == false) {
                try {
                    auth.currentUser?.reload()?.addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            if (auth.currentUser?.isEmailVerified == true) {
                                btnContinue.isEnabled = true
                                auth.signOut()
                            }
                        } else {
                            Log.e("VerificationDialog", "Failed to reload user", task.exception)
                        }
                    }
                } catch (e: Exception) {
                    Log.e("VerificationDialog", "Error during email verification", e)
                }
                delay(5000)
            }
        }

        btnContinue.setOnClickListener {
            if (auth.currentUser?.isEmailVerified == true) {
                dialog.dismiss()
                DialogUtils.showLoading(requireActivity()).dismiss()
                Toast.makeText(requireContext(), "Account Verified", Toast.LENGTH_SHORT).show();
                loginUser()
            } else {
                dialog.dismiss()
                DialogUtils.showLoading(requireActivity()).dismiss()
                loginUser()
            }
        }
    }

    private fun verifyEmail(user: FirebaseUser?) {
        loadingDialog.dismiss()

        if (user == null) {
            Log.e("EmailVerification", "User is not logged in. Cannot send verification email.")
            auth.signOut()
            return
        }

        user.sendEmailVerification()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d("EmailVerification", "Verification email sent to ${user.email}")
                    Toast.makeText(
                        requireContext(),
                        "Check your email for verification",
                        Toast.LENGTH_SHORT
                    ).show()
                    showVerificationDialog(user)

                    // Sign out the user immediately after sending the verification email
                    auth.signOut()
                } else {
                    Log.e("EmailVerification", "Failed to send verification email to ${user.email}")
                }
            }
            .addOnFailureListener { exception ->
                Log.e("EmailVerification", "Error sending verification email: ${exception.message}")
            }
    }


}