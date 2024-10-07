package com.example.canorecoapp

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.example.canorecoapp.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize NavController
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.fragmentContainerView) as NavHostFragment
        navController = navHostFragment.navController

        // Handle intent for fragment navigation
        intent?.let {
            if (it.hasExtra("navigate_to_fragment")) {
                val fragmentTag = it.getStringExtra("navigate_to_fragment")
                navigateToFragment(fragmentTag)
            }
        }
        WindowCompat.setDecorFitsSystemWindows(
            window,false
        )


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

    companion object {
        private const val REQUEST_CODE_PERMISSION = 1
    }
}
