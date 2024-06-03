package com.example.canorecoapp.views.signups

import android.app.DatePickerDialog
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import com.example.canorecoapp.R
import com.example.canorecoapp.databinding.FragmentStepThreeBinding
import com.example.canorecoapp.viewmodels.SignUpViewModel
import com.google.android.material.datepicker.MaterialDatePicker
import java.util.Calendar


class StepThreeFragment : Fragment() {
    private lateinit var binding: FragmentStepThreeBinding
    private lateinit var viewModel: SignUpViewModel
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentStepThreeBinding.inflate(layoutInflater)
        // Inflate the layout for this fragment
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.btnDatePicker.setOnClickListener {
            showDatePickerDialog()
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(requireActivity()).get(SignUpViewModel::class.java)
    }
    private fun showDatePickerDialog() {
        val datePicker = MaterialDatePicker.Builder.datePicker()
            .setTitleText("Select Date")
            .build()
        datePicker.addOnPositiveButtonClickListener { selection ->
            val calendar = Calendar.getInstance().apply {
                timeInMillis = selection
            }
            viewModel.year = calendar.get(Calendar.YEAR).toString()
            viewModel.month = (calendar.get(Calendar.MONTH) + 1).toString()
            viewModel.day = calendar.get(Calendar.DAY_OF_MONTH).toString()
        }
        datePicker.show(parentFragmentManager, "MaterialDatePicker")
    }

}