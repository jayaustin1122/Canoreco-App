package com.example.canorecoapp.views

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.animation.doOnEnd
import androidx.navigation.fragment.findNavController
import com.example.canorecoapp.R
import com.example.canorecoapp.databinding.FragmentLoginOrSignUpBinding


class LoginOrSignUpFragment : Fragment() {
    private lateinit var binding : FragmentLoginOrSignUpBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentLoginOrSignUpBinding.inflate(layoutInflater)
        // Inflate the layout for this fragment
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.signupButton.setOnClickListener {
            animateViewsAndNavigate()
        }
    }

    private fun animateViewsAndNavigate() {
        // Create the translation and scaleY animation for the bottom arc
        val arcTranslateY = ObjectAnimator.ofFloat(binding.bottomArc, "translationY", 0f, -binding.bottomArc.height.toFloat())
        val arcScaleY = ObjectAnimator.ofFloat(binding.bottomArc, "scaleY", 3f, 3f)

        // Create fade-out animation for the signup button
        val buttonFadeOut = ObjectAnimator.ofFloat(binding.signupButton, "alpha", 1f, 0f)

        // Combine animations
        val animatorSet = AnimatorSet().apply {
            playTogether(arcTranslateY, arcScaleY, buttonFadeOut)
            duration = 500 // Animation duration in milliseconds
        }

        // Start the animations
        animatorSet.start()
        animatorSet.doOnEnd {
            findNavController().navigate(R.id.signUpFragment)
        }
    }

}