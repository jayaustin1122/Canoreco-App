package com.example.canorecoapp.views.user.outages

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.annotation.RequiresApi
import androidx.navigation.fragment.findNavController
import com.example.canorecoapp.R
import com.example.canorecoapp.databinding.FragmentOutagesBinding
import com.google.android.material.tabs.TabLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import smartdevelop.ir.eram.showcaseviewlib.GuideView
import smartdevelop.ir.eram.showcaseviewlib.config.DismissType
import smartdevelop.ir.eram.showcaseviewlib.config.Gravity
import smartdevelop.ir.eram.showcaseviewlib.config.PointerType

class OutagesFragment : Fragment() {
    private lateinit var binding: FragmentOutagesBinding
    private var selectedFragmentId: Int? = null
    private var from: String? = null
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentOutagesBinding.inflate(layoutInflater)
        return binding.root
    }
    private fun handleBackNavigation() {
        val bundle = Bundle().apply {
            putInt("selectedFragmentId", null ?: R.id.navigation_Home)
        }
        when (from) {
            "home" -> findNavController().navigate(R.id.userHolderFragment, bundle)
            "service" -> {
                bundle.putInt("selectedFragmentId", null ?: R.id.navigation_services)
                findNavController().navigate(R.id.userHolderFragment, bundle)
            }
        }
    }
    private fun replaceFragment(fragment: Fragment, from: String?) {
        val bundle = Bundle().apply {
            putString("from", from)
        }
        fragment.arguments = bundle

        childFragmentManager.beginTransaction()
            .replace(R.id.map_fragment_container, fragment)
            .commit()
    }
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadUsersInfo()
        showAppBarGuide()
        arguments?.let {
            from = it.getString("from")
        }


        // Set up the tabs
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("Current Outages"))
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("Future Outages"))


        if (savedInstanceState == null) {
            replaceFragment(CurrentOutagesMapFragment(),from)
        }

        // Handle tab selection
        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                when (tab.position) {
                    0 -> replaceFragment(CurrentOutagesMapFragment(),from)
                    1 -> replaceFragment(FutureOutagesMapFragment(),from)
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab) {
                // No action needed
            }

            override fun onTabReselected(tab: TabLayout.Tab) {
                // No action needed
            }
        })
    }

    private fun showAppBarGuide() {
        val sharedPreferences = requireActivity().getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
        val areGuidesShown = sharedPreferences.getBoolean("sss", false)

        if (!areGuidesShown) {
            val toolbarGuide = GuideView.Builder(requireContext())
                .setTitle("Current and Future Outages")
                .setContentText("This is where you can see all Future Outages and Current Outages")
                .setGravity(Gravity.center)
                .setDismissType(DismissType.anywhere)
                .setPointerType(PointerType.circle)
                .setTargetView(binding.tabLayout)
                .setGuideListener {
                }
                .build()

            toolbarGuide.show()
        }
    }


    @RequiresApi(Build.VERSION_CODES.O)
    private fun loadUsersInfo() {
        val db = FirebaseFirestore.getInstance()
        val currentUser = FirebaseAuth.getInstance().currentUser

        currentUser?.let { user ->
            db.collection("users").document(user.uid).get()
                .addOnSuccessListener { document ->
                    val userType = document.getString("userType")

                    when (userType) {
                        "member" -> {
                            binding.backButton.setOnClickListener {
                              handleBackNavigation()
                            }
                            requireActivity().onBackPressedDispatcher.addCallback(
                                viewLifecycleOwner,
                                object : OnBackPressedCallback(true) {
                                    override fun handleOnBackPressed() {
                                       handleBackNavigation()
                                    }
                                })
                        }

                        "linemen" -> {
                            binding.backButton.setOnClickListener {
                                val bundle = Bundle().apply {
                                    findNavController().navigate(R.id.adminHolderFragment)
                                }
                                findNavController().navigate(R.id.adminHolderFragment, bundle)
                            }
                            requireActivity().onBackPressedDispatcher.addCallback(
                                viewLifecycleOwner,
                                object : OnBackPressedCallback(true) {
                                    override fun handleOnBackPressed() {
                                        findNavController().navigate(R.id.adminHolderFragment)
                                    }
                                })
                        }

                        else -> {
                            Toast.makeText(
                                requireContext(),
                                "Unknown user type",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
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
