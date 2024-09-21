package com.example.canorecoapp.views.user.account

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.canorecoapp.R
import com.example.canorecoapp.databinding.FragmentChangePasswordBinding
import com.example.canorecoapp.utils.DialogUtils
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore


class ChangePasswordFragment : Fragment() {
    private lateinit var binding : FragmentChangePasswordBinding
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentChangePasswordBinding.inflate(layoutInflater)
        // Inflate the layout for this fragment
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
       loadUsersInfo()
        binding.backButton.setOnClickListener {
            DialogUtils.showWarningMessage(requireActivity(), "Warning", "Are you sure you want to exit? Changes will not be saved."
            ) { sweetAlertDialog ->
                sweetAlertDialog.dismissWithAnimation()
                findNavController().navigateUp()
            }
        }
    }

    private fun validateData(password: String?, email: String?) {
         val oldPass = binding.etOldPassword.text.toString()
         val confirmPass = binding.etConfirmPassword.text.toString()
         val newPass = binding.etNewPassword.text.toString()


             if (oldPass.isEmpty()) {
                Toast.makeText(requireContext(), "Password cannot be empty", Toast.LENGTH_SHORT).show()
                return
            }
            else if (confirmPass.isEmpty()) {
                Toast.makeText(requireContext(), "Please confirm your password", Toast.LENGTH_SHORT).show()
                return
            }
            else if (newPass != confirmPass) {
                Toast.makeText(requireContext(), "Passwords do not match", Toast.LENGTH_SHORT).show()
                return
            }
            else {
                 updatePassword(password,email)
             }

    }

    fun updatePassword(
        oldUserPassword: String?,
        email: String?,
    ) {
        val firestore = FirebaseFirestore.getInstance()
        val auth = FirebaseAuth.getInstance()
        val userId = auth.currentUser?.uid
        val user = auth.currentUser
        val newPassword = binding.etNewPassword.text.toString()

        // Reauthenticate the user
        val credential: AuthCredential = EmailAuthProvider.getCredential(email!!, oldUserPassword!!)
        user?.reauthenticate(credential)?.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                // Update password in Firebase Authentication
                user.updatePassword(newPassword).addOnCompleteListener {
                    if (it.isSuccessful) {
                        Toast.makeText(requireContext(), "Password Updated", Toast.LENGTH_SHORT).show()
                        // Update the password field in Firestore
                        firestore.collection("users")
                            .document(userId!!)
                            .update("password", newPassword)
                            .addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    findNavController().navigateUp()
                                } else {
                                    Toast.makeText(
                                        this.requireContext(),
                                        task.exception?.message ?: "Error updating password in Firestore",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                    } else {
                        Toast.makeText(requireContext(), "Error Updating Password in Firebase Auth", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                Toast.makeText(requireContext(), "Reauthentication failed", Toast.LENGTH_SHORT).show()
            }
        }
    }
    private fun loadUsersInfo() {
        val db = FirebaseFirestore.getInstance()
        val currentUser = FirebaseAuth.getInstance().currentUser

        currentUser?.let { user ->
            db.collection("users").document(user.uid).get()
                .addOnSuccessListener { document ->

                    val userName = document.getString("firstName")
                    val lastName = document.getString("lastName")
                    val contact = document.getString("phone")
                    val image = document.getString("image")
                    val email = document.getString("email")
                    val municipality = document.getString("municipality")
                    val barangay = document.getString("barangay")
                    val password = document.getString("password")
                    val street = document.getString("street")
                    binding.etOldPassword.setText(password)
                    binding.btnSave.setOnClickListener {
                        validateData(password,email)
                    }


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