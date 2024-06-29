package com.example.canorecoapp.views.signups

import android.app.ProgressDialog
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.viewpager2.widget.ViewPager2
import com.example.canorecoapp.R
import com.example.canorecoapp.adapter.SignUpAdapters
import com.example.canorecoapp.databinding.FragmentSignUpBinding
import com.example.canorecoapp.utils.DateTimeUtils.Companion.getCurrentDate
import com.example.canorecoapp.utils.DateTimeUtils.Companion.getCurrentTime
import com.example.canorecoapp.utils.FirebaseUtils
import com.example.canorecoapp.viewmodels.SignUpViewModel
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.storage.FirebaseStorage
import com.shuhart.stepview.StepView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.concurrent.TimeUnit

const val TOPIC = "/topics/myTopic2"
class SignUpFragment : Fragment() {
    private lateinit var binding: FragmentSignUpBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var firebaseUtils: FirebaseUtils
    private lateinit var viewPager: ViewPager2
    private lateinit var adapter: SignUpAdapters
    private lateinit var stepView: StepView
    private lateinit var viewModel: SignUpViewModel
    private lateinit var progressDialog : ProgressDialog
    private lateinit var storage : FirebaseStorage
    private lateinit var fireStore : FirebaseFirestore
    private lateinit var storedVerificationId: String
    private lateinit var resendToken: PhoneAuthProvider.ForceResendingToken
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
        storage = FirebaseStorage.getInstance()
        fireStore = FirebaseFirestore.getInstance()
        progressDialog = ProgressDialog(this.requireContext())
        progressDialog.setTitle("Please wait")
        progressDialog.setCanceledOnTouchOutside(false)
        auth = FirebaseAuth.getInstance()
        viewPager.adapter = adapter

        adapter.addFragment(StepOneFragment())
        adapter.addFragment(StepTwoFragment())
        adapter.addFragment(StepThreeFragment())
        adapter.addFragment(StepFourFragment())
        adapter.addFragment(StepFiveFragment())
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
                3 -> validateFragmentFour()


            }
        }

    }

    private fun sendPhoneNumberCode() {
        val phoneNumber = viewModel.phone
        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(phoneNumber) // Phone number to verify
            .setTimeout(60L, TimeUnit.SECONDS) // Timeout and unit
            .setActivity(this@SignUpFragment.requireActivity()) // Activity (for callback binding)
            .setCallbacks(callbacks)
            .build()
        PhoneAuthProvider.verifyPhoneNumber(options)
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
    private fun validateFragmentFour() {
        val phoneNUmber = viewModel.phone
        val accountNumber = viewModel.accountNumber
        val address = viewModel.address
        if (phoneNUmber.isEmpty()) {
            Toast.makeText(requireContext(), "Please Enter Contact Number or Valid Contact Number", Toast.LENGTH_SHORT).show()
            return
        } else if (accountNumber.isEmpty()) {
            Toast.makeText(requireContext(), "Please Enter your Account Number", Toast.LENGTH_SHORT).show()
            return
        } else if (address.isEmpty()) {
            Toast.makeText(requireContext(), "Please Enter Your Address", Toast.LENGTH_SHORT).show()
            return
        } else {
            sendPhoneNumberCode()

            progressDialog.dismiss()
            createUserAccount()
            progressDialog.setMessage("Creating Account...")
            progressDialog.show()
            nextItem()

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
    private fun createUserAccount() {
        progressDialog.setMessage("Creating Account...")
        progressDialog.show()
        val email = viewModel.email
        val password = viewModel.password
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val authResult = auth.createUserWithEmailAndPassword(email, password).await()

                // Get the FCM token
                val fcmToken = FirebaseMessaging.getInstance().token.await()

                withContext(Dispatchers.Main) {
                    uploadImage(fcmToken)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    progressDialog.dismiss()
                    Toast.makeText(
                        this@SignUpFragment.requireContext(),
                        "Failed Creating Account or ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }
    private fun uploadImage( token: String) {
        progressDialog.setMessage("Uploading Image...")
        progressDialog.show()

        val reference = storage.reference.child("profile")
            .child(token!!)
        viewModel.image?.let {
            reference.putFile(it).addOnCompleteListener {
                if (it.isSuccessful) {
                    reference.downloadUrl.addOnSuccessListener { image ->
                        uploadToFirebase(token, image.toString())
                    }
                } else {
                    progressDialog.dismiss()
                    Toast.makeText(
                        this@SignUpFragment.requireContext(),
                        "Error uploading image",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }
    private fun uploadToFirebase(token: String?, imageUrl: String) {
        progressDialog.setMessage("Saving Account...")
        progressDialog.show()
        val fullname = viewModel.fullname
        val email = viewModel.email
        val password = viewModel.password
        val month = viewModel.month
        val day = viewModel.day
        val year = viewModel.year
        val currentDate = getCurrentDate()
        val currentTime = getCurrentTime()
        val uid = auth.uid
        val timestamp = System.currentTimeMillis()


        val user: HashMap<String, Any?> = hashMapOf(
            "uid" to uid,
            "email" to email,
            "password" to password,
            "fullName" to fullname,
            "image" to imageUrl,
            "currentDate" to currentDate,
            "currentTime" to currentTime,
            "phone" to viewModel.phone,
            "userType" to "member",
            "access" to false,
            "token" to token,
            "dateOfBirth" to "$month-$day-$year",
            "timestamp" to timestamp,
            "address" to viewModel.address
        )
        val firestore = FirebaseFirestore.getInstance()
        try {
            firestore.collection("Users")
                .document(uid!!)
                .set(user)
                .addOnCompleteListener { task ->
                    progressDialog.dismiss()
                    if (task.isSuccessful) {
                        findNavController().apply {
                            popBackStack(R.id.signUpFragment, false)
                            navigate(R.id.signInFragment)
                        }
                        Toast.makeText(
                            this@SignUpFragment.requireContext(),
                            "Account Created",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        Toast.makeText(
                            this@SignUpFragment.requireContext(),
                            task.exception?.message ?: "Error creating account",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
        } catch (e: Exception) {
            progressDialog.dismiss()
            Toast.makeText(
                this.requireContext(),
                "Error uploading data: ${e.message}",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

}