package com.example.canorecoapp.views.signups

import android.os.Bundle
import android.util.Log
import android.util.Patterns
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import androidx.viewpager2.widget.ViewPager2
import com.example.canorecoapp.adapter.SignUpAdapters
import com.example.canorecoapp.databinding.FragmentSignUpBinding
import com.example.canorecoapp.utils.FirebaseUtils
import com.example.canorecoapp.viewmodels.SignUpViewModel
import com.shuhart.stepview.StepView
import java.util.Calendar


class SignUpFragment : Fragment() {
    private lateinit var binding: FragmentSignUpBinding
    private lateinit var firebaseUtils: FirebaseUtils
    private lateinit var viewPager: ViewPager2
    private lateinit var adapter: SignUpAdapters
    private lateinit var stepView: StepView
    private lateinit var viewModel: SignUpViewModel
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentSignUpBinding.inflate(layoutInflater)
        viewPager = binding.viewpagersignup
        stepView = binding.stepView
        return binding.root
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(requireActivity()).get(SignUpViewModel::class.java)
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        firebaseUtils = FirebaseUtils()
        firebaseUtils.initialize(requireContext())
        adapter = SignUpAdapters(requireActivity())
        viewPager.adapter = adapter

        adapter.addFragment(StepOneFragment())
        adapter.addFragment(StepTwoFragment())
        adapter.addFragment(StepThreeFragment())
        adapter.addFragment(StepFourFragment())
        stepView.go(0, true)
        viewPager.isUserInputEnabled = false
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                stepView.go(position, true)
            }
        })
        binding.btnContinue.setOnClickListener {
            when (viewPager.currentItem) {
                0 -> validateFragmentOne()
                1 -> validateFragmentTwo()
                2 -> validateFragmentThree()
                //3 -> validateFragmentFour()
            }
        }

    }
    fun nextItem(){
        val currentItem = viewPager.currentItem
        val nextItem = currentItem + 1
        if (nextItem < adapter.itemCount) {
            viewPager.currentItem = nextItem

        }
    }
    fun validateFragmentOne(){
        val fullname = viewModel.fullname
        val email = viewModel.email
        val password = viewModel.password
        if (fullname.isEmpty()) {
            Toast.makeText(requireContext(), "Please enter your fullname", Toast.LENGTH_SHORT).show()
            return
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(requireContext(), "Please enter a valid email address", Toast.LENGTH_SHORT).show()
            return
        } else if (password.length < 8) {
            Toast.makeText(requireContext(), "Password must be at least 8 characters long", Toast.LENGTH_SHORT).show()
            return
        } else {
            nextItem()
        }
    }
    fun validateFragmentTwo() {
        val selectedImageUri = viewModel.image
        if (selectedImageUri == null) {
            Toast.makeText(requireContext(), "Please upload a profile picture", Toast.LENGTH_SHORT).show()
            return
        } else {
            nextItem()
            Log.d("SignUpFragment", "validateFragmentTwo: selectedImageUri is not null")
        }
    }
    fun validateFragmentThree() {
        val month = viewModel.month
        val day = viewModel.day
        val year = viewModel.year
        if (month.isEmpty() || day.isEmpty() || year.isEmpty()) {
            Toast.makeText(requireContext(), "Please Select Date of Birth", Toast.LENGTH_SHORT).show()
            return
        } else {
            val currentYear = Calendar.getInstance().get(Calendar.YEAR)
            val birthYear = year.toInt()
            val userAge = currentYear - birthYear

            if (userAge < 13) {
                Toast.makeText(requireContext(), "You must be at least 13 years old to continue", Toast.LENGTH_SHORT).show()
                return
            } else {
                nextItem()
            }
        }
    }
}