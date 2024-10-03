package com.example.canorecoapp

import android.os.Bundle
import android.telephony.SmsManager
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.canorecoapp.databinding.FragmentDeviceNotifBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import pub.devrel.easypermissions.EasyPermissions

class DeviceNotifFragment : Fragment(), EasyPermissions.PermissionCallbacks {
    private lateinit var binding: FragmentDeviceNotifBinding
    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var realtimeDatabase: FirebaseDatabase
    private lateinit var devicesRef: DatabaseReference

    private var notificationsListener: ListenerRegistration? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()
        realtimeDatabase = FirebaseDatabase.getInstance()

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentDeviceNotifBinding.inflate(layoutInflater)
        // Inflate the layout for this fragment
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        requestSmsPermission()
        startListeningForDeviceStatuses()
    }


    private fun startListeningForDeviceStatuses() {
        devicesRef = FirebaseDatabase.getInstance().getReference("devices")
        devicesRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                snapshot.children.forEach { deviceSnapshot ->
                    val status = deviceSnapshot.child("status").getValue(String::class.java) ?: "unknown"
                    if (status == "damaged") {
                        val barangay = deviceSnapshot.child("barangay").getValue(String::class.java) ?: ""
                        handleDeviceDamage(barangay)
                    }
                    if (status == "repaired") {
                        val barangay = deviceSnapshot.child("barangay").getValue(String::class.java) ?: ""
                        sendSmsRepaired(barangay)
                        sendnotif(barangay)
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("DeviceNotifFragment", "Failed to read device statuses", error.toException())
            }
        })
    }

    private fun handleDeviceDamage(barangay: String) {
        val usersRef = db.collection("users")
        usersRef.whereEqualTo("barangay", barangay).get().addOnSuccessListener { querySnapshot ->
            for (userDoc in querySnapshot.documents) {
                val userId = userDoc.id
                val userPhone = userDoc.getString("phone") // Assuming there's a 'phone' field
                val userEmail = userDoc.getString("email") ?: "No Email"
                val notificationTitle = "Electric Post Damaged in $barangay"
                val notificationMessage = "A device in your Barangay: $barangay has been detected as damaged. Please check the app or news for more details."
                val timestamp = System.currentTimeMillis() / 1000
                val notificationData = mapOf(
                    "title" to notificationTitle,
                    "status" to false,
                    "isRead" to false,
                    "message" to notificationMessage,
                    "timestamp" to timestamp.toString()
                )

                // Send SMS
                if (!userPhone.isNullOrEmpty()) {
                    sendSms(userPhone, notificationMessage)
                }
            }
        }.addOnFailureListener { exception ->
            Log.e("DeviceNotifFragment", "Failed to query users", exception)
        }
    }
    private fun sendnotif(barangay: String) {
        val usersRef = db.collection("users")
        usersRef.whereEqualTo("barangay", barangay).get().addOnSuccessListener { querySnapshot ->
            for (userDoc in querySnapshot.documents) {
                val userId = userDoc.id
                val userEmail = userDoc.getString("email") ?: "No Email"
                val notificationTitle = "Electric in $barangay"
                val notificationMessage = "A Electric post in $barangay has been repaired."
                val timestamp = System.currentTimeMillis() / 1000
                val notificationData = mapOf(
                    "title" to notificationTitle,
                    "status" to false,
                    "isRead" to false,
                    "message" to notificationMessage,
                    "timestamp" to timestamp.toString()
                )

                db.collection("users").document(userId)
                    .collection("notifications").document(timestamp.toString())
                    .set(notificationData)
                    .addOnSuccessListener {
                        Log.d("DeviceNotifFragment", "Notification successfully added for user $userId")
                    }
                    .addOnFailureListener { e ->
                        Log.e("DeviceNotifFragment", "Failed to add notification for user $userId", e)
                    }
            }
        }.addOnFailureListener { exception ->
            Log.e("DeviceNotifFragment", "Failed to query users", exception)
        }
    }

    private fun sendSmsRepaired(barangay: String) {
        val usersRef = db.collection("users")
        usersRef.whereEqualTo("barangay", barangay).get().addOnSuccessListener { querySnapshot ->
            for (userDoc in querySnapshot.documents) {
                val userId = userDoc.id
                val userPhone = userDoc.getString("phone") // Assuming there's a 'phone' field
                val userEmail = userDoc.getString("email") ?: "No Email"
                val notificationTitle = "Electric Post Repaired in $barangay"
                val notificationMessage = "A device in your Barangay: $barangay has been repaired"
                val timestamp = System.currentTimeMillis() / 1000
                val notificationData = mapOf(
                    "title" to notificationTitle,
                    "status" to false,
                    "isRead" to false,
                    "message" to notificationMessage,
                    "timestamp" to timestamp.toString()
                )

                // Send SMS
                if (!userPhone.isNullOrEmpty()) {
                    sendSms(userPhone, notificationMessage)
                }
            }
        }.addOnFailureListener { exception ->
            Log.e("DeviceNotifFragment", "Failed to query users", exception)
        }
    }

    private fun sendSms(phoneNumber: String, notificationMessage: String) {
        val sms = SmsManager.getDefault()
        sms.sendTextMessage(phoneNumber,null, notificationMessage,null,null)
        Toast.makeText(requireContext(), "Sms Send", Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        super.onDestroy()
        notificationsListener?.remove()
    }
    private fun hasSmsPermission(): Boolean {
        return EasyPermissions.hasPermissions(requireContext(), android.Manifest.permission.SEND_SMS)
    }
    private val SMS_PERMISSION_REQUEST_CODE = 123
    // Request the SMS permission
    private fun requestSmsPermission() {
        EasyPermissions.requestPermissions(
            this,
            "This app needs permission to send SMS messages.",
            SMS_PERMISSION_REQUEST_CODE,
            android.Manifest.permission.SEND_SMS
        )
    }
    // Override onRequestPermissionsResult to handle the result of the permission request
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        EasyPermissions.onRequestPermissionsResult(
            requestCode,
            permissions,
            grantResults,
            this
        )
    }
    // Implement EasyPermissions.PermissionCallbacks interface
    override fun onPermissionsGranted(requestCode: Int, perms: List<String>) {
        if (requestCode == SMS_PERMISSION_REQUEST_CODE) {

        }
    }
    override fun onPermissionsDenied(requestCode: Int, perms: List<String>) {
        if (requestCode == SMS_PERMISSION_REQUEST_CODE) {
            // Permission denied, inform the user or handle it appropriately
            Toast.makeText(requireContext(), "SMS permission denied", Toast.LENGTH_SHORT).show()
        }
    }
}