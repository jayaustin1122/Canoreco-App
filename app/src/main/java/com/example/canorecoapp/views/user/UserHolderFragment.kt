package com.example.canorecoapp.views.user

import com.example.canorecoapp.views.user.home.HomeUserFragment
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.FragmentManager
import com.bumptech.glide.Glide
import com.example.canorecoapp.R
import com.example.canorecoapp.databinding.FragmentUserHolderBinding
import com.example.canorecoapp.views.user.account.AccountUserFragment
import com.example.canorecoapp.views.user.service.ServicesUserFragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class UserHolderFragment : Fragment() {
    private lateinit var binding: FragmentUserHolderBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var fragmentManager: FragmentManager
    private var isUserInfoLoaded = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentUserHolderBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        auth = FirebaseAuth.getInstance()
        fragmentManager = childFragmentManager

        val homeFragment = HomeUserFragment()
        val serviceFragment = ServicesUserFragment()
        val accountUserFragment = AccountUserFragment()

        if (!isUserInfoLoaded) {
            loadUsersInfo()
        }

        val bottomNavigationView: BottomNavigationView? = binding.bottomNavigationUser
        bottomNavigationView?.setOnNavigationItemSelectedListener { item ->
            val selectedFragment: Fragment = when (item.itemId) {

                R.id.navigation_Home -> homeFragment
                R.id.navigation_services -> serviceFragment
                R.id.navigation_account -> accountUserFragment
                else -> return@setOnNavigationItemSelectedListener false
            }
            fragmentManager.beginTransaction()
                .replace(R.id.fragment_containerUser, selectedFragment)
                .commit()

            true
        }

        if (savedInstanceState == null) {
            if (!homeFragment.isAdded) {
                fragmentManager.beginTransaction()
                    .add(R.id.fragment_containerUser, homeFragment)
                    .commit()
            }
            bottomNavigationView?.selectedItemId = R.id.navigation_Home
        }
    }

    private fun loadUsersInfo() {
        val db = FirebaseFirestore.getInstance()
        val currentUser = FirebaseAuth.getInstance().currentUser

        currentUser?.let { user ->
            db.collection("users").document(user.uid).get()
                .addOnSuccessListener { document ->
                    val userName = document.getString("fullName")
                    val image = document.getString("image")
                    val token = document.getString("token")
                    val context = context ?: return@addOnSuccessListener
                    binding.imgProfile?.let {
                        Glide.with(context)
                            .load(image)
                            .into(it)
                    }

                    isUserInfoLoaded = true
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

