package com.example.canorecoapp.views.signups

import android.app.AlertDialog
import android.content.ContentValues
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
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
import androidx.lifecycle.ViewModelProvider
import com.example.canorecoapp.R
import com.example.canorecoapp.databinding.FragmentStepOneBinding
import com.example.canorecoapp.utils.FirebaseUtils
import com.example.canorecoapp.viewmodels.SignUpViewModel
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.firestore.FirebaseFirestore
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions


class StepOneFragment : Fragment() {
    private lateinit var binding: FragmentStepOneBinding
    private lateinit var viewModel: SignUpViewModel
    private val CAMERA_PERMISSION_CODE = 101
    private lateinit var firebaseUtils: FirebaseUtils
    private val IMAGE_PICK_GALLERY_CODE = 102
    private val IMAGE_PICK_CAMERA_CODE = 103
    private var selectedImage: Uri? = null
    private var extractedText: String = ""
    private val firestore = FirebaseFirestore.getInstance()
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentStepOneBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel = ViewModelProvider(requireActivity())[SignUpViewModel::class.java]
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.fileUploadContainer.setOnClickListener {
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
        // Create a ContentValues object to store metadata for the image
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.TITLE, "Temp Pic")
            put(MediaStore.Images.Media.DESCRIPTION, "Temp Description")
        }

        // Insert the metadata into MediaStore and get a URI to save the captured image
        selectedImage = requireActivity().contentResolver.insert(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            values
        )

        if (selectedImage != null) {
            // Create an Intent for the camera
            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
                putExtra(MediaStore.EXTRA_OUTPUT, selectedImage) // Pass the URI to save the image
            }

            // Start the camera activity
            startActivityForResult(intent, IMAGE_PICK_CAMERA_CODE)
        } else {
            Log.e("PickImageFromCamera", "Failed to create MediaStore entry for image.")
            Toast.makeText(context, "Unable to access camera. Please try again.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == android.app.Activity.RESULT_OK) {
            when (requestCode) {
                IMAGE_PICK_GALLERY_CODE -> {
                    selectedImage = data?.data
                    if (selectedImage != null) {
                        binding.fileUploadContainer.setImageURI(selectedImage)
                        Log.d("ReportFragment", "Image selected from gallery: $selectedImage")
                        val bitmap = MediaStore.Images.Media.getBitmap(
                            this@StepOneFragment.requireActivity().contentResolver,
                            selectedImage
                        )
                        processImage(bitmap)
                        binding.fileUploadContainer.visibility = View.GONE
                    } else {
                        Log.e("ReportFragment", "Failed to get image URI from gallery.")
                    }
                }

                IMAGE_PICK_CAMERA_CODE -> {
                    if (selectedImage != null) {
                        // Use the URI provided during camera intent setup
                        binding.fileUploadContainer.setImageURI(selectedImage)
                        Log.d("ReportFragment", "Image captured from camera: $selectedImage")
                        val bitmap = MediaStore.Images.Media.getBitmap(
                            this@StepOneFragment.requireActivity().contentResolver,
                            selectedImage
                        )
                        processImage(bitmap)
                        binding.fileUploadContainer.visibility = View.GONE
                    } else {
                        Log.e("ReportFragment", "Image URI is null after capturing.")
                    }
                }
            }
        } else if (resultCode == android.app.Activity.RESULT_CANCELED) {
            Log.d("ReportFragment", "Image selection or capture canceled by user.")
        } else {
            Log.e("ReportFragment", "Unexpected resultCode: $resultCode")
        }
    }

    private fun processImage(bitmap: Bitmap) {
        val image = InputImage.fromBitmap(bitmap, 0)
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

        recognizer.process(image)
            .addOnSuccessListener { visionText ->
                extractedText = visionText.text
                Log.d("TextRecognition", "Extracted Text: $extractedText")
                verifyReceipt()
            }
            .addOnFailureListener { e ->

            }
    }
    private fun verifyReceipt() {
        val trimmedText = extractedText.trim()
        Log.d("ExtractedText", "Full Text: $trimmedText")

        // Regex to extract CCT# value
        val accountNumberRegex = Regex("cct#:\\s*(\\d{2}-\\d{4}-\\d{4})", RegexOption.IGNORE_CASE)
        val match = accountNumberRegex.find(trimmedText)
        val extractedAccountNumber = match?.groups?.get(1)?.value

        if (extractedAccountNumber != null) {
            Log.d("ExtractedCCT", "CCT#: $extractedAccountNumber")

            firestore.collection("accounts")
                .whereEqualTo("accountNumber", extractedAccountNumber)
                .get()
                .addOnSuccessListener { documents ->
                    if (!documents.isEmpty) {
                        for (document in documents) {
                            // Extract fields from the Firestore document
                            val consumerAccount = document.getString("consumerAccount") ?: "N/A"
                            val accountNumbers = document.getString("accountNumber") ?: "N/A"
                            val barangay = document.getString("barangay") ?: "N/A"
                            val firstName = document.getString("firstName") ?: "N/A"
                            val lastName = document.getString("lastName") ?: "N/A"
                            val municipality = document.getString("municipality") ?: "N/A"
                            val status = document.getString("status") ?: "N/A"
                            val street = document.getString("street") ?: "N/A"

                            // Log all fields
                            Log.d("VerifyReceipt", "Consumer Account: $consumerAccount")
                            Log.d("VerifyReceipt", "Account Numbers: $accountNumbers")
                            Log.d("VerifyReceipt", "Barangay: $barangay")
                            Log.d("VerifyReceipt", "First Name: $firstName")
                            Log.d("VerifyReceipt", "Last Name: $lastName")
                            Log.d("VerifyReceipt", "Municipality: $municipality")
                            Log.d("VerifyReceipt", "Status: $status")
                            Log.d("VerifyReceipt", "Street: $street")

                            if (status == "unlinked") {
                                // Set data in the view model
                                viewModel.firstName = firstName
                                viewModel.lastName = lastName
                                viewModel.barangay = barangay
                                viewModel.street = street
                                viewModel.municipality = municipality
                                viewModel.meterNumber = accountNumbers
                                Log.d("VerifyReceipt", "ViewModel Updated: ${viewModel.meterNumber}")

                                // Update UI to display account details
                                binding.divider1.visibility = View.VISIBLE
                                binding.accountDetailsTextView.visibility = View.VISIBLE
                                binding.accountNameTextView.visibility = View.GONE
                                binding.accountNumberTextView.visibility = View.VISIBLE

                                binding.accountNameTextInputLayout.visibility = View.VISIBLE
                                binding.etAccountName.setText("$firstName $lastName")

                                binding.municipality.visibility = View.VISIBLE
                                binding.etMunicipality.setText(municipality)

                                binding.barangaytextInputLayout.visibility = View.VISIBLE
                                binding.etBarangay.setText(barangay)

                                binding.streetTextInputLayout.visibility = View.VISIBLE
                                binding.etStreet.setText(street)
                            } else {
                                // If the account is already linked


                                // Dialog -----------------

                                binding.fileUploadContainer.setImageDrawable(
                                    ContextCompat.getDrawable(requireContext(), R.drawable.upload)
                                );

                                binding.fileUploadContainer.visibility =View.VISIBLE

                                Toast.makeText(context, "This account is already in use", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } else {
                        binding.fileUploadContainer.setImageDrawable(
                            ContextCompat.getDrawable(requireContext(), R.drawable.upload)
                        );
                        // No matching documents found
                        Toast.makeText(context, "Account is not registered.", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("VerifyReceipt", "Error fetching account: ${e.message}")
                    Toast.makeText(context, "Error verifying account: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        } else {
            binding.fileUploadContainer.setImageDrawable(
                ContextCompat.getDrawable(requireContext(), R.drawable.upload)
            );
            binding.fileUploadContainer.visibility =View.VISIBLE

            // Log an error message if the CCT# is not found
            Log.e("ExtractedCCT", "No CCT# found in the receipt text.")

            Snackbar.make(
                binding.root,
                "Image unclear or no Canoreco bill detected. Please retake a clear photo of your bill.",
                Snackbar.LENGTH_LONG
            )
                .show()
        }
    }


}
