package com.example.canorecoapp.views.linemen.employee

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.Dialog
import android.app.ProgressDialog
import android.os.Bundle
import android.os.Handler
import android.text.InputType
import android.util.Log
import android.util.Patterns
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import cn.pedant.SweetAlert.SweetAlertDialog
import com.example.canorecoapp.R
import com.example.canorecoapp.databinding.DialogReviewBinding
import com.example.canorecoapp.databinding.FragmentLoginEmployeeBinding
import com.example.canorecoapp.utils.DialogUtils
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext


class LoginEmployeeFragment : Fragment() {
    private lateinit var binding : FragmentLoginEmployeeBinding
    private lateinit var auth : FirebaseAuth
    private lateinit var loadingDialog: SweetAlertDialog
    private var doubleBackToExitPressedOnce = false
    private val handler = Handler()
    private lateinit var fireStore : FirebaseFirestore
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentLoginEmployeeBinding.inflate(layoutInflater)
        // Inflate the layout for this fragment
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        auth = FirebaseAuth.getInstance()
        fireStore = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()
        fireStore = FirebaseFirestore.getInstance()
        binding.etPass.inputType =
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        binding.etUsernameLogin.inputType =
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS

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
                navigate(R.id.signUpFragment)
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
                Toast.makeText(requireContext(), "Press back again to exit", Toast.LENGTH_SHORT).show()
                handler.postDelayed({ doubleBackToExitPressedOnce = false }, 2000)
            }
        }
    }
    private fun checkUser() {
        val phoneOrEmail = binding.etUsernameLogin.text?.trim().toString()
        val password = binding.etPass.text?.trim().toString()

        // Check if phoneOrEmail and password are not empty
        if (phoneOrEmail.isEmpty() || password.isEmpty()) {
            loadingDialog.dismiss()
            Toast.makeText(requireContext(), "Phone/Email or password cannot be empty.", Toast.LENGTH_SHORT).show()
            return
        }

        val dbref = FirebaseFirestore.getInstance().collection("users")

        // Determine whether the input is a phone number or an email
        val isEmail = android.util.Patterns.EMAIL_ADDRESS.matcher(phoneOrEmail).matches()

        // Query Firestore: if it's an email, check the "email" field, otherwise check the "phone" field
        val query = if (isEmail) {
            dbref.whereEqualTo("email", phoneOrEmail) // Query by email
        } else {
            dbref.whereEqualTo("phone", phoneOrEmail) // Query by phone number
        }

        // Execute the query
        query.get().addOnCompleteListener { task ->
            loadingDialog.dismiss()  // Dismiss the loading dialog once the task is complete

            if (task.isSuccessful) {
                val snapshot = task.result
                if (snapshot != null && !snapshot.isEmpty) {
                    // Assume the first document matches the user (in case multiple documents are returned)
                    val userDoc = snapshot.documents.first()
                    val authEmail = userDoc.getString("authEmail")
                    val userType = userDoc.getString("userType")

                    if (!authEmail.isNullOrEmpty()) {
                        // Proceed to login with the fetched email
                        loginUser(authEmail, userType)
                    } else {
                        loadingDialog.dismiss()
                        // authEmail not found in the user document
                        Toast.makeText(requireContext(), "Phone/Email is wrong.", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    loadingDialog.dismiss()
                    // User not found
                    Toast.makeText(requireContext(), "User not found.", Toast.LENGTH_SHORT).show()
                }
            } else {
                loadingDialog.dismiss()
                // Error occurred while fetching user data
                Toast.makeText(requireContext(), "An error occurred: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    private fun loginUser(authEmail: String?, userType: String?) {
        val password = binding.etPass.text.toString()

        if (authEmail.isNullOrEmpty()) {
            loadingDialog.dismiss()
            Toast.makeText(requireContext(), "authEmail is null, cannot log in.", Toast.LENGTH_SHORT).show()
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Sign in using the retrieved authEmail and the entered password
                auth.signInWithEmailAndPassword(authEmail, password).await()

                withContext(Dispatchers.Main) {
                    // After login, check the user type and navigate accordingly
                    checkUserType(userType)
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    loadingDialog.dismiss()  // Dismiss the loading dialog on failure
                    // Handle login failure
                    Toast.makeText(this@LoginEmployeeFragment.requireContext(), "Password is Incorrect", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    private fun checkUserType(userType: String?) {
        when (userType) {
            "linemen" -> {
                loadingDialog.dismiss()  // Dismiss the loading dialog when done
                Toast.makeText(this@LoginEmployeeFragment.requireContext(), "Login Successfully", Toast.LENGTH_SHORT).show()
                // Navigate to adminHolderFragment for "linemen"
                findNavController().apply {
                    popBackStack(R.id.splashFragment, false)
                    navigate(R.id.adminHolderFragment)
                }
            }

            "member" -> {
                loadingDialog.dismiss()
                DialogUtils.showSuccessMessage(
                    requireActivity(),
                    "Success",
                    "Welcome to Canoreco App"
                ).show()
                // Navigate to userHolderFragment for verified members
                findNavController().apply {
                    popBackStack(R.id.splashFragment, false)
                    navigate(R.id.userHolderFragment)
                }

            }

            else -> {
                loadingDialog.dismiss()
                // Handle unknown userType or show an appropriate message
                Toast.makeText(requireContext(), "Unknown user type. Please contact support.", Toast.LENGTH_SHORT).show()
            }
        }
    }

}