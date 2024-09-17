package com.example.canorecoapp.views.user.account

import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.content.ContentValues
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.navigation.fragment.findNavController
import cn.pedant.SweetAlert.SweetAlertDialog
import com.bumptech.glide.Glide
import com.example.canorecoapp.R
import com.example.canorecoapp.databinding.DialogChangePhoneNumberBinding
import com.example.canorecoapp.databinding.FragmentAccountLineMenBinding
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class AccountUserFragment : Fragment() {
    private lateinit var binding : FragmentAccountLineMenBinding
    private lateinit var auth : FirebaseAuth
    private lateinit var fireStore : FirebaseFirestore
    private lateinit var selectedImage: Uri
    private val CAMERA_PERMISSION_CODE = 101
    private val IMAGE_PICK_GALLERY_CODE = 102
    private val IMAGE_PICK_CAMERA_CODE = 103
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentAccountLineMenBinding.inflate(layoutInflater)
        // Inflate the layout for this fragment
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        auth = FirebaseAuth.getInstance()
        fireStore = FirebaseFirestore.getInstance()
        selectedImage = Uri.EMPTY

        Handler(Looper.getMainLooper()).postDelayed({
            loadUsersInfo()
        }, 600)

        binding.logoutCard.setOnClickListener {
            val progressDialog = SweetAlertDialog(requireContext(), SweetAlertDialog.PROGRESS_TYPE)
            progressDialog.titleText = "Logging out..."
            progressDialog.show()

            auth.signOut()
            Handler(Looper.getMainLooper()).postDelayed({
                progressDialog.changeAlertType(SweetAlertDialog.SUCCESS_TYPE)
                progressDialog.titleText = "Logged Out!"
                progressDialog.confirmText = "OK"
                progressDialog.setCanceledOnTouchOutside(false)
                progressDialog.setConfirmClickListener {
                    findNavController().navigate(R.id.signInFragment)
                    progressDialog.dismiss()
                }
            }, 1000)
        }
        binding.updateProfile.setOnClickListener {
            findNavController().navigate(R.id.changePersonalFragment)
        }
        binding.updateContactAddress.setOnClickListener {
            findNavController().navigate(R.id.changeContactAddressFragment)
        }
        binding.changePassword.setOnClickListener {
            findNavController().navigate(R.id.changePasswordFragment)
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
                binding.imgUserProfile.setImageURI(selectedImage)
                Log.d("TwoSignupFragment", "Image selected: $selectedImage")
                updateProfile(selectedImage)
            } else if (requestCode == IMAGE_PICK_CAMERA_CODE) {
                binding.imgUserProfile.setImageURI(selectedImage)

                Log.d("TwoSignupFragment", "Image selected: $selectedImage")
                binding.imgUserProfile.visibility = View.GONE
            }
        }
    }

    fun updateProfile(selectedImage: Uri) {
        val firestore = FirebaseFirestore.getInstance()
        val auth = FirebaseAuth.getInstance()
        val userId = auth.currentUser?.uid
        val user = auth.currentUser


        firestore.collection("users")
            .document(userId!!)
            .update("profile", selectedImage.toString())
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(requireContext(), "Profile Updated", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(
                        this.requireContext(),
                        task.exception?.message ?: "Error updating Profile in Firestore",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }


    private fun loadUsersInfo() {
        val db = FirebaseFirestore.getInstance()
        val currentUser = FirebaseAuth.getInstance().currentUser

        currentUser?.let { user ->
            db.collection("users").document(user.uid).get()
                .addOnSuccessListener { document ->

                    val userName = document.getString("firstName")
                    val lastName = document.getString("lastName")
                    val contact = document.getString("phone")
                    val image = document.getString("image")
                    val email = document.getString("email")
                    val password = document.getString("password")

                    binding.username.text = "$userName $lastName"
                    binding.contactNumber.text = contact
                    val context = context ?: return@addOnSuccessListener
                    Glide.with(context)
                        .load(image) // Load the image URL from Firestore
                        .into(binding.imgUserProfile)

                }
                .addOnFailureListener { exception ->

                    Toast.makeText(
                        requireContext(),
                        "Error Loading User Data: ${exception.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
        } ?: run {

            Toast.makeText(
                requireContext(),
                "User not authenticated",
                Toast.LENGTH_SHORT
            ).show()
        }
    }



}