package com.example.canorecoapp.views.onboardingscreens

import android.app.ProgressDialog
import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import androidx.viewpager2.widget.ViewPager2
import com.example.canorecoapp.R
import com.example.canorecoapp.databinding.FragmentOnBoardingTworagmentBinding


class OnBoardingTworagment : Fragment() {
    private lateinit var binding : FragmentOnBoardingTworagmentBinding
    private lateinit var progressDialog : ProgressDialog
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentOnBoardingTworagmentBinding.inflate(layoutInflater)
        // Inflate the layout for this fragment
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        progressDialog = ProgressDialog(this.requireContext())
        progressDialog.setTitle("Please wait")
        progressDialog.setCanceledOnTouchOutside(false)
        val viewPager  = activity?.findViewById<ViewPager2>(R.id.viewPager)
        binding.buttonNext.setOnClickListener {
            viewPager?.currentItem = 2
        }
        binding.buttonSkip.setOnClickListener {
            findNavController().navigate(R.id.signInFragment)
            onBoardingFinish()
        }

    }
    private fun onBoardingFinish(){
        progressDialog.setMessage("Processing...")
        progressDialog.show()
        view?.postDelayed({
            val sharedPref = requireActivity().getSharedPreferences("onBoarding", Context.MODE_PRIVATE)
            val editor = sharedPref.edit()
            editor.putBoolean("Finished", true)
            editor.apply()

            progressDialog.dismiss()
        }, 2000)
    }

}