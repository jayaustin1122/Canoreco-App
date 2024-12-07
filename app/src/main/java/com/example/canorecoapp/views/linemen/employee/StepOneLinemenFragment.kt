package com.example.canorecoapp.views.linemen.employee

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.ViewModelProvider
import com.example.canorecoapp.R
import com.example.canorecoapp.databinding.FragmentStepOneLinemenBinding
import com.example.canorecoapp.viewmodels.SignUpViewModel


class StepOneLinemenFragment : Fragment() {
    private lateinit var binding: FragmentStepOneLinemenBinding
    private lateinit var viewModel: SignUpViewModel
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentStepOneLinemenBinding.inflate(layoutInflater)
        // Inflate the layout for this fragment
        return binding.root
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel = ViewModelProvider(requireActivity())[SignUpViewModel::class.java]
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.etFirstNameSignUp.addTextChangedListener {
            viewModel.firstName = it.toString()
        }

        binding.etLastNameSignUp.addTextChangedListener {
            viewModel.lastName = it.toString()
        }
        binding.etPosition.addTextChangedListener {
            viewModel.position = it.toString()
        }

    }

}