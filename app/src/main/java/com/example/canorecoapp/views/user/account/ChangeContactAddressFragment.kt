package com.example.canorecoapp.views.user.account

import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.example.canorecoapp.R
import com.example.canorecoapp.databinding.FragmentChangeContactAddressBinding
import com.example.canorecoapp.utils.DialogUtils
import com.example.canorecoapp.utils.MunicipalityData.municipalitiesWithBarangays
import com.example.canorecoapp.viewmodels.UserViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore


class ChangeContactAddressFragment : Fragment() {
    private lateinit var binding : FragmentChangeContactAddressBinding
    private val viewModel: UserViewModel by viewModels()
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentChangeContactAddressBinding.inflate(layoutInflater)
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

                    binding.etContactNumber.setText(userInfo.phone)
                    binding.tvMunicipality.setText(userInfo.municipality)
                    binding.tvBrgy.setText(userInfo.barangay)
                }
            }
        })
        binding.backButton.setOnClickListener {
            DialogUtils.showWarningMessage(requireActivity(), "Warning", "Are you sure you want to exit? Changes will not be saved."
            ) { sweetAlertDialog ->
                sweetAlertDialog.dismissWithAnimation()
                val bundle = Bundle().apply {
                    putInt("selectedFragmentId", null ?: R.id.navigation_account)
                }
                findNavController().navigate(R.id.userHolderFragment, bundle)
            }
        }

        val municipalities = municipalitiesWithBarangays.keys.toList()
        val municipalityAdapter = ArrayAdapter(requireContext(), R.layout.address_item_views, municipalities)
        binding.tvMunicipality.setAdapter(municipalityAdapter)

        binding.tvMunicipality.setOnItemClickListener { parent, view, position, id ->
            val selectedMunicipality = parent.getItemAtPosition(position).toString()


            val barangays = municipalitiesWithBarangays[selectedMunicipality] ?: emptyList()
            val barangayAdapter = ArrayAdapter(requireContext(), R.layout.address_item_views, barangays)
            binding.tvBrgy.setAdapter(barangayAdapter)
        }

        binding.tvBrgy.setOnItemClickListener { parent, view, position, id ->
            val selectedBarangay = parent.getItemAtPosition(position).toString()
        }
        binding.btnSave.setOnClickListener {
            validateData()
        }
    }

    private fun validateData() {
        var phone = binding.etContactNumber.text.toString().trim()
        val barangay = binding.tvBrgy.text.toString().trim()
        val municipality = binding.tvMunicipality.text.toString().trim()

        if (phone.isEmpty()) {
            Toast.makeText(requireContext(), "Please add your Contact Number to continue", Toast.LENGTH_SHORT).show()
            return
        } else if (!phone.startsWith("09")) {
            Toast.makeText(requireContext(), "Phone number must start with '09'. Adjusting the number.", Toast.LENGTH_SHORT).show()
            phone = "09${phone.trimStart('0')}"
            return
        } else if (barangay.isEmpty()) {
            Toast.makeText(requireContext(), "Please add your Barangay to continue", Toast.LENGTH_SHORT).show()
            return
        } else if (municipality.isEmpty()) {
            Toast.makeText(requireContext(), "Please add your Municipality to continue", Toast.LENGTH_SHORT).show()
            return
        } else {
            updateInFirestore()
        }
    }

    private fun updateInFirestore() {
        val db = FirebaseFirestore.getInstance()
        val currentUser = FirebaseAuth.getInstance().currentUser

        val contactNumber = binding.etContactNumber.text.toString().trim()
        val municipality = binding.tvMunicipality.text.toString().trim()
        val barangay = binding.tvBrgy.text.toString().trim()
        val street = binding.etStreet.text.toString().trim()

        currentUser?.let { user ->

            val updatedData: HashMap<String, Any?> = hashMapOf(
                "phone" to contactNumber,
                "municipality" to municipality,
                "barangay" to barangay,
                "street" to street
            )

            // Update the Firestore document
            db.collection("users").document(user.uid)
                .update(updatedData)
                .addOnSuccessListener {
                    Toast.makeText(requireContext(), "Contact information updated successfully", Toast.LENGTH_SHORT).show()
                    findNavController().navigateUp()
                }
                .addOnFailureListener { exception ->
                    Toast.makeText(requireContext(), "Failed to update contact info: ${exception.message}", Toast.LENGTH_SHORT).show()
                }
        } ?: run {
            Toast.makeText(requireContext(), "User not authenticated", Toast.LENGTH_SHORT).show()
        }
    }

}