package com.example.canorecoapp.views.signups

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.ContentValues
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
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.ViewModelProvider
import com.example.canorecoapp.R
import com.example.canorecoapp.databinding.FragmentStepTwoBinding
import com.example.canorecoapp.utils.FirebaseUtils
import com.example.canorecoapp.utils.MunicipalityData.municipalitiesWithBarangays
import com.example.canorecoapp.viewmodels.SignUpViewModel


class StepTwoFragment : Fragment() {
    private lateinit var binding: FragmentStepTwoBinding

    private lateinit var viewModel: SignUpViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentStepTwoBinding.inflate(layoutInflater)
        // Inflate the layout for this fragment
        return binding.root
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(requireActivity()).get(SignUpViewModel::class.java)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.etContactNumber.addTextChangedListener {
            viewModel.phone = it.toString()
        }
        val municipalities = municipalitiesWithBarangays.keys.toList()
        val municipalityAdapter = ArrayAdapter(requireContext(), R.layout.address_item_views, municipalities)
        binding.tvMunicipality.setAdapter(municipalityAdapter)


        binding.tvMunicipality.setOnItemClickListener { parent, view, position, id ->
            val selectedMunicipality = parent.getItemAtPosition(position).toString()
            viewModel.address = selectedMunicipality
            binding.tvBrgy.setText("")
            val barangays = municipalitiesWithBarangays[selectedMunicipality] ?: emptyList()
            val barangayAdapter = ArrayAdapter(requireContext(), R.layout.address_item_views, barangays)
            binding.tvBrgy.setAdapter(barangayAdapter)
        }

        makeDropdownOnly(binding.tvMunicipality)
        makeDropdownOnly(binding.tvBrgy)
        binding.tvBrgy.setOnItemClickListener { parent, view, position, id ->
            val selectedBarangay = parent.getItemAtPosition(position).toString()
            viewModel.barangay = selectedBarangay
        }
        binding.etStreet.addTextChangedListener{
            viewModel.street = it.toString()
        }
    }
    override fun onResume() {
        super.onResume()
        val municipalities = municipalitiesWithBarangays.keys.toList()
        val municipalityAdapter = ArrayAdapter(requireContext(), R.layout.address_item_views, municipalities)
        binding.tvMunicipality.setAdapter(municipalityAdapter)
    }
    @SuppressLint("ClickableViewAccessibility")
    private fun makeDropdownOnly(autoCompleteTextView: AutoCompleteTextView) {
        autoCompleteTextView.setOnClickListener {
            autoCompleteTextView.showDropDown()
        }
        autoCompleteTextView.keyListener = null
        autoCompleteTextView.setFocusable(false)
        autoCompleteTextView.setOnTouchListener { _, _ ->
            autoCompleteTextView.showDropDown()
            false
        }
    }
}