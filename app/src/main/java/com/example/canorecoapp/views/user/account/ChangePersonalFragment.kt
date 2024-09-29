package com.example.canorecoapp.views.user.account

import android.app.Activity
import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.ContentValues
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.example.canorecoapp.R

import com.example.canorecoapp.databinding.FragmentChangePersonalBinding
import com.example.canorecoapp.utils.DialogUtils
import com.example.canorecoapp.utils.FirebaseUtils
import com.example.canorecoapp.viewmodels.UserViewModel
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.util.Calendar


class ChangePersonalFragment : Fragment() {
    private lateinit var binding : FragmentChangePersonalBinding
    private lateinit var selectedImage: Uri
    private lateinit var firebaseUtils: FirebaseUtils
    private val CAMERA_PERMISSION_CODE = 101
    private val IMAGE_PICK_GALLERY_CODE = 102
    private val IMAGE_PICK_CAMERA_CODE = 103
    private lateinit var storage : FirebaseStorage
    private lateinit var progressDialog : ProgressDialog
    private lateinit var fireStore : FirebaseFirestore
    private val viewModel: UserViewModel by viewModels()
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentChangePersonalBinding.inflate(layoutInflater)
        // Inflate the layout for this fragment
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        storage = FirebaseStorage.getInstance()
        fireStore = FirebaseFirestore.getInstance()
        progressDialog = ProgressDialog(this.requireContext())
        progressDialog.setTitle("Please wait")
        progressDialog.setCanceledOnTouchOutside(false)
        firebaseUtils = FirebaseUtils()
        firebaseUtils.initialize(requireContext())
        selectedImage = Uri.EMPTY
        viewModel.errorMessage.observe(viewLifecycleOwner, Observer { errorMessage ->
            if (errorMessage != null) {
                Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_SHORT).show()
            }
        })

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            viewModel.loadUserInfo()
        }
        viewModel.userInfo.observe(viewLifecycleOwner, Observer { userInfo ->
            userInfo?.let {
                binding.apply {
                    // Set the user's profile image
                    binding.etFirstName.setText(userInfo.firstName)
                    binding.etLastName.setText(userInfo.lastName)
                    binding.etBirthDate.setText(userInfo.dateOfBirth)

                    Glide.with(requireContext())
                        .load(userInfo.image)
                        .into(binding.imgPersonal)
                }
            }
        })
        binding.imgPersonal.setOnClickListener {
            showImagePickerDialog()
        }
        binding.backButton.setOnClickListener {
            DialogUtils.showWarningMessage(requireActivity(), "Warning", "Are you sure you want to exit? Changes will not be saved."
            ) { sweetAlertDialog ->
                sweetAlertDialog.dismissWithAnimation()
                val bundle = Bundle().apply {
                    putInt("selectedFragmentId", null ?: R.id.navigation_account)
                }
                findNavController().navigate(R.id.userHolderFragment, bundle)
            }
        }

        binding.btnSave.setOnClickListener {

            uploadImage()
        }
        binding.etBirthDate.setOnClickListener {
            showDatePickerDialog()
        }
    }
    private fun uploadImage() {
        if (selectedImage == Uri.EMPTY) {
            val currentUser = FirebaseAuth.getInstance().currentUser
            currentUser?.let { user ->
                val db = FirebaseFirestore.getInstance()
                db.collection("users").document(user.uid).get()
                    .addOnSuccessListener { document ->
                        val existingImage = document.getString("image")
                        updateInFirestore(Uri.parse(existingImage))
                    }
                    .addOnFailureListener { exception ->
                        Toast.makeText(
                            requireContext(),
                            "Failed to load current image: ${exception.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
            }
        } else {
            // Upload new image and then update Firestore
            progressDialog.setMessage("Uploading Image...")
            progressDialog.show()
            val reference = storage.reference.child("profile")
            selectedImage.let {
                reference.putFile(it).addOnCompleteListener {
                    if (it.isSuccessful) {
                        reference.downloadUrl.addOnSuccessListener { image ->
                            updateInFirestore(image)
                        }
                    } else {
                        progressDialog.dismiss()
                        Toast.makeText(
                            this@ChangePersonalFragment.requireContext(),
                            "Error uploading image",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }
    }

    private fun showImagePickerDialog() {
        val options = arrayOf("Camera", "Gallery")
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Choose Image From")
            .setItems(options) { dialog: DialogInterface?, which: Int ->
                when (which) {
                    0 -> {
                        if (checkCameraPermission()) {
                            pickImageFromCamera()
                        } else {
                            requestCameraPermission()
                        }
                    }

                    1 -> pickImageFromGallery()
                }
            }
            .show()
    }

    private fun checkCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            requireContext(),
            android.Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestCameraPermission() {
        ActivityCompat.requestPermissions(
            requireActivity(),
            arrayOf(android.Manifest.permission.CAMERA),
            CAMERA_PERMISSION_CODE
        )
    }

    private fun pickImageFromGallery() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, IMAGE_PICK_GALLERY_CODE)
    }

    private fun pickImageFromCamera() {
        val values = ContentValues()
        values.put(MediaStore.Images.Media.TITLE, "Temp Pic")
        values.put(MediaStore.Images.Media.DESCRIPTION, "Temp Description")
        selectedImage =
            requireActivity().contentResolver.insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                values
            )!!
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        intent.putExtra(MediaStore.EXTRA_OUTPUT, selectedImage)
        startActivityForResult(intent, IMAGE_PICK_CAMERA_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == IMAGE_PICK_GALLERY_CODE) {
                selectedImage = data?.data!!
                binding.imgPersonal.setImageURI(selectedImage)

                Log.d("TwoSignupFragment", "Image selected: $selectedImage")
            } else if (requestCode == IMAGE_PICK_CAMERA_CODE) {
                binding.imgPersonal.setImageURI(selectedImage)

                Log.d("TwoSignupFragment", "Image selected: $selectedImage")
            }
        }
    }
    private fun showDatePickerDialog() {
        val datePicker = MaterialDatePicker.Builder.datePicker()
            .setTitleText("Select Date")
            .build()
        datePicker.addOnPositiveButtonClickListener { selection ->
            val calendar = Calendar.getInstance().apply {
                timeInMillis = selection
            }
            val year = calendar.get(Calendar.YEAR).toString()
            val month = (calendar.get(Calendar.MONTH) + 1).toString()
            val day = calendar.get(Calendar.DAY_OF_MONTH).toString()

            binding.etBirthDate.setText("${month}/${day}/${year}")
        }
        datePicker.show(parentFragmentManager, "MaterialDatePicker")
    }
    private fun updateInFirestore(image: Uri) {
        progressDialog.setMessage("Updating Account...")
        progressDialog.show()

        val currentUser = FirebaseAuth.getInstance().currentUser
        val firstName = binding.etFirstName.text.toString().trim()
        val lastName = binding.etLastName.text.toString().trim()
        val dateOfBirth = binding.etBirthDate.text.toString().trim()

        currentUser?.let { user ->
            val updatedData = hashMapOf<String, Any>(
                "firstName" to firstName,
                "lastName" to lastName,
                "dateOfBirth" to dateOfBirth
            )

            if (image != Uri.EMPTY) {
                updatedData["image"] = image.toString()
            }

            FirebaseFirestore.getInstance().collection("users").document(user.uid)
                .update(updatedData)
                .addOnSuccessListener {
                    progressDialog.dismiss()
                    Toast.makeText(requireContext(), "Account information updated successfully", Toast.LENGTH_SHORT).show()
                    findNavController().navigateUp()
                }
                .addOnFailureListener { exception ->
                    progressDialog.dismiss()
                    Toast.makeText(requireContext(), "Failed to update account info: ${exception.message}", Toast.LENGTH_SHORT).show()
                }
        } ?: run {
            progressDialog.dismiss()
            Toast.makeText(requireContext(), "User not authenticated", Toast.LENGTH_SHORT).show()
        }
    }
}