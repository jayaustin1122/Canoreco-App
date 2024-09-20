package com.example.canorecoapp.views.user

import android.content.Context
import android.content.SharedPreferences
import android.content.res.ColorStateList
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import android.widget.Toolbar
import androidx.annotation.OptIn
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.example.canorecoapp.R
import com.example.canorecoapp.databinding.FragmentUserHolderBinding
import com.example.canorecoapp.views.user.account.AccountUserFragment
import com.example.canorecoapp.views.user.home.HomeUserFragment
import com.example.canorecoapp.views.user.service.ServicesUserFragment
import com.google.android.material.badge.BadgeDrawable
import com.google.android.material.badge.BadgeUtils
import com.google.android.material.badge.ExperimentalBadgeUtils
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import smartdevelop.ir.eram.showcaseviewlib.GuideView
import smartdevelop.ir.eram.showcaseviewlib.config.DismissType
import smartdevelop.ir.eram.showcaseviewlib.config.Gravity
import smartdevelop.ir.eram.showcaseviewlib.config.PointerType


class UserHolderFragment : Fragment() {
    private lateinit var binding: FragmentUserHolderBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var fragmentManager: FragmentManager
    private var isUserInfoLoaded = false
    private var selectedFragmentId: Int = R.id.navigation_Home


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        binding = FragmentUserHolderBinding.inflate(inflater, container, false)
        return binding.root
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        auth = FirebaseAuth.getInstance()
        fragmentManager = childFragmentManager
        val homeFragment = HomeUserFragment()
        val serviceFragment = ServicesUserFragment()
        val accountUserFragment = AccountUserFragment()
        val sharedPreferences: SharedPreferences = requireActivity().getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
        val areGuidesShown = sharedPreferences.getBoolean("areGuidesShown", false)
        val areFinish = sharedPreferences.getBoolean("areFinish", false)

        if (!areGuidesShown) {
            showGuide(binding.bottomNavigationUser) // Show bottom navigation guide
        }


        if (!isUserInfoLoaded) {
            loadUsersInfo()
        }
        loadUsersInfo()
        val toolbar = binding.toolbar


        // Restore the selected fragment ID if available
        savedInstanceState?.let {
            selectedFragmentId = it.getInt("selectedFragmentId", R.id.navigation_Home)
        }
        toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.notif -> {
                    findNavController().navigate(R.id.notifFragment)
                    true
                }

                else -> false
            }
        }


        val bottomNavigationView: BottomNavigationView? = binding.bottomNavigationUser
        bottomNavigationView?.setOnNavigationItemSelectedListener { item ->
            selectedFragmentId = item.itemId
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
            val initialFragment = when (selectedFragmentId) {
                R.id.navigation_Home -> homeFragment
                R.id.navigation_services -> serviceFragment
                R.id.navigation_account -> accountUserFragment
                else -> homeFragment
            }

            fragmentManager.beginTransaction()
                .replace(R.id.fragment_containerUser, initialFragment)
                .commit()

            bottomNavigationView?.selectedItemId =
                selectedFragmentId
        }
        loadNotificationBadge()


    }


    private fun showGuide(targetView: View) {
        val sharedPreferences: SharedPreferences = requireActivity().getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)

        lifecycleScope.launch {
            val builder = GuideView.Builder(requireContext())
                .setTitle("Bottom Navigation")
                .setContentText("This is where you can navigate Home, Services, and Account")
                .setGravity(Gravity.center)
                .setDismissType(DismissType.anywhere)
                .setPointerType(PointerType.circle)
                .setTargetView(targetView)
                .setGuideListener {
                    showAppBarGuide() // Show app bar guide after bottom navigation guide
                }

            builder.build().show()

        }
    }


    private fun showAppBarGuide() {
            val toolbarGuide = GuideView.Builder(requireContext())
                .setTitle("Notification and Account Icon")
                .setContentText("This is where you can access notifications and user settings.")
                .setGravity(Gravity.center)
                .setDismissType(DismissType.anywhere)
                .setPointerType(PointerType.circle)
                .setTargetView(binding.toolbar)
                .setGuideListener {

                }
                .build()

            toolbarGuide.show()
        }




        @Deprecated("Deprecated in Java")
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(com.example.canorecoapp.R.menu.appbar, menu)


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
                        this@UserHolderFragment.requireContext(),
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
                    if (notificationCount > 0) {
                        val badge: BadgeDrawable = BadgeDrawable.create(this@UserHolderFragment.requireContext())
                        badge.isVisible = true
                        badge.number = notificationCount
                        val toolbar = binding.toolbar
                        val menuItem = toolbar.menu.findItem(R.id.notif)
                        BadgeUtils.attachBadgeDrawable(badge, toolbar, R.id.notif)
                    } else {

                    }
                }
            }
        } ?: run {
            Toast.makeText(
                this@UserHolderFragment.requireContext(),
                "User not authenticated",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
    override fun onDestroyView() {
        super.onDestroyView()
        notificationsListener?.remove()
    }


    @RequiresApi(Build.VERSION_CODES.O)
    private fun loadUsersInfo() {
        val db = FirebaseFirestore.getInstance()
        val currentUser = FirebaseAuth.getInstance().currentUser

        currentUser?.let { user ->
            db.collection("users").document(user.uid).get()
                .addOnSuccessListener { document ->
                    val userName = document.getString("fullName")
                    val image = document.getString("image")
                    val token = document.getString("token")
                    val toolbar = binding.toolbar
                    val menuItem = toolbar.menu.findItem(R.id.imgProfiled)
                    val iconBell = toolbar.menu.findItem(R.id.notif)
                    val whiteColor = ContextCompat.getColor(requireContext(), R.color.white)
                    val colorStateList = ColorStateList.valueOf(whiteColor)
                    iconBell.iconTintList = colorStateList
                    Glide.with(requireContext())
                        .load(image)
                        .error(R.drawable.logo)
                        .transform(com.bumptech.glide.load.resource.bitmap.CircleCrop())
                        .into(object : com.bumptech.glide.request.target.CustomTarget<android.graphics.drawable.Drawable>() {
                            override fun onResourceReady(resource: android.graphics.drawable.Drawable, transition: com.bumptech.glide.request.transition.Transition<in android.graphics.drawable.Drawable>?) {
                                menuItem.icon = resource
                            }

                            override fun onLoadCleared(placeholder: android.graphics.drawable.Drawable?) {

                            }
                        })

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

