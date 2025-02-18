package com.example.canorecoapp.views.user.account

import android.annotation.SuppressLint
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import cn.pedant.SweetAlert.SweetAlertDialog
import com.bumptech.glide.Glide
import com.example.canorecoapp.R
import com.example.canorecoapp.databinding.FragmentAccountUserBinding
import com.example.canorecoapp.utils.DialogUtils
import com.example.canorecoapp.viewmodels.UserViewModel
import com.example.canorecoapp.views.user.news.FullScreenImageFragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class AccountUserFragment : Fragment() {
    private lateinit var binding: FragmentAccountUserBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var fireStore: FirebaseFirestore
    private lateinit var selectedImage: Uri
    private val viewModel: UserViewModel by viewModels()
    private lateinit var loadingDialog: SweetAlertDialog
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentAccountUserBinding.inflate(layoutInflater)
        return binding.root
    }

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        auth = FirebaseAuth.getInstance()
        fireStore = FirebaseFirestore.getInstance()
        selectedImage = Uri.EMPTY

        // Observe the user info from the ViewModel
        viewModel.userInfo.observe(viewLifecycleOwner, Observer { userInfo ->
            userInfo?.let {
                binding.apply {
                    // Set user's name and contact number
                    username.text = "${userInfo.firstName} ${userInfo.lastName}"
                    contactNumber.text = userInfo.phone

                    // Load user's profile image or default image if not found
                    val imageUrl = userInfo.image
                    if (!imageUrl.isNullOrEmpty()) {
                        Glide.with(requireContext())
                            .load(imageUrl)
                            .into(imgUserProfile)
                    } else {
                        // Set a default image if the user's image is missing
                        Glide.with(requireContext())
                            .load(R.drawable.img_user_placeholder) // replace with your default image
                            .into(imgUserProfile)
                    }

//                    // Set click listener to open image in FullScreenImageFragment
//                    imgUserProfile.setOnClickListener {
//                        val bundle = Bundle().apply {
//                            // Check if imageUrl is null or empty and handle it accordingly
//                            if (!imageUrl.isNullOrEmpty()) {
//                                // If imageUrl is available, pass it as a string in the list
//                                putStringArrayList("imageList", arrayListOf(imageUrl))
//                            } else {
//                                // If imageUrl is not available, pass a default resource ID as an integer
//                                putInt("defaultImageRes", R.drawable.img_user_placeholder)
//                            }
//                            putInt("initialPosition", 0)
//                        }
//                        findNavController().navigate(R.id.fullScreenImageFragment, bundle)
//                    }

                }
            } ?: run {

            }
        })


        // Handle error messages from the ViewModel
        viewModel.errorMessage.observe(viewLifecycleOwner, Observer { errorMessage ->
            errorMessage?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
            }
        })

        // Trigger user info load
        viewModel.loadUserInfo()

        // Set up view listeners for user actions
        setupViewListeners()
    }

    private fun setupViewListeners() {
        binding.addAccount.setOnClickListener {
            findNavController().navigate(R.id.addAccountFragment)
        }
        binding.logoutCard.setOnClickListener { showLogoutDialog() }
        binding.logout.setOnClickListener { showLogoutDialog() }
        binding.logoutIcon.setOnClickListener { showLogoutDialog() }
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

    private fun showLogoutDialog() {
        DialogUtils.showWarningMessage(
            requireActivity(), "Logout", "Are you sure you want to Logout?"
        ) { sweetAlertDialog ->
            sweetAlertDialog.dismissWithAnimation()
            loadingDialog = DialogUtils.showLoading(requireActivity())
            loadingDialog.show()
            Handler().postDelayed({
                loadingDialog.dismiss()
                auth.signOut()
                findNavController().navigate(R.id.signInFragment)
            }, 2000)

        }
    }
}
