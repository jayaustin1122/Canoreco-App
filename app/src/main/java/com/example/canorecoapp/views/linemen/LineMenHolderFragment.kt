package com.example.canorecoapp.views.linemen

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.doOnAttach
import androidx.fragment.app.FragmentManager
import com.example.canorecoapp.R
import com.example.canorecoapp.databinding.FragmentLineMenHolderBinding
import com.example.canorecoapp.views.linemen.account.AccountLineMenFragment
import com.example.canorecoapp.views.linemen.home.HomeLineMenFragment
import com.example.canorecoapp.views.linemen.home.LinemenCurrentFurtureFragment
import com.example.canorecoapp.views.linemen.notifications.NotificationLineMenFragment
import com.google.android.material.bottomappbar.BottomAppBar

import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class LineMenHolderFragment : Fragment() {
    private lateinit var binding : FragmentLineMenHolderBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var fragmentManager: FragmentManager
    private var isUserInfoLoaded = false
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentLineMenHolderBinding.inflate(layoutInflater)
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        auth = FirebaseAuth.getInstance()
        fragmentManager = requireActivity().supportFragmentManager
        val homeFragment = LinemenCurrentFurtureFragment()
        val serviceFragment = NotificationLineMenFragment()
        val accountUserFragment = AccountLineMenFragment()

        if (!isUserInfoLoaded) {
            loadUsersInfo()
        }

        val bottomNavigationView: BottomNavigationView = binding.bottomNavigationLinemen
        bottomNavigationView.setOnNavigationItemSelectedListener { item ->
            val selectedFragment: Fragment = when (item.itemId) {
                R.id.navigation_Home_linemen -> homeFragment
                R.id.navigation_notificationl_linemen -> serviceFragment
                R.id.navigation_account_linemen -> accountUserFragment

                else -> return@setOnNavigationItemSelectedListener false
            }
            fragmentManager.beginTransaction()
                .replace(R.id.fragment_containerLinemen, selectedFragment)
                .commitAllowingStateLoss()
            true
        }
        if (savedInstanceState == null) {
            if (!homeFragment.isAdded) {
                fragmentManager.beginTransaction()
                    .add(R.id.fragment_containerLinemen, homeFragment)
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