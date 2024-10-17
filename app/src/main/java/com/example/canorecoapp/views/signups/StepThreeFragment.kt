package com.example.canorecoapp.views.signups

import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.canorecoapp.databinding.FragmentStepThreeBinding
import com.example.canorecoapp.viewmodels.SignUpViewModel

class StepThreeFragment : Fragment() {
    private lateinit var binding: FragmentStepThreeBinding
    private lateinit var viewModel: SignUpViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentStepThreeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(requireActivity()).get(SignUpViewModel::class.java)

        binding.etEmailSignUp.addTextChangedListener {
            viewModel.email = it.toString()
        }

        // Enhanced password strength logic
        binding.etPasswordSignUp.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val password = s.toString()

                if (password.isEmpty()) {
                    binding.passwordStrengthTextView.visibility = View.GONE
                } else {
                    binding.passwordStrengthTextView.visibility = View.VISIBLE
                    val hasUppercase = password.any { it.isUpperCase() }
                    val hasLowercase = password.any { it.isLowerCase() }
                    val hasDigit = password.any { it.isDigit() }
                    val hasSpecialChar = password.any { !it.isLetterOrDigit() }

                    when {
                        password.length < 6 -> {
                            binding.passwordStrengthTextView.text = "Weak"
                            binding.passwordStrengthTextView.setTextColor(Color.RED)
                        }
                        hasUppercase && hasLowercase && hasDigit && hasSpecialChar -> {
                            binding.passwordStrengthTextView.text = "Strong"
                            binding.passwordStrengthTextView.setTextColor(Color.BLUE)
                        }
                        (hasUppercase || hasLowercase) && hasDigit -> {
                            binding.passwordStrengthTextView.text = "Medium"
                            binding.passwordStrengthTextView.setTextColor(Color.GREEN)
                        }
                        else -> {
                            binding.passwordStrengthTextView.text = "Weak"
                            binding.passwordStrengthTextView.setTextColor(Color.RED)
                        }
                    }
                    viewModel.password = password
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        // Password match logic
        binding.etConfirmPasswordSignUp.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val password = binding.etPasswordSignUp.text.toString()
                val confirmPassword = s.toString()

                if (password.isEmpty() && confirmPassword.isEmpty()) {
                    binding.passwordMatchTextView.visibility = View.GONE
                } else {
                    binding.passwordMatchTextView.visibility = View.VISIBLE
                    if (password == confirmPassword) {
                        if (password.isEmpty()) {
                            binding.passwordMatchTextView.text = ""
                        } else {
                            binding.passwordMatchTextView.text = "Passwords match"
                            binding.passwordMatchTextView.setTextColor(Color.BLUE)
                        }
                    } else {
                        binding.passwordMatchTextView.text = "Passwords do not match"
                        binding.passwordMatchTextView.setTextColor(Color.RED)
                    }
                    viewModel.confirmPass = confirmPassword
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }
}
