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
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.example.canorecoapp.R
import com.example.canorecoapp.databinding.FragmentStepTwoBinding
import com.example.canorecoapp.utils.FirebaseUtils
import com.example.canorecoapp.viewmodels.SignUpViewModel


class StepTwoFragment : Fragment() {
    private lateinit var binding: FragmentStepTwoBinding
    private lateinit var selectedImage: Uri
    private lateinit var viewModel: SignUpViewModel
    private lateinit var firebaseUtils: FirebaseUtils
    private val CAMERA_PERMISSION_CODE = 101
    private val IMAGE_PICK_GALLERY_CODE = 102
    private val IMAGE_PICK_CAMERA_CODE = 103
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentStepTwoBinding.inflate(layoutInflater)
        // Inflate the layout for this fragment
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
        selectedImage = Uri.EMPTY
        binding.btnUpload.setOnClickListener {
            showImagePickerDialog()
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
                binding.imageView2.setImageURI(selectedImage)
                viewModel.setImage2(selectedImage)
                Log.d("TwoSignupFragment", "Image selected: $selectedImage")
                binding.btnUpload.visibility = View.GONE
            } else if (requestCode == IMAGE_PICK_CAMERA_CODE) {
                binding.imageView2.setImageURI(selectedImage)
                viewModel.setImage2(selectedImage)
                Log.d("TwoSignupFragment", "Image selected: $selectedImage")
                binding.btnUpload.visibility = View.GONE
            }
        }
    }
}