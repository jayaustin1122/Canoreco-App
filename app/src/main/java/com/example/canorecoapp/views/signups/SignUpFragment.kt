package com.example.canorecoapp.views.signups

import android.app.Dialog
import android.content.ContentValues.TAG
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
import com.example.canorecoapp.databinding.FragmentSignUpBinding
import com.example.canorecoapp.utils.DialogUtils
import com.example.canorecoapp.utils.FirebaseUtils
import com.example.canorecoapp.utils.ProgressDialogUtils
import com.example.canorecoapp.viewmodels.SignUpViewModel
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
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
import java.util.concurrent.TimeUnit
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
                viewModel.municipality = ""
                viewModel.email = ""
                viewModel.password = ""
                viewModel.confirmPass = ""
                findNavController().navigate(R.id.signInFragment)
            }
        }
    }
    private fun validateFragmentOne() {
        // Check if any of the fields are empty
        if (viewModel.firstName.isEmpty() ||
            viewModel.lastName.isEmpty() ||
            viewModel.barangay.isEmpty() ||
            viewModel.street.isEmpty() ||
            viewModel.municipality.isEmpty()) {

            // Show an error message or toast
            Toast.makeText(context, "Please fill in all fields", Toast.LENGTH_SHORT).show()
        } else {
            Log.d("logging", "Account Number3: ${viewModel.meterNumber}")

            // Proceed to the next item if all fields are filled
            nextItem()
        }
    }

    private fun validateFragmentTwo() {
        val email = viewModel.email
        val password = viewModel.password
        val confirmPass = viewModel.confirmPass
        val phone = viewModel.phone



        // Ensure password is not empty
        if (password.isEmpty()) {
            Toast.makeText(requireContext(), "Password cannot be empty!", Toast.LENGTH_SHORT).show()
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
        // Ensure phone is not empty
        else if (phone.isEmpty()) {
            Toast.makeText(requireContext(), "Phone number cannot be empty!", Toast.LENGTH_SHORT).show()
            return
        }
        else  {
         uploadInFireStore(viewModel.phone)

        }

    }

    private fun createUserAccount() {

        // Generate a random number and append it to the base email
        val randomNumber = (1000..9999).random() // You can adjust the range as needed
        val email = "canoreco${randomNumber}@gmail.com"
        val password = viewModel.password

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Create user with email and password
                val authResult = auth.createUserWithEmailAndPassword(email, password).await()
                val user = auth.currentUser
                val fcmToken = FirebaseMessaging.getInstance().token.await()
                viewModel.email = email
                // Proceed with uploading user data to Firebase
                withContext(Dispatchers.Main) {
                    uploadToFirebase()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    loadingDialog.dismiss()
                    Toast.makeText(
                        this@SignUpFragment.requireContext(),
                        "Failed Creating Account: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
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
                        nextItem()
                        Log.d("UploadInFireStore", "OTP uploaded successfully: $otpCode")
                    } else {
                        Log.e("UploadInFireStore", "Failed to upload OTP to Firestore.")
                    }
                }
        } catch (e: Exception) {
            Log.e("UploadInFireStore", "Error uploading OTP to Firestore: ${e.message}")
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
        val smsRef = FirebaseFirestore.getInstance().collection("sms").document("otp")
        Log.d("VerifyOTP", "Checking OTP in Firestore...")

        smsRef.get()
            .addOnSuccessListener { snapshot ->
                if (snapshot != null && snapshot.exists()) {
                    Log.d("VerifyOTP", "Snapshot exists, checking data...")
                    val code = snapshot.getString("code") ?: ""
                    val phone = snapshot.getString("phone") ?: ""

                    if (typedOTP == code) {
                        Log.d("VerifyOTP", "OTP matched. Verifying...")

                        smsRef.update(
                            mapOf(
                                "status" to false,
                                "code" to "",
                                "phone" to ""
                            )
                        ).addOnSuccessListener {
                            Log.d("VerifyOTP", "OTP verified successfully, Firestore data cleared.")
                            loadingDialog.dismiss()
                            Toast.makeText(requireContext(), "OTP Verified Successfully", Toast.LENGTH_SHORT).show()
                            createUserAccount()

                        }.addOnFailureListener { e ->
                            loadingDialog.dismiss()
                            Log.e("VerifyOTP", "Failed to update OTP data in Firestore.", e)
                            Toast.makeText(requireContext(), "Failed to update OTP data", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        loadingDialog.dismiss()
                        Log.d("VerifyOTP", "OTP does not match or status is false.")
                        Toast.makeText(requireContext(), "Invalid OTP or OTP already used", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    loadingDialog.dismiss()
                    Log.d("VerifyOTP", "No OTP data found in Firestore.")
                    Toast.makeText(requireContext(), "No OTP data found", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                loadingDialog.dismiss()
                Log.e("VerifyOTP", "Failed to retrieve document from Firestore.", e)
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

    private fun nextItem() {
        val currentItem = viewPager.currentItem
        val nextItem = currentItem + 1
        if (nextItem < adapter.itemCount) {
            viewPager.currentItem = nextItem
        }
    }

    private fun generateRandomOtp(): String {
        val randomNumber = Random.nextInt(100000, 999999)
        return randomNumber.toString()
    }

    private fun uploadToFirebase() {
        val firstName = viewModel.firstName
        val lastName = viewModel.lastName
        val password = viewModel.password
        val timestamp = System.currentTimeMillis() / 1000

        // Check if the user is authenticated
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid == null) {
            loadingDialog.dismiss()
            // Handle the case when the user is not authenticated
            Log.e("UploadToFirebase", "User is not authenticated. Cannot upload data.")
            Toast.makeText(requireContext(), "Error: User not authenticated", Toast.LENGTH_SHORT).show()
            return
        }

        val user: HashMap<String, Any?> = hashMapOf(
            "uid" to uid, // Use the authenticated user's UID instead of timestamp
            "email" to "",
            "authEmail" to  viewModel.email,
            "password" to password,
            "firstName" to firstName,
            "lastName" to lastName,
            "phone" to viewModel.phone,
            "userType" to "member",
            "access" to true,
            "timestamp" to timestamp,
            "barangay" to viewModel.barangay,
            "municipality" to viewModel.municipality,
        )

        val firestore = FirebaseFirestore.getInstance()

        Log.d("UploadToFirebase", "Preparing to upload user data: $user")

        try {
            firestore.collection("users")
                .document(uid) // Use the correct UID here
                .set(user)
                .addOnCompleteListener { task ->
                    loadingDialog.dismiss()

                    if (task.isSuccessful) {
                        Log.d("UploadToFirebase", "User data uploaded successfully for UID: $uid")

                        // Display success message
                        DialogUtils.showSuccessMessage(
                            requireActivity(),
                            "Success",
                            "Account created successfully"
                        ).show()

                        // Verify the phone number and show the review dialog
                        verifyInFirebase(viewModel.phone)
                        showReviewDialog()
                        updateAccountStatusToLinked(viewModel.meterNumber)
                        Log.d("sa", "Account ${viewModel.meterNumber} status successfully updated to 'linked'")

                        // Navigate to sign-in and sign out the current user
                        findNavController().apply {
                            popBackStack(R.id.signUpFragment, false)
                            navigate(R.id.signInFragment)
                        }
                        FirebaseAuth.getInstance().signOut()

                    } else {
                        Log.e("UploadToFirebase", "Error during upload: ${task.exception?.message}")
                        Toast.makeText(
                            this@SignUpFragment.requireContext(),
                            task.exception?.message ?: "Error creating account",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
                .addOnFailureListener { exception ->
                    // Log failure and show toast message
                    Log.e("UploadToFirebase", "Upload failed: ${exception.message}", exception)
                    Toast.makeText(
                        this.requireContext(),
                        "Error uploading data: ${exception.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                    loadingDialog.dismiss()
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

    private fun updateAccountStatusToLinked(accountNumber: String) {
        val accountRef = firebaseUtils.fireStore.collection("accounts").document(accountNumber)

        try {
            accountRef.update("status", "linked")
                .addOnSuccessListener {
                    Log.d("logging", "Account Number4: ${viewModel.meterNumber}")

                    // Log successful update
                    Log.d("sa", "Account $accountNumber status successfully updated to 'linked'")
                    Toast.makeText(requireContext(), "Account successfully linked", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    // Log the error
                    Log.e("sa", "Error updating account $accountNumber status to 'linked'", e)
                    Toast.makeText(requireContext(), "Error updating account status", Toast.LENGTH_SHORT).show()
                }
        } catch (e: Exception) {
            // Log the exception
            Log.e("sa", "Exception caught while updating account status", e)
            Toast.makeText(requireContext(), "An error occurred while updating account status", Toast.LENGTH_SHORT).show()
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