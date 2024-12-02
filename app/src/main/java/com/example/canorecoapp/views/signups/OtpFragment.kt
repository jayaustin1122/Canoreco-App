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
import com.google.firebase.firestore.FirebaseFirestore
import java.util.concurrent.TimeUnit
import kotlin.random.Random

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
        auth = FirebaseAuth.getInstance()
        addTextChangeListeners()
        startResendTimer()

        binding.phoneNumberTextView.setText(viewModel.phone)
        binding.resendTextView.setOnClickListener {
            Toast.makeText(requireContext(), "Sending new OTP...", Toast.LENGTH_SHORT).show()
            uploadInFireStore(viewModel.phone)
        }
    }
    private fun generateRandomOtp(): String {
        val randomNumber = Random.nextInt(100000, 999999)
        return randomNumber.toString()
    }

    private fun uploadInFireStore(phone: String) {
        val otpCode = generateRandomOtp()
        val user: HashMap<String, Any?> = hashMapOf(
            "status" to true,
            "phone" to phone,
            "code" to otpCode
        )

        val firestore = FirebaseFirestore.getInstance()
        try {
            firestore.collection("sms")
                .document("otp")
                .set(user)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Log.d("UploadInFireStore", "OTP uploaded successfully: $otpCode")
                    } else {
                        Log.e("UploadInFireStore", "Failed to upload OTP to Firestore.")
                    }
                }
        } catch (e: Exception) {
            Log.e("UploadInFireStore", "Error uploading OTP to Firestore: ${e.message}")
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

            // Move focus to next field if current field is filled
            if (text.length == 1 && nextView != null) {
                nextView.requestFocus()
            } else if (text.isEmpty() && currentView != binding.otpEditText1) {
                currentView.requestFocus()
            }

            // Check if all OTP fields are filled (6 digits entered)
            if (areAllOtpFieldsFilled()) {
                val typedOTP = getEnteredOtp()
                viewModel.otp = typedOTP
                Log.d("OtpFragment", "OTP Set Automatically: $typedOTP") // Log the OTP when all 6 digits are entered
            }
        }
    }

    private fun areAllOtpFieldsFilled(): Boolean {
        return binding.otpEditText1.text.isNotEmpty() &&
                binding.otpEditText2.text.isNotEmpty() &&
                binding.otpEditText3.text.isNotEmpty() &&
                binding.otpEditText4.text.isNotEmpty() &&
                binding.otpEditText5.text.isNotEmpty() &&
                binding.otpEditText6.text.isNotEmpty()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        countdownTimer?.cancel()
    }
}
