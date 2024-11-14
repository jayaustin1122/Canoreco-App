package com.example.canorecoapp.views.signups

import android.app.Dialog
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.viewpager2.widget.ViewPager2
import cn.pedant.SweetAlert.SweetAlertDialog
import com.example.canorecoapp.R
import com.example.canorecoapp.adapter.SignUpAdapters
import com.example.canorecoapp.databinding.DialogLoginBinding
import com.example.canorecoapp.databinding.DialogReviewBinding
import com.example.canorecoapp.databinding.FragmentSignUpBinding
import com.example.canorecoapp.utils.DialogUtils
import com.example.canorecoapp.utils.FirebaseUtils
import com.example.canorecoapp.viewmodels.SignUpViewModel
import com.example.canorecoapp.views.user.news.NewsDetailsFragment
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.FirebaseException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
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
    private lateinit var loadingDialog: SweetAlertDialog
    private lateinit var storage: FirebaseStorage
    private lateinit var fireStore: FirebaseFirestore
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
        auth = FirebaseAuth.getInstance()
        viewPager.adapter = adapter
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    backItem()
                }
            })
        adapter.addFragment(StepOneFragment())
        adapter.addFragment(StepTwoFragment())
        adapter.addFragment(StepThreeFragment())
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
            }
        }
        binding.backButton.setOnClickListener {
            DialogUtils.showWarningMessage(
                requireActivity(),
                "Warning",
                "Are you sure you want to exit? Changes will not be saved."
            ) { sweetAlertDialog ->
                sweetAlertDialog.dismissWithAnimation()
                viewModel.firstName = ""
                viewModel.lastName = ""
                viewModel.month = ""
                viewModel.day = ""
                viewModel.year = ""
                viewModel.phone = ""
                viewModel.barangay = ""
                viewModel.address = ""
                viewModel.email = ""
                viewModel.password = ""
                viewModel.confirmPass = ""
                findNavController().navigate(R.id.signInFragment)
            }
        }
    }

    private fun validateOtpFragment() {
        val otp = viewModel.otp

        if (viewModel.skipOtpVerification) {
            nextItem()
            return
        }

        when {
            otp.isEmpty() -> {
                Snackbar.make(requireView(), "Please verify the OTP", Snackbar.LENGTH_SHORT).show()
                return
            }
            otp.length != 6 -> {
                Snackbar.make(requireView(), "Please complete the OTP", Snackbar.LENGTH_SHORT).show()
                return
            }
            !viewModel.smsIsVerified -> {
                Snackbar.make(requireView(), "Please verify the OTP", Snackbar.LENGTH_SHORT).show()
                return
            }
            else -> {
                nextItem()
            }
        }
    }

    fun backItem() {
        val currentItem = viewPager.currentItem
        val nextItem = currentItem - 1
        if (nextItem >= 0) {
            viewPager.currentItem = nextItem
        }
    }

    fun nextItem() {
        val currentItem = viewPager.currentItem
        val nextItem = currentItem + 1
        if (nextItem < adapter.itemCount) {
            viewPager.currentItem = nextItem
        }
    }

    fun validateFragmentOne() {
        val firstName = viewModel.firstName
        val lastName = viewModel.lastName
        val month = viewModel.month
        val day = viewModel.day
        val year = viewModel.year
        val selectedImageUri = viewModel.image
        if (firstName.isEmpty() && lastName.isEmpty()) {
            Toast.makeText(
                requireContext(),
                "Please enter your First Name And Last Name",
                Toast.LENGTH_SHORT
            ).show()
            return
        } else if (month.isEmpty() || day.isEmpty() || year.isEmpty()) {
            Toast.makeText(requireContext(), "Please Select Date of Birth", Toast.LENGTH_SHORT)
                .show()
            return
        } else if (selectedImageUri == null) {
            Toast.makeText(requireContext(), "Please upload a profile picture", Toast.LENGTH_SHORT)
                .show()
            return
        } else {
            val currentYear = Calendar.getInstance().get(Calendar.YEAR)
            val birthYear = year.toInt()
            val userAge = currentYear - birthYear

            if (userAge < 13) {
                Toast.makeText(
                    requireContext(),
                    "You must be at least 13 years old to continue",
                    Toast.LENGTH_SHORT
                ).show()
                return
            } else {
                nextItem()
            }
        }
    }

    fun validateFragmentTwo() {
        var phone = viewModel.phone.trim() // Ensure no leading/trailing spaces
        val barangay = viewModel.barangay
        val municipality = viewModel.address

        if (phone.isEmpty()) {
            Toast.makeText(
                requireContext(),
                "Please add your Contact Number to continue",
                Toast.LENGTH_SHORT
            ).show()
            return
        }
        // Handle phone verification and conversion to +639 format
        if (phone.startsWith("09")) {
            viewModel.phone = phone
        }
        // Check for barangay and municipality
        if (barangay.isEmpty()) {
            Toast.makeText(
                requireContext(),
                "Please add your Barangay to continue",
                Toast.LENGTH_SHORT
            ).show()
            return
        } else if (municipality.isEmpty()) {
            Toast.makeText(
                requireContext(),
                "Please add your Municipality to continue",
                Toast.LENGTH_SHORT
            ).show()
            return
        } else {
            nextItem()
        }
    }

    fun validateFragmentThree() {
        val email = viewModel.email
        val password = viewModel.password
        val confirmPass = viewModel.confirmPass

        // Ensure the email is not empty
        if (email.isEmpty()) {
            Toast.makeText(requireContext(), "Email cannot be empty", Toast.LENGTH_SHORT).show()
            return
        }
        // Ensure the email follows proper email format and ends with "@gmail.com"
        else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email)
                .matches() || !email.endsWith("@gmail.com")
        ) {
            Toast.makeText(
                requireContext(),
                "Email must be a valid Gmail address",
                Toast.LENGTH_SHORT
            ).show()
            return
        }
        // Ensure the email does not contain ".con" typo
        else if (email.contains(".con")) {
            Toast.makeText(
                requireContext(),
                "Email cannot contain '.con', please enter a valid email",
                Toast.LENGTH_SHORT
            ).show()
            return
        }
        // Ensure password is not empty
        else if (password.isEmpty()) {
            Toast.makeText(requireContext(), "Password Not Match!", Toast.LENGTH_SHORT).show()
            return
        }
        // Ensure confirm password is not empty
        else if (confirmPass.isEmpty()) {
            Toast.makeText(requireContext(), "Please confirm your password", Toast.LENGTH_SHORT)
                .show()
            return
        }
        // Ensure passwords match
        else if (password != confirmPass) {
            Toast.makeText(requireContext(), "Passwords do not match", Toast.LENGTH_SHORT).show()
            return
        }
        // Proceed to create user account if all conditions are met
        else {
            createUserAccount()
        }
    }


    private fun createUserAccount() {
        loadingDialog = DialogUtils.showLoading(requireActivity())
        loadingDialog.show()
        val email = viewModel.email
        val password = viewModel.password
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val authResult = auth.createUserWithEmailAndPassword(email, password).await()

                val fcmToken = FirebaseMessaging.getInstance().token.await()

                withContext(Dispatchers.Main) {
                    uploadImage(fcmToken)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    loadingDialog.dismiss()
                    Toast.makeText(
                        this@SignUpFragment.requireContext(),
                        "Failed Creating Account or ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun uploadImage(token: String) {

        val reference = storage.reference.child("profile")
            .child(token!!)
        viewModel.image?.let {
            reference.putFile(it).addOnCompleteListener {
                if (it.isSuccessful) {
                    reference.downloadUrl.addOnSuccessListener { image ->
                        uploadToFirebase(token, image.toString())
                    }
                } else {
                    loadingDialog.dismiss()
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

        val firstName = viewModel.firstName
        val lastName = viewModel.lastName
        val email = viewModel.email
        val password = viewModel.password
        val month = viewModel.month
        val day = viewModel.day
        val year = viewModel.year
        val uid = auth.uid
        val timestamp = System.currentTimeMillis() / 1000


        val user: HashMap<String, Any?> = hashMapOf(
            "uid" to uid,
            "email" to email,
            "password" to password,
            "firstName" to firstName,
            "lastName" to lastName,
            "image" to imageUrl,
            "phone" to viewModel.phone,
            "userType" to "member",
            "access" to true,
            "token" to token,
            "dateOfBirth" to "$month-$day-$year",
            "timestamp" to timestamp,
            "barangay" to viewModel.barangay,
            "municipality" to viewModel.address,
        )
        val firestore = FirebaseFirestore.getInstance()
        try {
            firestore.collection("users")
                .document(uid!!)
                .set(user)
                .addOnCompleteListener { task ->

                    loadingDialog.dismiss()
                    DialogUtils.showSuccessMessage(
                        requireActivity(),
                        "Success",
                        "Account created successfully"
                    ).show()

                    showReviewDialog()
                    if (task.isSuccessful) {
                        findNavController().apply {
                            popBackStack(R.id.signUpFragment, false)
                            navigate(R.id.signInFragment)
                        }
                        auth.signOut()
                    } else {
                        Toast.makeText(
                            this@SignUpFragment.requireContext(),
                            task.exception?.message ?: "Error creating account",
                            Toast.LENGTH_SHORT
                        ).show()
                        loadingDialog.dismiss()
                    }
                }
        } catch (e: Exception) {
            loadingDialog.dismiss()
            Toast.makeText(
                this.requireContext(),
                "Error uploading data: ${e.message}",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
    private fun showReviewDialog() {
        val dialogBinding = DialogLoginBinding.inflate(layoutInflater)
        val dialog = Dialog(requireContext())
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialogBinding.btnDismiss.setOnClickListener {
            dialog.dismiss()
        }
        dialog.setContentView(dialogBinding.root)
        dialog.show()
    }

}