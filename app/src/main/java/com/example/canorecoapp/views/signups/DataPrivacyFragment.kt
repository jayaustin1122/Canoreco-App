package com.example.canorecoapp.views.signups

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.canorecoapp.R
import com.example.canorecoapp.databinding.FragmentDataPrivacyBinding
import com.google.android.material.snackbar.Snackbar


class DataPrivacyFragment : Fragment() {
    private lateinit var binding : FragmentDataPrivacyBinding
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentDataPrivacyBinding.inflate(layoutInflater)
        // Inflate the layout for this fragment
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.apply {
            btnSave.setOnClickListener {
                if (checkBoxAllowData.isChecked) {
                    findNavController().navigate(R.id.signUpFragment)
                } else {
                    Snackbar.make(view, "Please accept the data privacy policy to continue", Snackbar.LENGTH_LONG).show()
                }
            }
            backButton.setOnClickListener {
                findNavController().navigateUp()
            }
        }
    }

}