package com.example.canorecoapp.views.linemen.home

import android.os.Build
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.example.canorecoapp.R
import com.example.canorecoapp.databinding.FragmentLinemenCurrentFurtureBinding
import com.example.canorecoapp.viewmodels.UserViewModel
import com.example.canorecoapp.views.user.outages.CurrentOutagesMapFragment
import com.example.canorecoapp.views.user.outages.FutureOutagesMapFragment
import com.google.android.material.badge.ExperimentalBadgeUtils
import com.google.android.material.tabs.TabLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration


class LinemenCurrentFurtureFragment : Fragment() {
    private lateinit var binding: FragmentLinemenCurrentFurtureBinding
    private val viewModel: UserViewModel by viewModels()
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        binding = FragmentLinemenCurrentFurtureBinding.inflate(layoutInflater)
        // Inflate the layout for this fragment
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadNotificationBadge()
        // Set up the tabs
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("Current Outages"))
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("Future Outages"))

        if (savedInstanceState == null) {
            replaceFragment(HomeLineMenFragment())
        }

        // Handle tab selection
        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                when (tab.position) {
                    0 -> replaceFragment(HomeLineMenFragment())
                    1 -> replaceFragment(FutureOutagesMapFragment())
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab) {
                // No action needed
            }

            override fun onTabReselected(tab: TabLayout.Tab) {
                // No action needed
            }
        })
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            viewModel.loadUserInfo()
        }
        viewModel.userInfo.observe(viewLifecycleOwner, Observer { userInfo ->
            userInfo?.let {
                binding.apply {

                    imgUser?.let {
                        Glide.with(requireContext())
                            .load(userInfo.image)
                            .into(it)
                    }
                    imgUser.setOnClickListener {
                        val bundle = Bundle().apply {
                            putInt("selectedFragmentId", null ?: R.id.navigation_account_linemen)
                        }
                        findNavController().navigate(R.id.adminHolderFragment, bundle)
                    }
                    notif.setOnClickListener {
                        val bundle = Bundle().apply {
                            putString("from", userInfo.userType)
                        }
                        findNavController().navigate(R.id.notifFragment, bundle)
                    }
                }
            }
        })
}

private fun replaceFragment(fragment: Fragment) {
    childFragmentManager.beginTransaction()
        .replace(R.id.map_fragment_container, fragment)
        .commit()
}
    private var notificationsListener: ListenerRegistration? = null

    @OptIn(ExperimentalBadgeUtils::class)
    private fun loadNotificationBadge() {
        val db = FirebaseFirestore.getInstance()
        val currentUser = FirebaseAuth.getInstance().currentUser

        currentUser?.let { user ->
            val notificationsRef = db.collection("users")
                .document(user.uid)
                .collection("notifications")

            notificationsListener?.remove()

            notificationsListener = notificationsRef.addSnapshotListener { snapshot, exception ->
                if (exception != null) {
                    Toast.makeText(
                        this@LinemenCurrentFurtureFragment.requireContext(),
                        "Error Loading Notifications: ${exception.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@addSnapshotListener
                }

                snapshot?.let { querySnapshot ->
                    var notificationCount = 0
                    for (document in querySnapshot.documents) {
                        val status = document.get("status")
                        val notificationStatus = when (status) {
                            is Boolean -> status
                            else -> false
                        }
                        if (!notificationStatus) {
                            notificationCount++
                        }
                    }

                    if (isAdded) {
                        if (notificationCount > 0) {
                            // New notifications: Set notif icon to red
                            binding.notif.setColorFilter(
                                ContextCompat.getColor(requireContext(), R.color.g_red)
                            )
                        } else {
                            // No new notifications: Reset notif icon to default color
                            binding.notif.setColorFilter(
                                ContextCompat.getColor(requireContext(), R.color.white)
                            )
                        }
                    }

                }
            }
        } ?: run {
            Toast.makeText(
                this@LinemenCurrentFurtureFragment.requireContext(),
                "User not authenticated",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}