package com.example.canorecoapp.views.user.account

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ContentValues
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
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
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import cn.pedant.SweetAlert.SweetAlertDialog
import com.bumptech.glide.Glide
import com.example.canorecoapp.R
import com.example.canorecoapp.databinding.FragmentAccountLineMenBinding
import com.example.canorecoapp.utils.DialogUtils
import com.example.canorecoapp.viewmodels.UserViewModel
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

class AccountUserFragment : Fragment() {
    private lateinit var binding: FragmentAccountLineMenBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var fireStore: FirebaseFirestore
    private lateinit var selectedImage: Uri
    private lateinit var loadingDialog: SweetAlertDialog
    private val viewModel: UserViewModel by viewModels()
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        binding = FragmentAccountLineMenBinding.inflate(layoutInflater)
        // Inflate the layout for this fragment
        return binding.root
    }

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        auth = FirebaseAuth.getInstance()
        fireStore = FirebaseFirestore.getInstance()
        selectedImage = Uri.EMPTY
        loadingDialog = DialogUtils.showLoading(requireActivity())
        lifecycleScope.launch {
            loadingDialog.show()
            Handler(Looper.getMainLooper()).postDelayed({
                viewModel.userInfo.observe(viewLifecycleOwner, Observer { userInfo ->
                    userInfo?.let {
                        binding.apply {
                            // Set the user's profile image
                            binding.username.text = "${userInfo.firstName} ${userInfo.lastName}"
                            binding.contactNumber.text = userInfo.phone
                            Glide.with(requireContext()).load(userInfo.image)
                                .into(binding.imgUserProfile)
                            loadingDialog.dismissWithAnimation()
                        }
                    }
                })
            }, 600)
        }
        viewModel.errorMessage.observe(viewLifecycleOwner, Observer { errorMessage ->
            if (errorMessage != null) {
                Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_SHORT).show()
            }
        })

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            viewModel.loadUserInfo()
        }

        binding.logoutCard.setOnClickListener {
            DialogUtils.showWarningMessage(
                requireActivity(), "Logout", "Are you sure you want to Logout?"
            ) { sweetAlertDialog ->
                sweetAlertDialog.dismissWithAnimation()
                auth.signOut()
                findNavController().navigate(R.id.signInFragment)
            }
        }
        binding.logout.setOnClickListener {
            DialogUtils.showWarningMessage(
                requireActivity(), "Logout", "Are you sure you want to Logout?"
            ) { sweetAlertDialog ->
                sweetAlertDialog.dismissWithAnimation()
                auth.signOut()
                findNavController().navigate(R.id.signInFragment)
            }
        }
        binding.logoutIcon.setOnClickListener {
            DialogUtils.showWarningMessage(
                requireActivity(), "Logout", "Are you sure you want to Logout?"
            ) { sweetAlertDialog ->
                sweetAlertDialog.dismissWithAnimation()
                auth.signOut()
                findNavController().navigate(R.id.signInFragment)
            }
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

}