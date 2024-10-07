package com.example.canorecoapp.views.onboardingscreens

import android.app.ProgressDialog
import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import cn.pedant.SweetAlert.SweetAlertDialog
import com.example.canorecoapp.R
import com.example.canorecoapp.databinding.FragmentOnBoardingThreeBinding
import com.example.canorecoapp.utils.DialogUtils


class OnBoardingThreeFragment : Fragment() {
    private lateinit var binding : FragmentOnBoardingThreeBinding
    private lateinit var loadingDialog: SweetAlertDialog
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentOnBoardingThreeBinding.inflate(layoutInflater)
        // Inflate the layout for this fragment
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.buttonNext.setOnClickListener {
            onBoardingFinish()
        }
    }
    private fun onBoardingFinish(){
        loadingDialog = DialogUtils.showLoading(requireActivity())
        loadingDialog.show()
        view?.postDelayed({
            val sharedPref = requireActivity().getSharedPreferences("onBoarding", Context.MODE_PRIVATE)
            val editor = sharedPref.edit()
            editor.putBoolean("Finished", true)
            editor.apply()
            findNavController().navigate(R.id.signInFragment)
            loadingDialog.dismiss()
        }, 2000)
    }

}