package com.example.canorecoapp.views.linemen

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentManager
import com.example.canorecoapp.databinding.FragmentLineMenHolderBinding
import com.google.firebase.auth.FirebaseAuth

class LineMenHolderFragment : Fragment() {
    private lateinit var binding : FragmentLineMenHolderBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var fragmentManager: FragmentManager
    private var isUserInfoLoaded = false
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentLineMenHolderBinding.inflate(layoutInflater)
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

    }
}