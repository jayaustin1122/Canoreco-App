package com.example.canorecoapp.views.signups

import android.app.Activity
import android.app.AlertDialog
import android.content.ContentValues
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.ViewModelProvider
import com.example.canorecoapp.R
import com.example.canorecoapp.databinding.FragmentStepOneBinding
import com.example.canorecoapp.utils.FirebaseUtils
import com.example.canorecoapp.viewmodels.SignUpViewModel
import com.google.android.material.datepicker.MaterialDatePicker
import java.util.Calendar

class StepOneFragment : Fragment() {
    private lateinit var binding: FragmentStepOneBinding
    private lateinit var viewModel: SignUpViewModel
    private lateinit var selectedImage: Uri
    private lateinit var firebaseUtils: FirebaseUtils
    private val CAMERA_PERMISSION_CODE = 101
    private val IMAGE_PICK_GALLERY_CODE = 102
    private val IMAGE_PICK_CAMERA_CODE = 103
    private val STORAGE_PERMISSION_CODE = 104

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        Log.d("StepOneFragment", "onCreateView called")
        binding = FragmentStepOneBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("StepOneFragment", "onCreate called")
        viewModel = ViewModelProvider(requireActivity())[SignUpViewModel::class.java]
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d("StepOneFragment", "onViewCreated called")

        firebaseUtils = FirebaseUtils()
        firebaseUtils.initialize(requireContext())
        Log.d("StepOneFragment", "FirebaseUtils initialized")

        selectedImage = Uri.EMPTY
        binding.addImageButton.setOnClickListener {
            Log.d("StepOneFragment", "Add Image Button clicked")
            showImagePickerDialog()
        }
        binding.etFirstName.addTextChangedListener {
            viewModel.firstName = it.toString()
            Log.d("StepOneFragment", "First Name: ${viewModel.firstName}")
        }
        binding.etLastName.addTextChangedListener {
            viewModel.lastName = it.toString()
            Log.d("StepOneFragment", "Last Name: ${viewModel.lastName}")
        }
        binding.etBirthDate.setOnClickListener {
            Log.d("StepOneFragment", "Birth Date Field clicked")
            showDatePickerDialog()
        }
    }

    private fun showDatePickerDialog() {
        Log.d("StepOneFragment", "showDatePickerDialog called")
        val datePicker = MaterialDatePicker.Builder.datePicker()
            .setTitleText("Select Date")
            .build()
        datePicker.addOnPositiveButtonClickListener { selection ->
            val calendar = Calendar.getInstance().apply {
                timeInMillis = selection
            }
            viewModel.year = calendar.get(Calendar.YEAR).toString()
            viewModel.month = (calendar.get(Calendar.MONTH) + 1).toString()
            viewModel.day = calendar.get(Calendar.DAY_OF_MONTH).toString()

            binding.etBirthDate.setText("${viewModel.month}/${viewModel.day}/${viewModel.year}")
            Log.d("StepOneFragment", "Date selected: ${binding.etBirthDate.text}")
        }
        datePicker.show(parentFragmentManager, "MaterialDatePicker")
    }

    private fun checkStoragePermission(): Boolean {
        val permissionGranted = ContextCompat.checkSelfPermission(
            requireContext(),
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
        Log.d("StepOneFragment", "Storage permission granted: $permissionGranted")
        return permissionGranted
    }

    private fun requestStoragePermission() {
        Log.d("StepOneFragment", "Requesting storage permission")
        ActivityCompat.requestPermissions(
            requireActivity(),
            arrayOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE),
            STORAGE_PERMISSION_CODE
        )
    }

    private fun showImagePickerDialog() {
        Log.d("StepOneFragment", "showImagePickerDialog called")
        val options = arrayOf("Camera", "Gallery")
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Choose Image From")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> {
                        Log.d("StepOneFragment", "Camera option selected")
                        if (checkCameraPermission() && (checkStoragePermission() || android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q)) {
                            pickImageFromCamera()
                        } else {
                            Log.d("StepOneFragment", "Requesting camera and/or storage permissions")
                            requestCameraPermission()
                            if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.Q) {
                                requestStoragePermission()
                            }
                        }
                    }
                    1 -> {
                        Log.d("StepOneFragment", "Gallery option selected")
                        pickImageFromGallery()
                    }
                }
            }
            .show()
    }

    private fun checkCameraPermission(): Boolean {
        val permissionGranted = ContextCompat.checkSelfPermission(
            requireContext(),
            android.Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
        Log.d("StepOneFragment", "Camera permission granted: $permissionGranted")
        return permissionGranted
    }

    private fun requestCameraPermission() {
        Log.d("StepOneFragment", "Requesting camera permission")
        ActivityCompat.requestPermissions(
            requireActivity(),
            arrayOf(android.Manifest.permission.CAMERA),
            CAMERA_PERMISSION_CODE
        )
    }

    private fun pickImageFromGallery() {
        Log.d("StepOneFragment", "pickImageFromGallery called")
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, IMAGE_PICK_GALLERY_CODE)
    }

    private fun pickImageFromCamera() {
        Log.d("StepOneFragment", "pickImageFromCamera called")
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.TITLE, "Temp Pic")
            put(MediaStore.Images.Media.DESCRIPTION, "Temp Description")
        }
        selectedImage = requireActivity().contentResolver.insert(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            values
        ) ?: Uri.EMPTY

        if (selectedImage != Uri.EMPTY) {
            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
                putExtra(MediaStore.EXTRA_OUTPUT, selectedImage)
            }
            startActivityForResult(intent, IMAGE_PICK_CAMERA_CODE)
            Log.d("StepOneFragment", "Camera intent started")
        } else {
            Log.e("StepOneFragment", "Failed to create MediaStore entry")
            Toast.makeText(requireContext(), "Failed to create MediaStore entry", Toast.LENGTH_SHORT).show()
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if ((requestCode == CAMERA_PERMISSION_CODE || requestCode == STORAGE_PERMISSION_CODE) &&
            grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Log.d("StepOneFragment", "Permissions granted")
            pickImageFromCamera()
        } else {
            Log.e("StepOneFragment", "Permission Denied")
            Toast.makeText(requireContext(), "Permission Denied", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == IMAGE_PICK_GALLERY_CODE) {
                selectedImage = data?.data!!
                binding.imageView2.setImageURI(selectedImage)
                viewModel.setImage2(selectedImage)
                Log.d("StepOneFragment", "Image selected from gallery: $selectedImage")
            } else if (requestCode == IMAGE_PICK_CAMERA_CODE) {
                binding.imageView2.setImageURI(selectedImage)
                viewModel.setImage2(selectedImage)
                Log.d("StepOneFragment", "Image captured by camera: $selectedImage")
            }
        } else {
            Log.e("StepOneFragment", "Image selection/capture failed or canceled")
        }
    }
}
