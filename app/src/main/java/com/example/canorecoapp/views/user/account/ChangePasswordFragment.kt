package com.example.canorecoapp.views.user.account

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.example.canorecoapp.R
import com.example.canorecoapp.databinding.FragmentChangePasswordBinding
import com.example.canorecoapp.utils.DialogUtils
import com.example.canorecoapp.viewmodels.UserViewModel
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore


class ChangePasswordFragment : Fragment() {
    private lateinit var binding: FragmentChangePasswordBinding
    private val viewModel: UserViewModel by viewModels()
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
        viewModel.errorMessage.observe(viewLifecycleOwner, Observer { errorMessage ->
            if (errorMessage != null) {
                Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_SHORT).show()
            }
        })

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            viewModel.loadUserInfo()
        }
        viewModel.userInfo.observe(viewLifecycleOwner, Observer { userInfo ->
            userInfo?.let {
                binding.apply {
                    btnSave.setOnClickListener {
                        DialogUtils.showWarningMessage(requireActivity(), "Warning", "Are you sure you want to update your password?."
                        ) { sweetAlertDialog ->
                            sweetAlertDialog.dismissWithAnimation()
                            validateData(userInfo.password, userInfo.authEmail)
                        }
                    }
                }
                if (userInfo.userType == "member") {
                    requireActivity().onBackPressedDispatcher.addCallback(
                        viewLifecycleOwner,
                        object : OnBackPressedCallback(true) {
                            override fun handleOnBackPressed() {
                                val bundle = Bundle().apply {
                                    putInt("selectedFragmentId", R.id.navigation_account)
                                }
                                findNavController().navigate(R.id.userHolderFragment, bundle)
                            }
                        }
                    )
                    binding.backButton.setOnClickListener {
                        DialogUtils.showWarningMessage(
                            requireActivity(),
                            "Warning",
                            "Are you sure you want to exit? Changes will not be saved."
                        ) { sweetAlertDialog ->
                            sweetAlertDialog.dismissWithAnimation()

                            val bundle = Bundle().apply {
                                putInt("selectedFragmentId", R.id.navigation_account)
                            }
                            findNavController().navigate(R.id.userHolderFragment, bundle)
                        }
                    }
                } else {
                    requireActivity().onBackPressedDispatcher.addCallback(
                        viewLifecycleOwner,
                        object : OnBackPressedCallback(true) {
                            override fun handleOnBackPressed() {
                                val bundle = Bundle().apply {
                                    putInt("selectedFragmentId", R.id.navigation_account_linemen)
                                }
                                findNavController().navigate(R.id.adminHolderFragment, bundle)
                            }
                        }
                    )
                    binding.backButton.setOnClickListener {
                        DialogUtils.showWarningMessage(
                            requireActivity(),
                            "Warning",
                            "Are you sure you want to exit? Changes will not be saved."
                        ) { sweetAlertDialog ->
                            sweetAlertDialog.dismissWithAnimation()

                            val bundle = Bundle().apply {
                                putInt("selectedFragmentId", R.id.navigation_account_linemen)
                            }
                            findNavController().navigate(R.id.adminHolderFragment, bundle)
                        }
                    }
                }

            }
        })

    }

    private fun validateData(password: String?, email: String?) {
        val oldPass = binding.etOldPassword.text.toString().trim()
        val confirmPass = binding.etConfirmPassword.text.toString().trim()
        val newPass = binding.etNewPassword.text.toString().trim()


        if (oldPass.isEmpty()) {
            Toast.makeText(requireContext(), "Password cannot be empty", Toast.LENGTH_SHORT).show()
            return
        } else if (confirmPass.isEmpty()) {
            Toast.makeText(requireContext(), "Please confirm your password", Toast.LENGTH_SHORT)
                .show()
            return
        } else if (newPass != confirmPass) {
            Toast.makeText(requireContext(), "Passwords do not match", Toast.LENGTH_SHORT).show()
            return
        } else {
            updatePassword(password, email)
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
                        Toast.makeText(requireContext(), "Password Updated", Toast.LENGTH_SHORT)
                            .show()
                        // Update the password field in Firestore
                        firestore.collection("users")
                            .document(userId!!)
                            .update("password", newPassword)
                            .addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    viewModel.userInfo.observe(
                                        viewLifecycleOwner,
                                        Observer { userInfo ->
                                            userInfo?.let {
                                                if (userInfo.userType == "member") {
                                                    val bundle = Bundle().apply {
                                                        putInt(
                                                            "selectedFragmentId",
                                                            R.id.navigation_account
                                                        )
                                                    }
                                                    findNavController().navigate(
                                                        R.id.userHolderFragment,
                                                        bundle
                                                    )
                                                } else {
                                                    val bundle = Bundle().apply {
                                                        putInt(
                                                            "selectedFragmentId",
                                                            R.id.navigation_account_linemen
                                                        )
                                                    }
                                                    findNavController().navigate(
                                                        R.id.adminHolderFragment,
                                                        bundle
                                                    )
                                                }
                                            }
                                        })
                                } else {
                                    Toast.makeText(
                                        this.requireContext(),
                                        task.exception?.message
                                            ?: "Error updating password in Firestore",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                    } else {
                        Toast.makeText(
                            requireContext(),
                            "Your Old Password is wrong please check and try again",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } else {
                Toast.makeText(
                    requireContext(),
                    "Your Old Password is wrong please check and try again",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

}