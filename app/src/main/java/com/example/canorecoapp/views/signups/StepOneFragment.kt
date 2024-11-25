package com.example.canorecoapp.views.signups

import android.app.Activity
import android.app.AlertDialog
import android.content.ContentValues
import android.content.ContentValues.TAG
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.example.bidnshare.notification.FirebaseServiceCanoreco.Companion.token
import com.example.canorecoapp.R
import com.example.canorecoapp.databinding.FragmentStepOneBinding
import com.example.canorecoapp.utils.DialogUtils
import com.example.canorecoapp.utils.FirebaseUtils
import com.example.canorecoapp.viewmodels.SignUpViewModel
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.firebase.firestore.FirebaseFirestore
import org.bouncycastle.asn1.x500.style.RFC4519Style.uid
import java.util.Calendar
import kotlin.random.Random

class StepOneFragment : Fragment() {
    private lateinit var binding: FragmentStepOneBinding
    private lateinit var viewModel: SignUpViewModel

    private lateinit var firebaseUtils: FirebaseUtils

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentStepOneBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel = ViewModelProvider(requireActivity())[SignUpViewModel::class.java]
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        firebaseUtils = FirebaseUtils()
        firebaseUtils.initialize(requireContext())
        binding.search.setOnClickListener {
            val accountNumber = binding.etAccountNumber.text.toString()
            if (accountNumber.isNotEmpty()) {
                fetchAccountDetails(accountNumber)
            } else {
                Toast.makeText(context, "Please enter an account number", Toast.LENGTH_SHORT).show()
            }
        }
    }
    private fun fetchAccountDetails(accountNumber: String) {
        firebaseUtils.fireStore.collection("accounts")
            .document(accountNumber)
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    // Fetch and log the fields from Firestore
                    val consumerAccount = document.getString("consumerAccount") ?: "N/A"
                    val accountNumbers = document.getString("accountNumber") ?: "N/A"
                    val barangay = document.getString("barangay") ?: "N/A"
                    val firstName = document.getString("firstName") ?: "N/A"
                    val lastName = document.getString("lastName") ?: "N/A"
                    val municipality = document.getString("municipality") ?: "N/A"
                    val status = document.getString("status") ?: "N/A"
                    val street = document.getString("street") ?: "N/A"

                    // Log all fields
                    Log.d(TAG, "Consumer Account: $consumerAccount")
                    Log.d(TAG, "Barangay: $barangay")
                    Log.d(TAG, "First Name: $firstName")
                    Log.d(TAG, "Last Name: $lastName")
                    Log.d(TAG, "Municipality: $municipality")
                    Log.d(TAG, "Status: $status")
                    Log.d(TAG, "Street: $street")

                    if (status == "unlinked") {
                        // Set the view model data
                        viewModel.firstName = firstName
                        viewModel.lastName = lastName
                        viewModel.barangay = barangay
                        viewModel.street = street
                        viewModel.municipality = municipality
                        viewModel.meterNumber = accountNumbers
                        Log.d("logging", "Account Number: ${viewModel.meterNumber}")

                        // Update UI to display the account details
                        binding.divider1.visibility = View.VISIBLE
                        binding.accountDetailsTextView.visibility = View.VISIBLE
                        binding.accountNameTextView.visibility = View.VISIBLE
                        binding.accountNumberTextView.visibility = View.VISIBLE

                        binding.accountNameTextInputLayout.visibility = View.VISIBLE
                        binding.etAccountName.setText(firstName + " " + lastName)

                        binding.municipality.visibility = View.VISIBLE
                        binding.etMunicipality.setText(municipality)

                        binding.barangaytextInputLayout.visibility = View.VISIBLE
                        binding.etBarangay.setText(barangay)

                        binding.streetTextInputLayout.visibility = View.VISIBLE
                        binding.etStreet.setText(street)
                    } else {
                        // If the status is 'linked', show a Toast
                        Toast.makeText(context, "This account is already in use", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(context, "Account not found", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "Error fetching account details", e)
                Toast.makeText(context, "Error retrieving details", Toast.LENGTH_SHORT).show()
            }
    }



}
