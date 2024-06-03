package com.example.canorecoapp.views.signups

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.ViewModelProvider
import com.example.canorecoapp.R
import com.example.canorecoapp.databinding.FragmentStepOneBinding
import com.example.canorecoapp.viewmodels.SignUpViewModel


class StepOneFragment : Fragment() {
    private lateinit var binding: FragmentStepOneBinding
    private lateinit var viewModel: SignUpViewModel
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentStepOneBinding.inflate(layoutInflater)
        // Inflate the layout for this fragment
        return binding.root
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(requireActivity())[SignUpViewModel::class.java]
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.etFullname.addTextChangedListener {
            viewModel.fullname = it.toString()
        }

        binding.etEmailSignUp.addTextChangedListener {
            viewModel.email = it.toString()
        }

        binding.etPasswordSignUp.addTextChangedListener {
            viewModel.password = it.toString()
        }
    }
}