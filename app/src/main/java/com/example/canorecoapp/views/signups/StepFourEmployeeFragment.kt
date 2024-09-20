package com.example.canorecoapp.views.signups

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.canorecoapp.R
import com.example.canorecoapp.databinding.FragmentStepFourEmployeeBinding
import com.example.canorecoapp.viewmodels.SignUpViewModel

class StepFourEmployeeFragment : Fragment() {
    private lateinit var binding: FragmentStepFourEmployeeBinding
    private lateinit var viewModel: SignUpViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(requireActivity()).get(SignUpViewModel::class.java)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentStepFourEmployeeBinding.inflate(inflater, container, false)
        return binding.root
    }

    private val areas = listOf("Area 1", "Area 2", "Area 3", "Area 4")

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.etEmailSignUp.addTextChangedListener {
            viewModel.email = it.toString()
        }

        binding.etPasswordSignUp.addTextChangedListener {
            viewModel.password = it.toString()
        }

        binding.etConfirmPasswordSignUp.addTextChangedListener {
            viewModel.confirmPass = it.toString()
        }

        val barangayAdapter = ArrayAdapter(requireContext(), R.layout.address_item_views, areas)
        binding.tvArea.setAdapter(barangayAdapter)

        binding.tvArea.setOnItemClickListener { parent, view, position, id ->
            val selectedBarangay = parent.getItemAtPosition(position).toString()
            viewModel.area = selectedBarangay
        }
    }
}
