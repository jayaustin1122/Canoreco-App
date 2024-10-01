package com.example.canorecoapp.views.user.account

import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.example.canorecoapp.R
import com.example.canorecoapp.databinding.FragmentAddAccountBinding
import com.example.canorecoapp.viewmodels.UserViewModel
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.firestore.FirebaseFirestore
import org.bouncycastle.asn1.isismtt.x509.DeclarationOfMajority.dateOfBirth
import org.bouncycastle.cms.RecipientId.password

class AddAccountFragment : Fragment() {
    private lateinit var binding: FragmentAddAccountBinding
    private val viewModel: UserViewModel by viewModels()
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentAddAccountBinding.inflate(layoutInflater)
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

        binding.etAccountNumber.addTextChangedListener(object : TextWatcher {
            private var isFormatting: Boolean = false

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                // No action needed here
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // No action needed here
            }

            override fun afterTextChanged(s: Editable?) {
                if (isFormatting) return

                isFormatting = true

                // Get the original text
                val originalText = s.toString()
                // Remove non-digit characters
                val digits = originalText.replace(Regex("[^\\d]"), "")
                // Format the digits
                val formattedText = formatAccountNumber(digits)

                // Set the formatted text and update the cursor position
                binding.etAccountNumber.setText(formattedText)

                // Move cursor to the end of the text
                binding.etAccountNumber.setSelection(formattedText.length)

                isFormatting = false
            }
        })
        binding.btnSave.setOnClickListener {
            validateInputs()
        }
        binding.backButton.setOnClickListener {
            findNavController().navigateUp()
        }
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    val navController = findNavController()
                    if (navController.currentDestination?.id == R.id.userHolderFragment) {
                        navController.popBackStack()
                    } else {
                        val bundle = Bundle().apply {
                            putInt("selectedFragmentId", R.id.navigation_account)
                        }
                        navController.navigate(R.id.userHolderFragment, bundle)
                    }
                }
            }
        )

    }

    private fun validateInputs() {
        val accountNumber = binding.etAccountNumber.text.toString().trim()
        val accountName = binding.etAccountName.text.toString().trim()
       if (accountNumber.isEmpty()) {
            Toast.makeText(requireContext(), "PLease add or Complete Account Number", Toast.LENGTH_SHORT).show()
            return
        }
        else if (accountName.isEmpty()) {
            Toast.makeText(requireContext(), "Please add your Account Name", Toast.LENGTH_SHORT).show()
            return
        }
        else {
            bindInDb(accountNumber,accountName)
        }
    }

    private fun bindInDb(accountNumber: String, accountName: String) {
        val firestore = FirebaseFirestore.getInstance()
        val accountsRef = firestore.collection("accounts")

        accountsRef.document(accountNumber).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val consumerAccount = document.getString("consumerAccount")

                    if (consumerAccount.isNullOrEmpty()) {
                        val requestsRef = accountsRef.document(accountNumber).collection("requests")

                        viewModel.userInfo.observe(viewLifecycleOwner, Observer { userInfo ->
                            userInfo?.let {
                                val timestamp = System.currentTimeMillis() / 1000

                                val requestData = mapOf(
                                    "accountNumber" to accountNumber,
                                    "firstName" to userInfo.firstName,
                                    "lastName" to userInfo.lastName,
                                    "municipality" to userInfo.municipality,
                                    "barangay" to userInfo.barangay,
                                    "uid" to userInfo.uid,
                                    "timestamp" to timestamp.toString(),
                                    "status" to "pending"
                                )

                                requestsRef.document(timestamp.toString()).set(requestData)
                                    .addOnSuccessListener {
                                        Snackbar.make(requireView(), "Request added successfully", Snackbar.LENGTH_LONG).show()
                                        Log.d("Firestore", "Request added successfully with timestamp: $timestamp")
                                    }
                                    .addOnFailureListener { e ->
                                        Log.w("Firestore", "Error adding request: ${e.message}")
                                    }
                            }
                        })
                    } else {
                        Snackbar.make(requireView(), "This Account Number is Already in Use", Snackbar.LENGTH_LONG).show()
                    }
                } else {
                    Log.w("Firestore", "No such document with account number: $accountNumber")
                }
            }
            .addOnFailureListener { e ->
                Log.w("Firestore", "Error getting document: ${e.message}")
            }
    }



    private fun formatAccountNumber(digits: String): String {
        return when {
            digits.length >= 10 -> "${digits.substring(0, 2)}-${digits.substring(2, 6)}-${digits.substring(6, 10)}"
            digits.length >= 6 -> "${digits.substring(0, 2)}-${digits.substring(2)}"
            else -> digits
        }
    }

}
