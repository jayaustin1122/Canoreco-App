package com.example.canorecoapp

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.example.canorecoapp.databinding.ActivityMainBinding
import com.example.canorecoapp.databinding.DialogVerificationBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    private lateinit var auth: FirebaseAuth

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        // Initialize NavController
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.fragmentContainerView) as NavHostFragment
        navController = navHostFragment.navController

        checkEmailVerification()
        intent?.let {
            if (it.hasExtra("navigate_to_fragment")) {
                val fragmentTag = it.getStringExtra("navigate_to_fragment")
                navigateToFragment(fragmentTag)
            }
        }

        // Request notification permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                REQUEST_CODE_PERMISSION)
        } else {
            startNotificationService()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startNotificationService()
            }
        }
    }

    private fun startNotificationService() {
        val serviceIntent = Intent(this, NotificationService::class.java)
        startService(serviceIntent)
    }

    private fun navigateToFragment(fragmentTag: String?) {
        when (fragmentTag) {
            "YourFragmentTag" -> {
                navController.navigate(R.id.splashFragment)
            }
        }
    }

    private fun checkEmailVerification() {
        val currentUser: FirebaseUser? = auth.currentUser
        if (currentUser != null && !currentUser.isEmailVerified) {
            // Show dialog to ask for email verification

        } else if (currentUser == null) {
            // User is not logged in, navigate to login screen or splash fragment
            navController.navigate(R.id.splashFragment)
        }
    }

    private fun showVerificationDialog(user: FirebaseUser) {
        val dialogBinding = DialogVerificationBinding.inflate(layoutInflater)
        val dialogBuilder = AlertDialog.Builder(this)
        dialogBuilder.setView(dialogBinding.root)
        val dialog = dialogBuilder.create()
        dialog.setCancelable(false)
        dialog.show()

        dialogBinding.btnResend.setOnClickListener {
            sendVerificationEmail(user)
        }

        dialogBinding.btnContinue.isEnabled = false
        dialogBinding.btnContinue.setOnClickListener {
            if (auth.currentUser?.isEmailVerified == true) {
                dialog.dismiss()
                navigateToFragment("YourFragmentTag")
            } else {
                auth.signOut()
                dialog.dismiss()
                navigateToFragment("splashFragment") // Log out and return to login
            }
        }

        // Continuously check for verification
        lifecycleScope.launch {
            while (auth.currentUser?.isEmailVerified == false) {
                try {
                    auth.currentUser?.reload()?.addOnCompleteListener { task ->
                        if (task.isSuccessful && auth.currentUser?.isEmailVerified == true) {
                            dialogBinding.btnContinue.isEnabled = true
                        }
                    }
                } catch (e: Exception){
                    e.printStackTrace()
                }
                delay(5000) // Check every 5 seconds

            }
        }
    }

    private fun sendVerificationEmail(user: FirebaseUser) {
        user.sendEmailVerification()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    showToast("Verification email sent to ${user.email}")
                } else {
                    showToast("Failed to send verification email.")
                }
            }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    companion object {
        private const val REQUEST_CODE_PERMISSION = 1
    }
}
