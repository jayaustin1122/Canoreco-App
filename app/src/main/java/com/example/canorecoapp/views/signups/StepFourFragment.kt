package com.example.canorecoapp.views.signups

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.ViewModelProvider
import com.example.canorecoapp.R
import com.example.canorecoapp.databinding.FragmentStepFourBinding
import com.example.canorecoapp.viewmodels.SignUpViewModel


class StepFourFragment : Fragment() {
    private lateinit var binding :  FragmentStepFourBinding
    private lateinit var viewModel: SignUpViewModel
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentStepFourBinding.inflate(layoutInflater)
        // Inflate the layout for this fragment

        return binding.root
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(requireActivity())[SignUpViewModel::class.java]
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        binding.phoneNumber.addTextChangedListener {
            viewModel.phone = it.toString()
        }

        binding.address.addTextChangedListener {
            viewModel.address = it.toString()
        }
    }
}