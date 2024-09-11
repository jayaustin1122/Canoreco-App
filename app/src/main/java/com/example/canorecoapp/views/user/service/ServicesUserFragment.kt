package com.example.canorecoapp.views.user.service

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.example.canorecoapp.R
import com.example.canorecoapp.databinding.FragmentServicesUserBinding

class ServicesUserFragment : Fragment() {
    private lateinit var binding: FragmentServicesUserBinding
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentServicesUserBinding.inflate(layoutInflater)
        // Inflate the layout for this fragment
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.outages.setOnClickListener {
            findNavController().apply {
                navigate(R.id.outagesFragment)
            }
        }
        binding.billingInformation.setOnClickListener {
            findNavController().apply {
                navigate(R.id.billingInformationFragment)
            }
        }
        binding.consumerComplaints.setOnClickListener {
            findNavController().apply {
                navigate(R.id.reportFragment)
            }
        }
        binding.serviceApplication.setOnClickListener {
            findNavController().apply {
                navigate(R.id.servicesFragment)
            }
        }
        binding.bayadCenters.setOnClickListener {
            findNavController().apply {
                navigate(R.id.bayadCentersFragment)
            }
        }
        binding.track.setOnClickListener {
            findNavController().apply {
                navigate(R.id.listOfMyComplaintsFragment)
            }
        }
    }

}