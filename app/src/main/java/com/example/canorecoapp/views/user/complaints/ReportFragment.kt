package com.example.canorecoapp.views.user.complaints

import android.annotation.SuppressLint
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
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import cn.pedant.SweetAlert.SweetAlertDialog
import com.example.canorecoapp.R
import com.example.canorecoapp.databinding.FragmentReportBinding
import com.example.canorecoapp.utils.DialogUtils
import com.example.canorecoapp.utils.complaints
import com.example.canorecoapp.utils.municipalitiesWithBarangays
import com.example.canorecoapp.viewmodels.ReportViewModel
import com.example.canorecoapp.viewmodels.UserViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage


class ReportFragment : Fragment() {
    private lateinit var loadingDialog: SweetAlertDialog
    private lateinit var binding: FragmentReportBinding
    private var selectedImage: Uri? = null
    private val IMAGE_PICK_GALLERY_CODE = 102
    private val IMAGE_PICK_CAMERA_CODE = 103
    private lateinit var auth: FirebaseAuth
    private var from: String? = null
    private val CAMERA_PERMISSION_CODE = 101
    private var selectedFragmentId: Int? = null
    private val viewModel: ReportViewModel by viewModels()


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentReportBinding.inflate(inflater, container, false)
        return binding.root
    }

    private fun handleBackNavigation() {
        val bundle = Bundle().apply {
            putInt("selectedFragmentId", selectedFragmentId ?: R.id.navigation_Home)
        }
        when (from) {
            "home" -> {
                bundle.putInt("selectedFragmentId", R.id.navigation_Home)
                findNavController().navigate(R.id.userHolderFragment, bundle)
            }

            "service" -> {
                bundle.putInt("selectedFragmentId", R.id.navigation_services)
                findNavController().navigate(R.id.userHolderFragment, bundle)
            }

            else -> {
                bundle.putInt("selectedFragmentId", R.id.navigation_Home)
                findNavController().navigate(R.id.userHolderFragment, bundle)
            }
        }
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()

        arguments?.let {
            from = it.getString("from")
        }

        setupUI()
        setupObservers()
    }

    private fun setupUI() {
        // Initially hide the views
        binding.concernDescriptionInputLayout.visibility = View.GONE
        binding.uploadInstructions.visibility = View.GONE
        binding.located.visibility = View.GONE
        binding.fileUploadContainer.visibility = View.GONE
        binding.submitButton.visibility = View.GONE
        binding.mucipalityTypeInputLayout.visibility = View.GONE
        binding.brgyTypeInputLayout.visibility = View.GONE
        binding.streetTypeInputLayout.visibility = View.GONE

        // Setup Report Type Spinner
        val concerns = complaints.keys.toList()
        val concernsAdapter = ArrayAdapter(requireContext(), R.layout.address_item_views, concerns)
        binding.reportTypeSpinner.setAdapter(concernsAdapter)

        binding.reportTypeSpinner.setOnItemClickListener { parent, view, position, id ->
            val selectedConcern = parent.getItemAtPosition(position).toString()
            val barangays = complaints[selectedConcern] ?: emptyList()
            val barangayAdapter =
                ArrayAdapter(requireContext(), R.layout.address_item_views, barangays)
            binding.concernSpinner.setAdapter(barangayAdapter)
        }

        // Setup Concern Spinner with visibility handling
        binding.concernSpinner.setOnItemClickListener { parent, view, position, id ->
            val selectedConcern = parent.getItemAtPosition(position).toString()

            // Show views when a concern is selected
            binding.concernDescriptionInputLayout.visibility = View.VISIBLE
            binding.uploadInstructions.visibility = View.VISIBLE
            binding.located.visibility = View.VISIBLE
            binding.fileUploadContainer.visibility = View.VISIBLE
            binding.submitButton.visibility = View.VISIBLE
            binding.mucipalityTypeInputLayout.visibility = View.VISIBLE
            binding.brgyTypeInputLayout.visibility = View.VISIBLE
            binding.streetTypeInputLayout.visibility = View.VISIBLE
        }

        // Setup Municipality Spinner
        val municipalities = municipalitiesWithBarangays.keys.toList()
        val municipalityAdapter =
            ArrayAdapter(requireContext(), R.layout.address_item_views, municipalities)
        binding.tvMucipality.setAdapter(municipalityAdapter)
        binding.tvMucipality.setOnItemClickListener { parent, view, position, id ->
            val selectedMunicipality = parent.getItemAtPosition(position).toString()
            binding.tvBrgy.setText("")
            val barangays = municipalitiesWithBarangays[selectedMunicipality] ?: emptyList()
            val barangayAdapter =
                ArrayAdapter(requireContext(), R.layout.address_item_views, barangays)
            binding.tvBrgy.setAdapter(barangayAdapter)
        }

        makeDropdownOnly(binding.reportTypeSpinner)
        makeDropdownOnly(binding.tvMucipality)
        makeDropdownOnly(binding.concernSpinner)
        makeDropdownOnly(binding.tvBrgy)
        binding.fileUploadContainer.setOnClickListener {
            showImagePickerDialog()
        }

        binding.submitButton.setOnClickListener {
            DialogUtils.showWarningMessage(requireActivity(), "Warning", "Are you sure you want to report this complaint?."
            ) { sweetAlertDialog ->
                sweetAlertDialog.dismissWithAnimation()
                loadingDialog = DialogUtils.showLoading(requireActivity())
                loadingDialog.show()
                validateData()
            }

        }

        binding.backButton.setOnClickListener {
            handleBackPressWithConfirmation()
        }

        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    handleBackPressWithConfirmation()
                }
            })
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun makeDropdownOnly(autoCompleteTextView: AutoCompleteTextView) {
        autoCompleteTextView.setOnClickListener {
            autoCompleteTextView.showDropDown()
        }
        autoCompleteTextView.keyListener = null
        autoCompleteTextView.setFocusable(false)
        autoCompleteTextView.setOnTouchListener { _, _ ->
            autoCompleteTextView.showDropDown()
            false
        }
    }

    private fun setupObservers() {
        viewModel.uploadStatus.observe(viewLifecycleOwner, Observer { status ->
            when (status) {
                is ReportViewModel.UploadStatus.Idle -> {
                    Log.d("ReportFragment", "Upload status: Idle")
                }

                is ReportViewModel.UploadStatus.UploadingImage -> {
                    Log.d("ReportFragment", "Uploading image...")
                }

                is ReportViewModel.UploadStatus.ImageUploadSuccess -> {
                    Log.d("ReportFragment", "Image upload successful, URL: ${status.imageUrl}")
                    // Proceed to upload complaint data
                    uploadComplaintData(status.imageUrl)
                }

                is ReportViewModel.UploadStatus.ImageUploadFailure -> {
                    Log.e("ReportFragment", "Image upload failed: ${status.error}")
                    Toast.makeText(requireContext(), status.error, Toast.LENGTH_SHORT).show()
                }

                is ReportViewModel.UploadStatus.UploadingData -> {

                    Log.d("ReportFragment", "Uploading complaint data...")
                    loadingDialog.dismiss()
                    DialogUtils.showSuccessMessage(
                        requireActivity(),
                        "Success!",
                        "Complaint Submitted"
                    ).show()

                    val bundle = Bundle().apply {
                        putInt("selectedFragmentId", selectedFragmentId ?: R.id.navigation_services)
                    }
                    bundle.putInt("selectedFragmentId", R.id.navigation_services)
                    findNavController().navigate(R.id.userHolderFragment, bundle)

                }

                is ReportViewModel.UploadStatus.DataUploadSuccess -> {
                    Log.d("ReportFragment", "Complaint data upload successful")
                    Toast.makeText(
                        requireContext(),
                        "Complaint submitted successfully.",
                        Toast.LENGTH_SHORT
                    ).show()
                    findNavController().navigateUp()
                }

                is ReportViewModel.UploadStatus.DataUploadFailure -> {
                    Log.e("ReportFragment", "Complaint data upload failed: ${status.error}")
                    loadingDialog.dismiss()
                    Toast.makeText(requireContext(), status.error, Toast.LENGTH_SHORT).show()
                }

                else -> {
                    Log.d("ReportFragment", "Unhandled status: $status")
                }
            }
        })
    }

    private fun handleBackPressWithConfirmation() {
        val report = binding.reportTypeSpinner.text.toString().trim()
        val concern = binding.concernSpinner.text.toString().trim()
        val concernDescription = binding.concernDescription.text.toString().trim()

        if (report.isNotEmpty() || concern.isNotEmpty() || concernDescription.isNotEmpty()) {
            DialogUtils.showWarningMessage(
                requireActivity(),
                "Warning",
                "Are you sure you want to exit? Changes will not be saved."
            ) { sweetAlertDialog ->
                sweetAlertDialog.dismissWithAnimation()
                handleBackNavigation()
            }
        } else {
            findNavController().navigateUp()
        }
    }

    private fun validateData() {
        val report = binding.reportTypeSpinner.text.toString().trim()
        val concern = binding.concernSpinner.text.toString().trim()
        val concernDescription = binding.concernDescription.text.toString().trim()
        val municipality = binding.tvMucipality.text.toString().trim()
        val barangay = binding.tvBrgy.text.toString().trim()
        val street = binding.tvStreet.text.toString().trim()

        // Add logging to track the values before upload
        Log.d(
            "ReportFragment",
            "Validating data: Report=$report, Concern=$concern, Description=$concernDescription, Municipality=$municipality, Barangay=$barangay, Street=$street"
        )

        when {
            report.isEmpty() -> {
                Toast.makeText(requireContext(), "Please select a Report Type", Toast.LENGTH_SHORT)
                    .show()

                loadingDialog.dismiss()
            }

            concern.isEmpty() -> {
                Toast.makeText(requireContext(), "Please select a Concern", Toast.LENGTH_SHORT)
                    .show()
                loadingDialog.dismiss()
            }

            concernDescription.isEmpty() -> {
                Toast.makeText(
                    requireContext(),
                    "Concern Description cannot be empty",
                    Toast.LENGTH_SHORT
                ).show()
                loadingDialog.dismiss()
            }

            municipality.isEmpty() -> {
                Toast.makeText(requireContext(), "Please select a Municipality", Toast.LENGTH_SHORT)
                    .show()
                loadingDialog.dismiss()
            }

            barangay.isEmpty() -> {
                Toast.makeText(requireContext(), "Please select a Barangay", Toast.LENGTH_SHORT)
                    .show()
                loadingDialog.dismiss()
            }

            selectedImage == null -> {
                Toast.makeText(requireContext(), "Please upload a picture", Toast.LENGTH_SHORT)
                    .show()
                loadingDialog.dismiss()
            }

            else -> {
                Log.d("ReportFragment", "All data valid, starting image upload...")
                viewModel.uploadImage(selectedImage!!)
            }
        }
    }

    private fun uploadComplaintData(imageUrl: String) {
        val report = binding.reportTypeSpinner.text.toString().trim()
        val concern = binding.concernSpinner.text.toString().trim()
        val concernDescription = binding.concernDescription.text.toString().trim()
        val municipality = binding.tvMucipality.text.toString().trim()
        val barangay = binding.tvBrgy.text.toString().trim()
        val street = binding.tvStreet.text.toString().trim()


        Log.d(
            "ReportFragment",
            "Uploading complaint: Report=$report, Concern=$concern, Description=$concernDescription, Municipality=$municipality, Barangay=$barangay, Street=$street, ImageURL=$imageUrl"
        )

        viewModel.uploadComplaint(
            report,
            concern,
            concernDescription,
            municipality,
            barangay,
            street,
            imageUrl
        )
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
        selectedImage = requireActivity().contentResolver.insert(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            values
        )
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        intent.putExtra(MediaStore.EXTRA_OUTPUT, selectedImage)
        startActivityForResult(intent, IMAGE_PICK_CAMERA_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == android.app.Activity.RESULT_OK) {
            when (requestCode) {
                IMAGE_PICK_GALLERY_CODE -> {
                    selectedImage = data?.data
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