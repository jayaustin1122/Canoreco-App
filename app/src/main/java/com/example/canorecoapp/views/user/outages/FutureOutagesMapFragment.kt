package com.example.canorecoapp.views.user.outages

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.canorecoapp.R
import com.example.canorecoapp.databinding.FragmentFutureOutagesMapBinding


class FutureOutagesMapFragment : Fragment() {
    private lateinit var binding : FragmentFutureOutagesMapBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentFutureOutagesMapBinding.inflate(layoutInflater)
        // Inflate the layout for this fragment
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

    }
}