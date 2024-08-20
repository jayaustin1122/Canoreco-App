package com.example.canorecoapp.views.user

import android.annotation.SuppressLint
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
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
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
        binding = FragmentUserHolderBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        auth = FirebaseAuth.getInstance()
        fragmentManager = requireActivity().supportFragmentManager
        val homeFragment = HomeUserFragment()
        val serviceFragment = ServicesUserFragment()
        val accountUserFragment = AccountUserFragment()

        if (!isUserInfoLoaded) {
            loadUsersInfo()
        }

        val bottomNavigationView: BottomNavigationView = binding.bottomNavigation
        bottomNavigationView.setOnNavigationItemSelectedListener { item ->
            val selectedFragment: Fragment = when (item.itemId) {
                R.id.navigation_Home -> homeFragment
                R.id.navigation_services -> serviceFragment
                R.id.navigation_account -> accountUserFragment

                else -> return@setOnNavigationItemSelectedListener false
            }
            fragmentManager.beginTransaction()
                .replace(R.id.fragment_container, selectedFragment)
                .commitAllowingStateLoss()
            true
        }

        if (savedInstanceState == null) {
            if (!homeFragment.isAdded) {
                fragmentManager.beginTransaction()
                    .add(R.id.fragment_container, homeFragment)
                    .commit()
            }
            bottomNavigationView.selectedItemId = R.id.navigation_Home
        }
    }

    private fun loadUsersInfo() {
        val db = FirebaseFirestore.getInstance()
        val currentUser = FirebaseAuth.getInstance().currentUser

        currentUser?.let { user ->
            db.collection("users").document(user.uid).get()
                .addOnSuccessListener { document ->
                    val userName = document.getString("fullName")
                    Toast.makeText(
                        requireContext(),
                        "Welcome ${userName ?: "User"}!",
                        Toast.LENGTH_SHORT
                    ).show()
                    isUserInfoLoaded = true // Mark user info as loaded
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
