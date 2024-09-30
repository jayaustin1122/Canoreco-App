package com.example.canorecoapp.views

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.Dialog
import android.app.ProgressDialog
import android.os.Bundle
import android.os.Handler
import android.os.Looper
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
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import cn.pedant.SweetAlert.SweetAlertDialog
import com.example.canorecoapp.R
import com.example.canorecoapp.databinding.DialogReviewBinding
import com.example.canorecoapp.databinding.FragmentSignInBinding
import com.example.canorecoapp.utils.DialogUtils
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext


class SignInFragment : Fragment() {
    private lateinit var binding : FragmentSignInBinding
    private lateinit var auth : FirebaseAuth
    private lateinit var loadingDialog: SweetAlertDialog
    private lateinit var successDialog: SweetAlertDialog
    private var backPressTime = 0L
    private var doubleBackToExitPressedOnce = false
    private val handler = Handler()
    private lateinit var fireStore : FirebaseFirestore
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentSignInBinding.inflate(layoutInflater)
        // Inflate the layout for this fragment
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        auth = FirebaseAuth.getInstance()
        fireStore = FirebaseFirestore.getInstance()
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
        loadingDialog = DialogUtils.showLoading(requireActivity())
        loadingDialog.show()
        email = binding.etUsernameLogin.text.toString().trim()
        pass = binding.etPass.text.toString().trim()

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()){
            //invalid email
            Toast.makeText(this.requireContext(),"Email Invalid", Toast.LENGTH_SHORT).show()
            loadingDialog.dismiss()
        }
        else if (pass.isEmpty()){
            Toast.makeText(this.requireContext(),"Empty Fields are not allowed", Toast.LENGTH_SHORT).show()
            loadingDialog.dismiss()
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
        CoroutineScope(Dispatchers.IO).launch {
            try {
                auth.signInWithEmailAndPassword(email,password).await()
                withContext(Dispatchers.Main){
                    checkUser()
                }

            }
            catch (e : Exception){
                withContext(Dispatchers.Main){
                    loadingDialog.dismiss()
                    Toast.makeText(
                        this@SignInFragment.requireContext(),
                        "${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }

            }
        }
    }
    private fun checkUser() {


        val firebaseUser = auth.currentUser

        if (firebaseUser != null) {
            val dbref = FirebaseFirestore.getInstance().collection("users").document(firebaseUser.uid)
            dbref.get().addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val snapshot = task.result
                    if (snapshot != null && snapshot.exists()) {
                        val userType = snapshot.getString("userType")
                        val access = snapshot.getBoolean("access")
                        val name = snapshot.getString("firstName")
                        when (userType) {
                            "linemen" -> {
                                if (access == false) {
                                    loadingDialog.dismiss()
                                    val dialogBinding = DialogReviewBinding.inflate(layoutInflater)
                                    val dialog = Dialog(this@SignInFragment.requireContext())
                                    dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
                                    dialog.setContentView(dialogBinding.root)
                                    dialog.show()
                                    auth.signOut()
                                }
                                else{
                                    loadingDialog.dismiss()
                                    successDialog = DialogUtils.showSuccessMessage(requireActivity(), "Log In Successful", "Welcome $name")
                                    successDialog.show()
                                Toast.makeText(
                                    this@SignInFragment.requireContext(),
                                    "Login Successfully",
                                    Toast.LENGTH_SHORT
                                ).show()
                                findNavController().apply {
                                    popBackStack(R.id.splashFragment, false)
                                    navigate(R.id.adminHolderFragment)
                                }

                                loadingDialog.dismiss()
                            }

                            }
                            "member" -> {
                                loadingDialog.dismiss()
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
                        Toast.makeText(this@SignInFragment.requireContext(), "User not found.", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this@SignInFragment.requireContext(), "An error occurred. Please try again later.", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            loadingDialog.dismiss()
            Toast.makeText(this@SignInFragment.requireContext(), "User not authenticated.", Toast.LENGTH_SHORT).show()
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
                Toast.makeText(requireContext(), "Account Verified", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(requireContext(), "Please verify your email before proceeding.", Toast.LENGTH_SHORT).show()
            }
        }
    }
    private fun verifyEmail(user: FirebaseUser?) {
        if (user == null) {
            Log.e("EmailVerification", "User is not logged in. Cannot send verification email.")
            auth.signOut()
            return
        }
        user.sendEmailVerification()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d("EmailVerification", "Verification email sent to ${user.email}")
                    Toast.makeText(requireContext(), "Check your email for verification", Toast.LENGTH_SHORT).show();
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