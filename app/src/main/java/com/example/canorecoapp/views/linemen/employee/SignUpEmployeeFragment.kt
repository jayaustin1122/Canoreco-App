package com.example.canorecoapp.views.linemen.employee

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.Dialog
import android.app.ProgressDialog
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.viewpager2.widget.ViewPager2
import cn.pedant.SweetAlert.SweetAlertDialog
import com.example.canorecoapp.R
import com.example.canorecoapp.adapter.SignUpAdapters
import com.example.canorecoapp.databinding.DialogLoginBinding
import com.example.canorecoapp.databinding.FragmentSignUpEmployeeBinding
import com.example.canorecoapp.utils.DialogUtils
import com.example.canorecoapp.utils.FirebaseUtils
import com.example.canorecoapp.viewmodels.SignUpViewModel
import com.example.canorecoapp.views.signups.OtpFragment
import com.example.canorecoapp.views.signups.StepTwoFragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.storage.FirebaseStorage
import com.shuhart.stepview.StepView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlin.random.Random


class SignUpEmployeeFragment : Fragment() {

    private lateinit var binding : FragmentSignUpEmployeeBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var firebaseUtils: FirebaseUtils
    private lateinit var viewPager: ViewPager2
    private lateinit var adapter: SignUpAdapters
    private lateinit var stepView: StepView
    private lateinit var viewModel: SignUpViewModel
    private lateinit var progressDialog : ProgressDialog
    private lateinit var storage : FirebaseStorage
    private lateinit var loadingDialog: SweetAlertDialog
    private lateinit var fireStore : FirebaseFirestore
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentSignUpEmployeeBinding.inflate(layoutInflater)
        // Inflate the layout for this fragment
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

        adapter.addFragment(StepOneLinemenFragment())
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
            findNavController().navigateUp()
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


    private fun nextItem(){
        val currentItem = viewPager.currentItem
        val nextItem = currentItem + 1
        if (nextItem < adapter.itemCount) {
            viewPager.currentItem = nextItem

        }
    }

    private fun validateFragmentOne() {
        val firstName = viewModel.firstName
        val lastName = viewModel.lastName
        val position = viewModel.position

        if (firstName.isEmpty()) {
            Toast.makeText(requireContext(), "Please add your First Name to continue", Toast.LENGTH_SHORT).show()
            return
        } else if (lastName.isEmpty()) {
            Toast.makeText(requireContext(), "Please add your Last Name to continue", Toast.LENGTH_SHORT).show()
            return
        } else if (position.isEmpty()) {
            Toast.makeText(requireContext(), "Please add your position to continue", Toast.LENGTH_SHORT).show()
            return
        } else {
            nextItem()

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
                        this@SignUpEmployeeFragment.requireContext(),
                        "Failed Creating Account: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
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
            "userType" to "linemen",
            "access" to true,
            "timestamp" to timestamp,
            "barangay" to viewModel.barangay,
            "municipality" to viewModel.municipality,
            "position" to viewModel.position,
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
                            this@SignUpEmployeeFragment.requireContext(),
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