package com.example.canorecoapp.views.signups

import android.os.Bundle
import android.os.CountDownTimer
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.canorecoapp.databinding.FragmentOtpBinding
import com.example.canorecoapp.viewmodels.SignUpViewModel
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.FirebaseException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import java.util.concurrent.TimeUnit

class OtpFragment : Fragment() {
    private lateinit var auth: FirebaseAuth
    private lateinit var binding: FragmentOtpBinding
    private lateinit var viewModel: SignUpViewModel
    private var countdownTimer: CountDownTimer? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentOtpBinding.inflate(layoutInflater)
        viewModel = ViewModelProvider(requireActivity()).get(SignUpViewModel::class.java)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.otpProgressBar.visibility = View.INVISIBLE
        auth = FirebaseAuth.getInstance()

        addTextChangeListeners()
        startResendTimer()

        binding.resendTextView.setOnClickListener {
            if (viewModel.token != null) {
                resendVerificationCode(viewModel.phone, viewModel.token!!)
            }
            startResendTimer()
            Toast.makeText(requireContext(), "Sending new OTP...", Toast.LENGTH_SHORT).show()

        }

        binding.verifyOTPBtn.setOnClickListener {
            val typedOTP = getEnteredOtp()
            if (typedOTP.length == 6) {
                val verificationId = viewModel.verificationId
                if (verificationId != null && verificationId.isNotEmpty()) {
                    val credential = PhoneAuthProvider.getCredential(verificationId, typedOTP)
                    binding.otpProgressBar.visibility = View.VISIBLE
                    signInWithPhoneAuthCredential(credential)
                    viewModel.otp = typedOTP
                } else {
                    Toast.makeText(requireContext(), "Verification ID is missing. Please resend the OTP.", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(requireContext(), "Please Enter Correct OTP", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun getEnteredOtp(): String {
        return (binding.otpEditText1.text.toString() +
                binding.otpEditText2.text.toString() +
                binding.otpEditText3.text.toString() +
                binding.otpEditText4.text.toString() +
                binding.otpEditText5.text.toString() +
                binding.otpEditText6.text.toString())
    }

    private fun startResendTimer() {

        binding.resendTextView.isEnabled = false

        // Start a 1-minute countdown
        countdownTimer = object : CountDownTimer(60000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                // Update the resendTextView with the countdown time
                val secondsRemaining = millisUntilFinished / 1000
                binding.resendTextView.text = "Resend OTP in $secondsRemaining seconds"
            }

            override fun onFinish() {
                // Re-enable the resend button when the timer finishes
                binding.resendTextView.visibility = View.VISIBLE
                binding.resendTextView.isEnabled = true
                binding.resendTextView.text = "Resend OTP"
            }
        }.start()
    }

    private fun resendVerificationCode(phone: String, token: PhoneAuthProvider.ForceResendingToken) {
        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(phone)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(requireActivity())
            .setCallbacks(callbacks)
            .setForceResendingToken(token)
            .build()
        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    private val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
        override fun onVerificationCompleted(credential: PhoneAuthCredential) {
            signInWithPhoneAuthCredential(credential)
        }

        override fun onVerificationFailed(e: FirebaseException) {
            Log.d("OtpFragment", "onVerificationFailed: ${e.message}")
            binding.otpProgressBar.visibility = View.INVISIBLE
            if (e is FirebaseAuthInvalidCredentialsException) {
                Toast.makeText(requireContext(), "Invalid Request", Toast.LENGTH_SHORT).show()
            } else if (e is FirebaseTooManyRequestsException) {
                Toast.makeText(requireContext(), "SMS Quota Exceeded", Toast.LENGTH_SHORT).show()
            }
        }

        override fun onCodeSent(verificationId: String, token: PhoneAuthProvider.ForceResendingToken) {
            viewModel.verificationId = verificationId
            viewModel.token = token
            Log.d("OtpFragment", "Verification ID received: $verificationId")
        }
    }

    private fun signInWithPhoneAuthCredential(credential: PhoneAuthCredential) {
        auth.signInWithCredential(credential)
            .addOnCompleteListener(requireActivity()) { task ->
                binding.otpProgressBar.visibility = View.INVISIBLE
                if (task.isSuccessful) {
                    Snackbar.make(requireView(), "Success! Your Phone Number is Verified. Please Click Next to Continue", Snackbar.LENGTH_SHORT).show()
                    binding.verifyOTPBtn.visibility = View.INVISIBLE
                    viewModel.smsIsVerified = true
                } else {
                    Log.d("OtpFragment", "signInWithPhoneAuthCredential failed: ${task.exception?.message}")
                    if (task.exception is FirebaseAuthInvalidCredentialsException) {
                        Toast.makeText(requireContext(), "Invalid OTP", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(requireContext(), "Authentication failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
    }

    private fun addTextChangeListeners() {
        binding.otpEditText1.addTextChangedListener(OTPTextWatcher(binding.otpEditText1, binding.otpEditText2))
        binding.otpEditText2.addTextChangedListener(OTPTextWatcher(binding.otpEditText2, binding.otpEditText3))
        binding.otpEditText3.addTextChangedListener(OTPTextWatcher(binding.otpEditText3, binding.otpEditText4))
        binding.otpEditText4.addTextChangedListener(OTPTextWatcher(binding.otpEditText4, binding.otpEditText5))
        binding.otpEditText5.addTextChangedListener(OTPTextWatcher(binding.otpEditText5, binding.otpEditText6))
        binding.otpEditText6.addTextChangedListener(OTPTextWatcher(binding.otpEditText6, null))
    }

    inner class OTPTextWatcher(private val currentView: View, private val nextView: View?) : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        override fun afterTextChanged(s: Editable?) {
            val text = s.toString()
            if (text.length == 1 && nextView != null) {
                nextView.requestFocus()
            } else if (text.isEmpty() && currentView != binding.otpEditText1) {
                currentView.requestFocus()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        countdownTimer?.cancel() // Cancel the timer when the view is destroyed
    }
}
