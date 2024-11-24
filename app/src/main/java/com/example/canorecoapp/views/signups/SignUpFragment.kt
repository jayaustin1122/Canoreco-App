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
import com.example.bidnshare.notification.FirebaseServiceCanoreco.Companion.token
import com.example.canorecoapp.R
import com.example.canorecoapp.adapter.SignUpAdapters
import com.example.canorecoapp.databinding.DialogLoginBinding
import com.example.canorecoapp.databinding.FragmentSignUpBinding
import com.example.canorecoapp.utils.DialogUtils
import com.example.canorecoapp.utils.FirebaseUtils
import com.example.canorecoapp.viewmodels.SignUpViewModel
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
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
import kotlin.random.Random

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
        adapter.addFragment(OtpFragment())
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
                2 -> validateOtpFragment()
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

        if (otp.length == 6) {
            loadingDialog = DialogUtils.showLoading(requireActivity())
            loadingDialog.show()
            verifyInFirebase(otp)
        } else {
            Toast.makeText(requireContext(), "Please Enter Correct OTP", Toast.LENGTH_SHORT).show()
        }

    }
    private fun verifyInFirebase(typedOTP: String) {
        // Reference to Firestore
        val smsRef = FirebaseFirestore.getInstance().collection("sms").document("otp")

        Log.d("VerifyOTP", "Checking OTP in Firestore...") // Log when we start checking Firestore

        smsRef.get() // Use get() to fetch the document once
            .addOnSuccessListener { snapshot ->
                if (snapshot != null && snapshot.exists()) {
                    val status = snapshot.getBoolean("status") ?: false
                    val code = snapshot.getString("code") ?: ""
                    val phone = snapshot.getString("phone") ?: ""

                    // Log Firestore data
                    Log.d("VerifyOTP", "Firestore OTP data - Status: $status, Code: $code, Phone: $phone")

                    // Check if the typed OTP matches the stored OTP
                    if (status && typedOTP == code) {
                        Log.d("VerifyOTP", "OTP matched. Verifying...") // Log when OTP matches

                        // If the OTP matches, clear the data
                        smsRef.update(
                            mapOf(
                                "status" to false,
                                "code" to "",
                                "phone" to ""
                            )
                        ).addOnSuccessListener {
                            // After successfully updating Firestore, show a success message
                            Log.d("VerifyOTP", "OTP verified successfully, Firestore data cleared.") // Log success
                            Toast.makeText(requireContext(), "OTP Verified Successfully", Toast.LENGTH_SHORT).show()
                            uploadToFirebase()
                        }.addOnFailureListener {
                            // Handle failure case
                            Log.e("VerifyOTP", "Failed to update OTP data in Firestore.") // Log failure
                            Toast.makeText(requireContext(), "Failed to update OTP data", Toast.LENGTH_SHORT).show()
                        }
                    }

                } else {
                    Log.d("DeviceNotifFragment", "No OTP data found in Firestore.") // Log if no data found in Firestore
                    Toast.makeText(requireContext(), "No OTP data found", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Log.e("DeviceNotifFragment", "Failed to retrieve document", e) // Log failure when document retrieval fails
                Toast.makeText(requireContext(), "Failed to retrieve OTP data", Toast.LENGTH_SHORT).show()
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
        nextItem()
    }

    fun validateFragmentTwo() {
        val email = viewModel.email
        val password = viewModel.password
        val confirmPass = viewModel.confirmPass
        val phone = viewModel.phone

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
        else if (phone.isEmpty()) {
            Toast.makeText(requireContext(), "Phone Is Empty!", Toast.LENGTH_SHORT).show()
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
            uploadInFireStore(phone)
            nextItem()
        }
    }
    private fun uploadInFireStore(phone: String) {
        // Generate a random 6-digit OTP
        val otpCode = generateRandomOtp()

        val user: HashMap<String, Any?> = hashMapOf(
            "status" to true,
            "phone" to phone, // You can replace this with the actual user's phone number
            "code" to otpCode // Use the generated OTP code
        )

        val firestore = FirebaseFirestore.getInstance()
        try {
            firestore.collection("sms")
                .document("otp")
                .set(user)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        // Successfully uploaded data to Firestore
                        Log.d("UploadInFireStore", "OTP uploaded successfully: $otpCode")
                    } else {
                        // Failed to upload data to Firestore
                        Log.e("UploadInFireStore", "Failed to upload OTP to Firestore.")
                    }
                }
        } catch (e: Exception) {
            Log.e("UploadInFireStore", "Error uploading OTP to Firestore: ${e.message}")
        }
    }


    private fun generateRandomOtp(): String {
        val randomNumber = Random.nextInt(100000, 999999)
        return randomNumber.toString()
    }






    private fun uploadToFirebase() {

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
            "uid" to timestamp,
            "email" to email,
            "password" to password,
            "firstName" to firstName,
            "lastName" to lastName,
            "phone" to viewModel.phone,
            "userType" to "member",
            "access" to true,
            "timestamp" to timestamp,
            "barangay" to viewModel.barangay,
            "municipality" to viewModel.address,
        )
        val firestore = FirebaseFirestore.getInstance()

        try {
            firestore.collection("users")
                .document(timestamp.toString())
                .set(user)
                .addOnCompleteListener { task ->

                    loadingDialog.dismiss()
                    DialogUtils.showSuccessMessage(
                        requireActivity(),
                        "Success",
                        "Account created successfully"
                    ).show()
                    verifyInFirebase(viewModel.phone)
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
                        Log.e("UploadToFirebase", "Error during upload: ${task.exception?.message}")
                    }
                }
        } catch (e: Exception) {
            loadingDialog.dismiss()
            Toast.makeText(
                this.requireContext(),
                "Error uploading data: ${e.message}",
                Toast.LENGTH_SHORT
            ).show()

            // Log the error with detailed information for debugging
            Log.e("UploadToFirebase", "Error uploading user data: ${e.message}", e)
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