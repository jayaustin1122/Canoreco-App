package com.example.canorecoapp.views.linemen.employee

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.ProgressDialog
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.viewpager2.widget.ViewPager2
import com.example.canorecoapp.R
import com.example.canorecoapp.adapter.SignUpAdapters
import com.example.canorecoapp.databinding.FragmentSignUpEmployeeBinding
import com.example.canorecoapp.utils.DateTimeUtils.Companion.getCurrentDate
import com.example.canorecoapp.utils.DateTimeUtils.Companion.getCurrentTime
import com.example.canorecoapp.utils.FirebaseUtils
import com.example.canorecoapp.viewmodels.SignUpViewModel
import com.example.canorecoapp.views.signups.StepFourEmployeeFragment
import com.example.canorecoapp.views.signups.StepOneFragment
import com.example.canorecoapp.views.signups.StepThreeFragment
import com.example.canorecoapp.views.signups.StepTwoFragment
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
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
import java.util.Calendar
import java.util.concurrent.TimeUnit


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

        adapter.addFragment(StepOneFragment())
        adapter.addFragment(StepTwoFragment())
        adapter.addFragment(StepFourEmployeeFragment())
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
            findNavController().navigateUp()
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
        val firstName = viewModel.firstName
        val lastName = viewModel.lastName
        val month = viewModel.month
        val day = viewModel.day
        val year = viewModel.year
        val selectedImageUri = viewModel.image
        if (firstName.isEmpty() && lastName.isEmpty()) {
            Toast.makeText(requireContext(), "Please enter your First Name And Last Name", Toast.LENGTH_SHORT).show()
            return
        }else if (month.isEmpty() || day.isEmpty() || year.isEmpty()) {
            Toast.makeText(requireContext(), "Please Select Date of Birth", Toast.LENGTH_SHORT)
                .show()
            return
        }else if (selectedImageUri == null) {
            Toast.makeText(requireContext(), "Please upload a profile picture", Toast.LENGTH_SHORT).show()
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
        val phone = viewModel.phone
        val barangay = viewModel.barangay
        val municipality = viewModel.municipality

        if (phone.isEmpty()) {
            Toast.makeText(requireContext(), "Please add your Contact Number to continue", Toast.LENGTH_SHORT).show()
            return
        } else if (!phone.startsWith("09")) {
            Toast.makeText(requireContext(), "Phone number must start with '09'. Adjusting the number.", Toast.LENGTH_SHORT).show()
            viewModel.phone = "09${phone.trimStart('0')}"
            return
        } else if (barangay.isEmpty()) {
            Toast.makeText(requireContext(), "Please add your Barangay to continue", Toast.LENGTH_SHORT).show()
            return
        } else if (municipality.isEmpty()) {
            Toast.makeText(requireContext(), "Please add your Municipality to continue", Toast.LENGTH_SHORT).show()
            return
        } else {
            nextItem()
            Log.d("SignUpFragment", "validateFragmentTwo: All fields are valid")
        }
    }


    fun validateFragmentThree() {
        val email = viewModel.email
        val password = viewModel.password
        val confirmPass = viewModel.confirmPass
        val area = viewModel.area
        if (email.isEmpty()) {
            Toast.makeText(requireContext(), "Email cannot be empty", Toast.LENGTH_SHORT).show()
            return
        }
        else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(requireContext(), "Invalid email format", Toast.LENGTH_SHORT).show()
            return
        }
        else if (password.isEmpty()) {
            Toast.makeText(requireContext(), "Password cannot be empty", Toast.LENGTH_SHORT).show()
            return
        }
        else if (area.isEmpty()) {
            Toast.makeText(requireContext(), "Area Cannot be Empty", Toast.LENGTH_SHORT).show()
            return
        }
        else if (confirmPass.isEmpty()) {
            Toast.makeText(requireContext(), "Please confirm your password", Toast.LENGTH_SHORT).show()
            return
        }
        else if (password != confirmPass) {
            Toast.makeText(requireContext(), "Passwords do not match", Toast.LENGTH_SHORT).show()
            return
        }
        else{
            progressDialog.dismiss()
            createUserAccount()
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
                val user = auth.currentUser
                verifyEmail(user)
                val fcmToken = FirebaseMessaging.getInstance().token.await()

                withContext(Dispatchers.Main) {
                    uploadImage(fcmToken)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    progressDialog.dismiss()
                    Toast.makeText(
                        this@SignUpEmployeeFragment.requireContext(),
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
                        this@SignUpEmployeeFragment.requireContext(),
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
        val firstName = viewModel.firstName
        val lastName = viewModel.lastName
        val email = viewModel.email
        val password = viewModel.password
        val month = viewModel.month
        val day = viewModel.day
        val year = viewModel.year
        val uid = auth.uid
        val timestamp = System.currentTimeMillis()/1000


        val user: HashMap<String, Any?> = hashMapOf(
            "uid" to uid,
            "email" to email,
            "password" to password,
            "firstName" to firstName,
            "lastName" to lastName,
            "image" to imageUrl,
            "phone" to viewModel.phone,
            "userType" to "linemen",
            "access" to true,
            "area" to viewModel.area,
            "token" to token,
            "dateOfBirth" to "$month-$day-$year",
            "timestamp" to timestamp,
            "barangay" to viewModel.barangay,
            "municipality" to viewModel.municipality,
        )
        val firestore = FirebaseFirestore.getInstance()
        try {
            firestore.collection("users")
                .document(uid!!)
                .set(user)
                .addOnCompleteListener { task ->

                    progressDialog.dismiss()
                    if (task.isSuccessful) {
                        findNavController().apply {
                            popBackStack(R.id.signUpFragment, false)
                            navigate(R.id.signInFragment)
                        }
                        auth.signOut()
                    } else {
                        Toast.makeText(
                            this@SignUpEmployeeFragment.requireContext(),
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
    private fun verifyEmail(user: FirebaseUser?) {
        if (user == null) {
            Log.e("EmailVerification", "User is not logged in. Cannot send verification email.")
            return
        }
        user.sendEmailVerification()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d("EmailVerification", "Verification email sent to ${user.email}")
                    showVerificationDialog(user)
                } else {
                    Log.e("EmailVerification", "Failed to send verification email to ${user.email}. Task failed.")
                    Toast.makeText(requireContext(), "Error sending verification email", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { exception ->
                Log.e("EmailVerification", "Error sending verification email: ${exception.message}")
                Toast.makeText(requireContext(), exception.message, Toast.LENGTH_SHORT).show()
            }
    }


    @SuppressLint("MissingInflatedId")
    private fun showVerificationDialog(user: FirebaseUser) {
        val dialogBuilder = AlertDialog.Builder(requireContext())
        val inflater = layoutInflater
        val dialogView = inflater.inflate(R.layout.dialog_verification, null)
        dialogBuilder.setView(dialogView)
        val btnContinue = dialogView.findViewById<Button>(R.id.btnContinue)
        val btnResend = dialogView.findViewById<TextView>(R.id.btnResend)

        val dialog = dialogBuilder.create()
        dialog.setCancelable(false)
        dialog.show()

        btnContinue.isEnabled = false
        btnResend.setOnClickListener {
            verifyEmail(user)
        }

        lifecycleScope.launch {
            while (auth.currentUser?.isEmailVerified == false) {
                auth.currentUser?.reload()?.addOnCompleteListener { task ->
                    if (task.isSuccessful && auth.currentUser?.isEmailVerified == true) {
                        btnContinue.isEnabled = true
                    }
                }
                delay(5000)
            }

        }

        btnContinue.setOnClickListener {
            if (auth.currentUser?.isEmailVerified == true) {
                dialog.dismiss()
            } else {
                Toast.makeText(requireContext(), "Please verify your email before proceeding.", Toast.LENGTH_SHORT).show()
            }
        }
    }

}