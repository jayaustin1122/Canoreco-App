package com.example.canorecoapp

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.canorecoapp.databinding.FragmentDeviceBinding
import com.example.canorecoapp.viewmodels.DeviceViewModel

class DeviceFragment : Fragment() {
    private lateinit var binding: FragmentDeviceBinding
    private val viewModel: DeviceViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentDeviceBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Observe real-time changes for device 9000
        viewModel.device9000Status.observe(viewLifecycleOwner) { status ->
            binding.buttonDevice9000.text = "Device 9000 ($status)"
            binding.buttonDevice9000.setBackgroundColor(
                if (status == "damaged") Color.RED else Color.BLUE
            )
        }

        // Observe real-time changes for device 9001
        viewModel.device9001Status.observe(viewLifecycleOwner) { status ->
            binding.buttonDevice9001.text = "Device 9001 ($status)"
            binding.buttonDevice9001.setBackgroundColor(
                if (status == "damaged") Color.RED else Color.BLUE
            )
        }

        // Handle button clicks
        binding.buttonDevice9000.setOnClickListener {
            val currentStatus = viewModel.device9000Status.value ?: "working"
            viewModel.toggleDeviceStatus("9000", currentStatus)
        }

        binding.buttonDevice9001.setOnClickListener {
            val currentStatus = viewModel.device9001Status.value ?: "working"
            viewModel.toggleDeviceStatus("9001", currentStatus)
        }
    }
}
