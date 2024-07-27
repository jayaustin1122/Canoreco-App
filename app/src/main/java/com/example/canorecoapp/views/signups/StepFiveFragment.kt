package com.example.canorecoapp.views.signups

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import com.example.canorecoapp.R
import com.example.canorecoapp.databinding.FragmentStepFiveBinding
import com.example.canorecoapp.viewmodels.SignUpViewModel
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import java.util.concurrent.TimeUnit


class StepFiveFragment : Fragment() {
    private lateinit var binding : FragmentStepFiveBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var storedVerificationId: String
    private lateinit var resendToken: PhoneAuthProvider.ForceResendingToken
    private lateinit var viewModel: SignUpViewModel
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentStepFiveBinding.inflate(layoutInflater)
        // Inflate the layout for this fragment
        return binding.root
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(requireActivity())[SignUpViewModel::class.java]
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        auth = FirebaseAuth.getInstance()
        val verifyButton: Button = binding.verifyOTPBtn
        val otpEditText1: EditText = binding.otpEditText1
        val otpEditText2: EditText = binding.otpEditText2
        val otpEditText3: EditText = binding.otpEditText3
        val otpEditText4: EditText = binding.otpEditText4
        val otpEditText5: EditText = binding.otpEditText5
        val otpEditText6: EditText = binding.otpEditText6
        val progressBar: ProgressBar = binding.otpProgressBar
        verifyButton.setOnClickListener {
            val otp = otpEditText1.text.toString() +
                    otpEditText2.text.toString() +
                    otpEditText3.text.toString() +
                    otpEditText4.text.toString() +
                    otpEditText5.text.toString() +
                    otpEditText6.text.toString()

            if (otp.length == 6) {
                progressBar.visibility = View.VISIBLE
                verifyPhoneNumberWithCode(storedVerificationId, otp)
                viewModel.phone = otp.toString()
            } else {
                Toast.makeText(requireContext(), "Please enter a valid OTP", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
        }
        // Optionally, handle the resend OTP functionality
        val resendTextView = binding.resendTextView
        resendTextView.setOnClickListener {
            resendVerificationCode(viewModel.phone)
        }
    }
    private fun verifyPhoneNumberWithCode(verificationId: String, code: String) {
        val credential = PhoneAuthProvider.getCredential(verificationId, code)
        signInWithPhoneAuthCredential(credential)
    }

    private fun signInWithPhoneAuthCredential(credential: PhoneAuthCredential) {
        auth.signInWithCredential(credential)
            .addOnCompleteListener(requireActivity()) { task ->
                val progressBar: ProgressBar = view?.findViewById(R.id.otpProgressBar) ?: return@addOnCompleteListener
                progressBar.visibility = View.GONE
                if (task.isSuccessful) {
                    // Sign in success
                    Toast.makeText(requireContext(), "Verification successful", Toast.LENGTH_SHORT).show()
                    // Proceed to create the account or navigate to the next fragment

                } else {
                    // Sign in failed
                    Toast.makeText(requireContext(), "Verification failed", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun resendVerificationCode(phoneNumber: String) {
        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(phoneNumber)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(requireActivity())
            .setCallbacks(callbacks)
            .setForceResendingToken(resendToken)
            .build()
        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    private val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
        override fun onVerificationCompleted(credential: PhoneAuthCredential) {
            signInWithPhoneAuthCredential(credential)
        }

        override fun onVerificationFailed(e: FirebaseException) {
            Toast.makeText(requireContext(), "Verification failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }

        override fun onCodeSent(
            verificationId: String,
            token: PhoneAuthProvider.ForceResendingToken
        ) {
            storedVerificationId = verificationId
            resendToken = token
        }
    }


}