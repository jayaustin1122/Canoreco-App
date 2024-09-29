package com.example.canorecoapp.views.user.service

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import com.example.canorecoapp.R
import com.example.canorecoapp.databinding.FragmentServicesUserBinding

class ServicesUserFragment : Fragment() {
    private lateinit var binding: FragmentServicesUserBinding
    private var selectedFragmentId: Int? = null
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
        arguments?.let {
            selectedFragmentId = it.getInt("selectedFragmentId", R.id.navigation_services)
        }
        binding.outages.setOnClickListener {
            findNavController().apply {
                val bundle = Bundle().apply {
                    putInt("selectedFragmentId", selectedFragmentId ?: R.id.navigation_services)
                }
                navigate(R.id.outagesFragment, bundle)
            }
        }

        binding.consumerComplaints.setOnClickListener {
            findNavController().apply {
                val bundle = Bundle().apply {
                    putInt("selectedFragmentId", selectedFragmentId ?: R.id.navigation_services)
                }
                navigate(R.id.reportFragment, bundle)
            }
        }
        binding.serviceApplication.setOnClickListener {
            findNavController().apply {
                navigate(R.id.servicesFragment)
            }
        }
        binding.bayadCenters.setOnClickListener {
            findNavController().apply {
                val bundle = Bundle().apply {
                    putInt("selectedFragmentId", selectedFragmentId ?: R.id.navigation_services)
                }
                navigate(R.id.bayadCentersFragment, bundle)
            }
        }
        binding.billingInformation.setOnClickListener {
            findNavController().apply {
                val bundle = Bundle().apply {
                    putInt("selectedFragmentId", selectedFragmentId ?: R.id.navigation_services)
                }
                navigate(R.id.billingInformationFragment, bundle)
            }
        }
        binding.track.setOnClickListener {
            findNavController().apply {
                val bundle = Bundle().apply {
                    putInt("selectedFragmentId", selectedFragmentId ?: R.id.navigation_services)
                }
                navigate(R.id.listOfMyComplaintsFragment, bundle)
            }
        }
    }

}