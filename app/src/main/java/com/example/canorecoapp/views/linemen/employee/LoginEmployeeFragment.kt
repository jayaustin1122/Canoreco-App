package com.example.canorecoapp.views.linemen.employee

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.Dialog
import android.app.ProgressDialog
import android.os.Bundle
import android.os.Handler
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
import com.example.canorecoapp.R
import com.example.canorecoapp.databinding.DialogReviewBinding
import com.example.canorecoapp.databinding.FragmentLoginEmployeeBinding
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
    private lateinit var progressDialog : ProgressDialog
    private var backPressTime = 0L
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
        progressDialog = ProgressDialog(this.requireContext())
        progressDialog.setTitle("Please wait")
        progressDialog.setCanceledOnTouchOutside(false)
        handler.postDelayed({ doubleBackToExitPressedOnce = false }, 2000)
        binding.buttonLoginLogin.setOnClickListener {
            validateData()
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

    private fun validateData() {
        email = binding.etUsernameLogin.text.toString().trim()
        pass = binding.etPass.text.toString().trim()

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()){
            //invalid email
            Toast.makeText(this.requireContext(),"Email Invalid", Toast.LENGTH_SHORT).show()
        }
        else if (pass.isEmpty()){
            Toast.makeText(this.requireContext(),"Empty Fields are not allowed", Toast.LENGTH_SHORT).show()
        }
        else{
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
                Toast.makeText(requireContext(), "Press back again to exit", Toast.LENGTH_SHORT).show()
                handler.postDelayed({ doubleBackToExitPressedOnce = false }, 2000)
            }
        }
    }
    private fun loginUser() {
        val email = binding.etUsernameLogin.text.toString()
        val password = binding.etPass.text.toString()
        progressDialog.setMessage("Logging In...")
        progressDialog.show()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                auth.signInWithEmailAndPassword(email,password).await()
                withContext(Dispatchers.Main){
                    checkUser()
                }

            }
            catch (e : Exception){
                withContext(Dispatchers.Main){
                    progressDialog.dismiss()
                    Toast.makeText(
                        this@LoginEmployeeFragment.requireContext(),
                        "${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }

            }
        }
    }
    private fun checkUser() {
        progressDialog.setTitle("Checking user")
        progressDialog.setMessage("Signing In...")
        progressDialog.show()

        val firebaseUser = auth.currentUser

        if (firebaseUser != null) {
            val dbref = FirebaseFirestore.getInstance().collection("users").document(firebaseUser.uid)
            dbref.get().addOnCompleteListener { task ->
                progressDialog.dismiss()
                if (task.isSuccessful) {
                    val snapshot = task.result
                    if (snapshot != null && snapshot.exists()) {
                        val userType = snapshot.getString("userType")
                        val access = snapshot.getBoolean("access")
                        when (userType) {
                            "linemen" -> {
                                if (access == false) {
                                    val dialogBinding = DialogReviewBinding.inflate(layoutInflater)
                                    val dialog = Dialog(this@LoginEmployeeFragment.requireContext())
                                    dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
                                    dialog.setContentView(dialogBinding.root)
                                    dialog.show()
                                    auth.signOut()
                                }
                                else{

                                    Toast.makeText(
                                        this@LoginEmployeeFragment.requireContext(),
                                        "Login Successfully",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    progressDialog.setMessage("Redirecting...")
                                    progressDialog.show()
                                    findNavController().apply {
                                        popBackStack(R.id.splashFragment, false)
                                        navigate(R.id.adminHolderFragment)
                                    }

                                    progressDialog.dismiss()
                                }

                            }
                            "member" -> {
                                if (auth.currentUser?.isEmailVerified == true) {
                                    findNavController().apply {
                                        popBackStack(R.id.splashFragment, false)
                                        navigate(R.id.userHolderFragment)
                                    }
                                } else if (auth.currentUser?.isEmailVerified == false) {
                                    verifyEmail(firebaseUser)
                                }
                            }

                            else-> {

                            }
                        }
                    } else {
                        Toast.makeText(this@LoginEmployeeFragment.requireContext(), "User not found.", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this@LoginEmployeeFragment.requireContext(), "An error occurred. Please try again later.", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            progressDialog.dismiss()
            Toast.makeText(this@LoginEmployeeFragment.requireContext(), "User not authenticated.", Toast.LENGTH_SHORT).show()
        }
    }
    @SuppressLint("MissingInflatedId")
    private fun showVerificationDialog(user: FirebaseUser) {
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
        }

        lifecycleScope.launch {
            while (auth.currentUser?.isEmailVerified == false) {
                try {
                    auth.currentUser?.reload()?.addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            if (auth.currentUser?.isEmailVerified == true) {
                                btnContinue.isEnabled = true
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
            } else {
                Toast.makeText(requireContext(), "Please verify your email before proceeding.", Toast.LENGTH_SHORT).show()
            }
        }
    }
    private fun verifyEmail(user: FirebaseUser?) {
        if (user == null) {
            Log.e("EmailVerification", "User is not logged in. Cannot send verification email.")
            return
        }
        user.sendEmailVerification()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d("EmailVerification", "Verification email sent to ${user.email}")
                    showVerificationDialog(user)
                } else {
                    Log.e("EmailVerification", "Failed to send verification email to ${user.email}. Task failed.")

                }
            }
            .addOnFailureListener { exception ->
                Log.e("EmailVerification", "Error sending verification email: ${exception.message}")

            }
    }
}