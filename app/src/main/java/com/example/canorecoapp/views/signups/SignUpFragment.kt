package com.example.canorecoapp.views.signups

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.ProgressDialog
import android.os.Bundle
import android.os.Handler
import android.os.Looper
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
import androidx.activity.OnBackPressedCallback
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.viewpager2.widget.ViewPager2
import cn.pedant.SweetAlert.SweetAlertDialog
import com.example.canorecoapp.R
import com.example.canorecoapp.adapter.SignUpAdapters
import com.example.canorecoapp.databinding.FragmentSignUpBinding
import com.example.canorecoapp.utils.DateTimeUtils.Companion.getCurrentDate
import com.example.canorecoapp.utils.DateTimeUtils.Companion.getCurrentTime
import com.example.canorecoapp.utils.DialogUtils
import com.example.canorecoapp.utils.FirebaseUtils
import com.example.canorecoapp.viewmodels.SignUpViewModel
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
import org.bouncycastle.cms.RecipientId.password
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
    private lateinit var successDialog: SweetAlertDialog
    private lateinit var warningMessage: SweetAlertDialog
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
        val phone = viewModel.phone
        val barangay = viewModel.barangay
        val municipality = viewModel.address

        if (phone.isEmpty()) {
            Toast.makeText(
                requireContext(),
                "Please add your Contact Number to continue",
                Toast.LENGTH_SHORT
            ).show()
            return
        } else if (!phone.startsWith("09")) {
            Toast.makeText(
                requireContext(),
                "Phone number must start with '09'. Adjusting the number.",
                Toast.LENGTH_SHORT
            ).show()
            viewModel.phone = "09${phone.trimStart('0')}"
            return
        } else if (barangay.isEmpty()) {
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
            Log.d("SignUpFragment", "validateFragmentTwo: All fields are valid")
        }
    }


    fun validateFragmentThree() {
        val email = viewModel.email
        val password = viewModel.password
        val confirmPass = viewModel.confirmPass
        if (email.isEmpty()) {
            Toast.makeText(requireContext(), "Email cannot be empty", Toast.LENGTH_SHORT).show()
            return
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(requireContext(), "Invalid email format", Toast.LENGTH_SHORT).show()
            return
        } else if (password.isEmpty()) {
            Toast.makeText(requireContext(), "Password cannot be empty", Toast.LENGTH_SHORT).show()
            return
        } else if (confirmPass.isEmpty()) {
            Toast.makeText(requireContext(), "Please confirm your password", Toast.LENGTH_SHORT)
                .show()
            return
        } else if (password != confirmPass) {
            Toast.makeText(requireContext(), "Passwords do not match", Toast.LENGTH_SHORT).show()
            return
        } else {
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
                    successDialog = DialogUtils.showSuccessMessage(
                        requireActivity(),
                        "Success",
                        "Account created successfully"
                    )
                    successDialog.show()
                    if (task.isSuccessful) {
                        findNavController().apply {
                            popBackStack(R.id.signUpFragment, false)
                            navigate(R.id.signInFragment)
                        }
                        auth.signOut()
                        loadingDialog.dismiss()
                    } else {
                        Toast.makeText(
                            this@SignUpFragment.requireContext(),
                            task.exception?.message ?: "Error creating account",
                            Toast.LENGTH_SHORT
                        ).show()
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


}