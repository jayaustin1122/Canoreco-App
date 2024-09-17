package com.example.canorecoapp.views.linemen

import android.content.res.ColorStateList
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.core.view.doOnAttach
import androidx.fragment.app.FragmentManager
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.example.canorecoapp.R
import com.example.canorecoapp.databinding.FragmentLineMenHolderBinding
import com.example.canorecoapp.views.linemen.account.AccountLineMenFragment
import com.example.canorecoapp.views.linemen.home.HomeLineMenFragment
import com.example.canorecoapp.views.linemen.home.LinemenCurrentFurtureFragment
import com.example.canorecoapp.views.linemen.notifications.NotificationLineMenFragment
import com.google.android.material.badge.BadgeDrawable
import com.google.android.material.badge.BadgeUtils
import com.google.android.material.badge.ExperimentalBadgeUtils

import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class LineMenHolderFragment : Fragment() {
    private lateinit var binding : FragmentLineMenHolderBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var fragmentManager: FragmentManager
    private var isUserInfoLoaded = false
    private var selectedFragmentId: Int = R.id.navigation_Home_linemen
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentLineMenHolderBinding.inflate(layoutInflater)
        return binding.root
    }


    @RequiresApi(Build.VERSION_CODES.O)
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
        loadUsersInfo()
        val toolbar = binding.toolbar
        // Restore the selected fragment ID if available
        savedInstanceState?.let {
            selectedFragmentId = it.getInt("selectedFragmentId", R.id.navigation_Home_linemen)
        }

        toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.notifEmployee -> {
                    findNavController().navigate(R.id.notifFragment)
                    true
                }

                else -> false
            }
        }

        val bottomNavigationView: BottomNavigationView = binding.bottomNavigationLinemen
        bottomNavigationView.setOnNavigationItemSelectedListener { item ->
            selectedFragmentId = item.itemId
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

            val initialFragment = when (selectedFragmentId) {
                R.id.navigation_Home_linemen -> homeFragment
                R.id.navigation_notificationl_linemen -> serviceFragment
                R.id.navigation_account_linemen -> accountUserFragment
                else -> homeFragment
            }

            fragmentManager.beginTransaction()
                .replace(R.id.fragment_containerLinemen, initialFragment)
                .commit()

            bottomNavigationView?.selectedItemId = selectedFragmentId  // Set the selected item in bottom navigation
        }
        loadNotificationBadge()
    }
    @Deprecated("Deprecated in Java")
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(com.example.canorecoapp.R.menu.appbar_employee, menu)


    }
    private fun saveNotificationToFirestore(title: String, text: String) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid != null) {
            val db = FirebaseFirestore.getInstance()
            val notificationData = hashMapOf(
                "title" to title,
                "text" to text,
                "timestamp" to System.currentTimeMillis(),
                "status" to false
            )
            db.collection("users")
                .document(uid)
                .collection("notifications")
                .document(System.currentTimeMillis().toString())
                .set(notificationData)
                .addOnSuccessListener {

                }
                .addOnFailureListener { e ->

                    e.printStackTrace()
                }
        }
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
                        this@LineMenHolderFragment.requireContext(),
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
                        val badge: BadgeDrawable = BadgeDrawable.create(this@LineMenHolderFragment.requireContext())
                        badge.isVisible = true
                        badge.number = notificationCount
                        val toolbar = binding.toolbar
                        val menuItem = toolbar.menu.findItem(R.id.notifEmployee)
                        BadgeUtils.attachBadgeDrawable(badge, toolbar, R.id.notifEmployee)
                    } else {

                    }
                }
            }
        } ?: run {
            Toast.makeText(
                this@LineMenHolderFragment.requireContext(),
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
                    val menuItem = toolbar.menu.findItem(R.id.imgProfiledEmployee)
                    val iconBell = toolbar.menu.findItem(R.id.notifEmployee)

                    if (menuItem == null || iconBell == null) {
                        Log.e("DEBUG", "Menu item or iconBell is null")
                        return@addOnSuccessListener
                    }

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