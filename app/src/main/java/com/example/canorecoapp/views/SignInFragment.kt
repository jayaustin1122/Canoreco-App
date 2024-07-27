package com.example.canorecoapp.views

import android.app.Dialog
import android.app.ProgressDialog
import android.os.Bundle
import android.os.Handler
import android.util.Patterns
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.navigation.fragment.findNavController
import com.example.canorecoapp.R
import com.example.canorecoapp.databinding.DialogReviewBinding
import com.example.canorecoapp.databinding.FragmentSignInBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext


class SignInFragment : Fragment() {
    private lateinit var binding : FragmentSignInBinding
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
        binding = FragmentSignInBinding.inflate(layoutInflater)
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
                navigate(R.id.tvForgotPasswordLogin)
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
                        this@SignInFragment.requireContext(),
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
            val dbref = FirebaseFirestore.getInstance().collection("Users").document(firebaseUser.uid)
            dbref.get().addOnCompleteListener { task ->
                progressDialog.dismiss()
                if (task.isSuccessful) {
                    val snapshot = task.result
                    if (snapshot != null && snapshot.exists()) {
                        val userType = snapshot.getString("userType")
                        val access = snapshot.getBoolean("access")

                        when (userType) {
                            "admin" -> {
                                Toast.makeText(this@SignInFragment.requireContext(), "Login Successfully", Toast.LENGTH_SHORT).show()
                                progressDialog.setMessage("Redirecting...")
                                progressDialog.show()
                                findNavController().apply {
                                    popBackStack(R.id.splashFragment, false)
                                    navigate(R.id.adminHolderFragment)
                                }
                                progressDialog.dismiss()
                            }
                            "member" -> {
                                if (access == false) {
                                    val dialogBinding = DialogReviewBinding.inflate(layoutInflater)
                                    val dialog = Dialog(this@SignInFragment.requireContext())
                                    dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
                                    dialog.setContentView(dialogBinding.root)
                                    dialog.show()
                                    auth.signOut()
                                } else {
                                    Toast.makeText(this@SignInFragment.requireContext(), "Login Successfully", Toast.LENGTH_SHORT).show()
                                    progressDialog.setMessage("Redirecting...")
                                    progressDialog.show()
                                    findNavController().apply {
                                        popBackStack(R.id.splashFragment, false)
                                        navigate(R.id.userHolderFragment)
                                    }
                                    progressDialog.dismiss()
                                }
                            }
                            else -> {
                                Toast.makeText(this@SignInFragment.requireContext(), "There seems to be an issue with your account. Please contact the admin for assistance.", Toast.LENGTH_SHORT).show()
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
            progressDialog.dismiss()
            Toast.makeText(this@SignInFragment.requireContext(), "User not authenticated.", Toast.LENGTH_SHORT).show()
        }
    }

}