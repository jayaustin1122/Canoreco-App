package com.example.canorecoapp.views.user

import android.app.Activity
import android.app.AlertDialog
import android.app.ProgressDialog
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
import androidx.navigation.fragment.findNavController
import com.example.canorecoapp.R
import com.example.canorecoapp.databinding.FragmentReportBinding
import com.example.canorecoapp.utils.DateTimeUtils
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage


class ReportFragment : Fragment() {

    private lateinit var binding : FragmentReportBinding
    private lateinit var selectedImage: Uri
    private val IMAGE_PICK_GALLERY_CODE = 102
    private val IMAGE_PICK_CAMERA_CODE = 103
    private lateinit var auth : FirebaseAuth
    private val CAMERA_PERMISSION_CODE = 101
    private lateinit var storage : FirebaseStorage
    private lateinit var progressDialog : ProgressDialog
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentReportBinding.inflate(layoutInflater)
        // Inflate the layout for this fragment
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        auth = FirebaseAuth.getInstance()
        storage = FirebaseStorage.getInstance()
        progressDialog = ProgressDialog(this.requireContext())
        progressDialog.setTitle("Please wait")
        progressDialog.setCanceledOnTouchOutside(false)
        selectedImage = Uri.EMPTY
        binding.fileUploadContainer.setOnClickListener{
            showImagePickerDialog()
        }
        binding.submitButton.setOnClickListener {
            validateData()
        }
    }
    private fun validateData() {
        val report = binding.reportTypeSpinner.text.toString().trim()
        val concern = binding.concernSpinner.text.toString().trim()
        val concernDiscription = binding.concernDescription.text.toString().trim()
        when {
            report.isEmpty() -> Toast.makeText(this.requireContext(), "Complete Report Type", Toast.LENGTH_SHORT).show()
            concern.isEmpty() -> Toast.makeText(this.requireContext(), "Complete Concern", Toast.LENGTH_SHORT).show()
            concernDiscription.isEmpty() -> Toast.makeText(this.requireContext(), "Concern Discription Cannot Be Empty", Toast.LENGTH_SHORT).show()
            !::selectedImage.isInitialized -> Toast.makeText(this.requireContext(),"Please Upload a Picture",
                Toast.LENGTH_SHORT).show()
            else -> uploadData()
        }

    }

    private fun uploadData() {
        progressDialog.setMessage("Uploading Image...")
        progressDialog.show()
        val uid = auth.uid

        val reference = storage.reference.child("Compaints Images")
            .child(uid!!)
        reference.putFile(selectedImage).addOnCompleteListener{
            if (it.isSuccessful){
                reference.downloadUrl.addOnSuccessListener {task->
                    // Pass the RFID data to uploadInfo
                    uploadDB(task.toString())

                }
            } else {
                progressDialog.dismiss()
                Toast.makeText(this@ReportFragment.requireContext(), "Error uploading image", Toast.LENGTH_SHORT).show()
            }
        }
    }
    private fun uploadDB(imageUrl: String) {
        progressDialog.setMessage("Uploading Your Complaint...")
        progressDialog.show()
        val reportText = binding.reportTypeSpinner.text.toString().trim()
        val concernText = binding.concernSpinner.text.toString().trim()
        val concernDiscriptionText = binding.concernDescription.text.toString().trim()
        val uid = auth.uid
        val timestamp = System.currentTimeMillis() /1000

        val report: HashMap<String, Any?> = hashMapOf(
            "uid" to uid,
            "reportTitle" to reportText,
            "concen" to concernText,
            "concernDescription" to concernDiscriptionText,
            "image" to imageUrl,
            "timestamp" to timestamp


        )
        val firestore = FirebaseFirestore.getInstance()
        try {
            firestore.collection("consumer_complaints")
                .document(timestamp.toString())
                .set(report)
                .addOnCompleteListener { task ->
                    progressDialog.dismiss()
                    if (task.isSuccessful) {
                        findNavController().apply {
                          navigateUp()
                        }
                        Toast.makeText(
                            this@ReportFragment.requireContext(),
                            "Complaint Submitted Successfully",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        Toast.makeText(
                            this@ReportFragment.requireContext(),
                            task.exception?.message ?: "Error uploading complaint",
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
            when (requestCode) {
                IMAGE_PICK_GALLERY_CODE -> {
                    selectedImage = data?.data!!
                    binding.fileUploadContainer.setImageURI(selectedImage)
                    Log.d("ReportFragment", "Image selected from gallery: $selectedImage")
                }
                IMAGE_PICK_CAMERA_CODE -> {
                    binding.fileUploadContainer.setImageURI(selectedImage)
                    Log.d("ReportFragment", "Image captured from camera: $selectedImage")
                }
            }
        }
    }

}