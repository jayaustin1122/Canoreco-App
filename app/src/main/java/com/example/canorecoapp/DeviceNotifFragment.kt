package com.example.canorecoapp

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.telephony.SmsManager
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.canorecoapp.databinding.FragmentDeviceNotifBinding
import com.example.canorecoapp.viewmodels.DeviceViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import pub.devrel.easypermissions.EasyPermissions
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DeviceNotifFragment : Fragment(), EasyPermissions.PermissionCallbacks {
    private lateinit var binding: FragmentDeviceNotifBinding
    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var realtimeDatabase: FirebaseDatabase
    private lateinit var devicesRef: DatabaseReference
    private val viewModel: DeviceViewModel by viewModels()

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

    override fun onStart() {
        super.onStart()
        startListeningForSms()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        requestSmsPermission()
        startListeningForDeviceStatuses()
        startListeningForDeviceStatuse2()
        startListeningForSms()
        startListeningForNotifications()
        startListeningForSmsOtp()
        // Observe real-time changes for device 9000
        viewModel.device9000Status.observe(viewLifecycleOwner) { status ->
            binding.buttonDevice9000.text = "Device 9000 ($status)"
        }

        // Observe real-time changes for device 9001
        viewModel.device9001Status.observe(viewLifecycleOwner) { status ->
            binding.buttonDevice9001.text = "Device 9001 ($status)"

        }

        // Handle button clicks
        binding.buttonDevice9000.setOnClickListener {
            val currentStatus = viewModel.device9000Status.value ?: "working"
            viewModel.toggleDeviceStatus("9000", currentStatus)
        }

        binding.buttonDevice9001.setOnClickListener {
            val currentStatus = viewModel.device9001Status.value ?: "working"
            viewModel.toggleDeviceStatus("9001", currentStatus)
        }
    }

    private fun startListeningForNotifications() {
        val userId = auth.currentUser?.uid ?: return
        val notificationsRef = db.collection("users")
            .document(userId)
            .collection("notifications")

        notificationsListener = notificationsRef.addSnapshotListener { snapshot, exception ->
            if (exception != null) {
                // Handle error here
                return@addSnapshotListener
            }

            snapshot?.let { snap ->
                for (document in snap.documentChanges) {
                    if (document.type == com.google.firebase.firestore.DocumentChange.Type.ADDED) {
                        val doc = document.document
                        val title = doc.getString("title") ?: "No Title"
                        val timestamp = doc.get("timestamp")
                        val isRead = doc.getBoolean("isRead") ?: false

                        if (!isRead) {
                            val notificationsRef2 = db.collection("users")
                                .document(userId)
                                .collection("notifications")
                                .document(timestamp.toString())
                            createNotification(
                                title,
                                timestamp.toString(),
                                notificationsRef2,
                                isRead
                            )
                            notificationsRef2.update("isRead", true)
                                .addOnCompleteListener { updateTask ->
                                    if (!updateTask.isSuccessful) {
                                        Log.e(
                                            "NotificationService",
                                            "Failed to update isRead status",
                                            updateTask.exception
                                        )
                                    }
                                }
                        }
                    }
                }
            }
        }
    }

    @SuppressLint("SimpleDateFormat")
    private fun parseAndFormatDate(timestampString: String): String {
        return try {

            val timestamp = timestampString.toDoubleOrNull()?.toLong() ?: return ""
            val date = Date(timestamp * 1000)
            val outputFormat = SimpleDateFormat("MMMM d, yyyy h:mm a", Locale.getDefault())
            outputFormat.format(date)
        } catch (e: NumberFormatException) {
            Log.e("Home", "Number format error: ", e)
            ""
        }
    }

    private fun createNotification(
        title: String,
        timestamp: String,
        notificationsRef2: DocumentReference,
        isRead: Boolean
    ) {
        val channelId = "test_channel_id"
        val channelName = "Test Channel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val notificationChannel =
                NotificationChannel(channelId, channelName, importance).apply {
                    description = "Channel description"
                    enableLights(true)
                    lightColor = Color.RED
                    enableVibration(true)
                    vibrationPattern = longArrayOf(0, 500, 1000)
                }
            val notificationManager =
                context?.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(notificationChannel)
        }
        val intent = Intent(requireContext(), MainActivity::class.java).apply {
            updateIsRead(notificationsRef2, isRead)
            putExtra("navigate_to_fragment", "YourFragmentTag")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK

        }
        val pendingIntent = PendingIntent.getActivity(
            requireContext(),
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )


        val notificationBuilder = NotificationCompat.Builder(requireContext(), channelId)
            .setSmallIcon(R.drawable.logo)
            .setContentTitle(title)
            .setContentText(parseAndFormatDate(timestamp))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(false)
            .setVibrate(longArrayOf(0, 500, 1000))
            .setContentIntent(pendingIntent)

        with(NotificationManagerCompat.from(requireContext())) {
            if (ActivityCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
            notify(1, notificationBuilder.build())
        }
    }

    private fun updateIsRead(notificationsRef2: DocumentReference, isRead: Boolean) {
        notificationsRef2.update("isRead", true).addOnCompleteListener { updateTask ->
            if (!updateTask.isSuccessful) {
                Log.e("NotificationService", "Failed to update isRead status", updateTask.exception)
            }
        }
    }

    private fun startListeningForSms() {
        // Reference to Firestore
        val smsRef = FirebaseFirestore.getInstance().collection("sms").document("to_all")
        val usersRef = FirebaseFirestore.getInstance().collection("users")

        smsRef.addSnapshotListener { snapshot, e ->
            if (e != null) {
                Log.e("DeviceNotifFragment", "Listen failed.", e)
                return@addSnapshotListener
            }

            if (snapshot != null && snapshot.exists()) {
                val status = snapshot.getBoolean("send") ?: false
                val content = snapshot.getString("content") ?: ""

                // Log the current status and content
                Log.d("SMSFragment", "Status: $status, Content: $content")

                // Check if the status changed to true
                if (status) {
                    // Retrieve user phone numbers
                    usersRef.get().addOnSuccessListener { usersSnapshot ->
                        val numbers = mutableListOf<String>()

                        for (user in usersSnapshot) {
                            val phone = user.getString("phone")
                            if (phone != null) {
                                numbers.add(phone)
                            }
                        }

                        // Log to indicate sending SMS to all users
                        Log.d(
                            "DeviceNotifFragment",
                            "Sending SMS to all users: $numbers with content: $content"
                        )

                        sendSms1(numbers, content)

                        // Optionally reset the send status in Firestore
                        smsRef.update("send", false) // Reset send status after sending
                    }.addOnFailureListener { usersError ->
                        Log.e(
                            "DeviceNotifFragment",
                            "Failed to read user phone numbers",
                            usersError
                        )
                    }
                }
            } else {
                Log.d("DeviceNotifFragment", "Current data: null")
            }
        }
    }

    private fun startListeningForSmsOtp() {
        val smsRef = FirebaseFirestore.getInstance().collection("sms").document("otp")

        smsRef.addSnapshotListener { snapshot, e ->
            if (e != null) {
                Log.e("DeviceNotifFragment", "Listen failed.", e)
                return@addSnapshotListener
            }

            if (snapshot != null && snapshot.exists()) {
                val status = snapshot.getBoolean("status") ?: false
                val code = snapshot.getString("code") ?: ""
                val phone = snapshot.getString("phone") ?: ""
                Log.d("SMSFragment", "StatusOTP: $status, Content: $code")
                if (status && phone.isNotEmpty()) {
                    sendSmsOtp(phone, code)
                    smsRef.update("status", false)
                        .addOnSuccessListener {
                            Log.d("SMSFragment", "Status updated to false after sending OTP")
                        }
                        .addOnFailureListener { error ->
                            Log.e("SMSFragment", "Failed to update status: $error")
                        }
                }
            } else {
                Log.d("DeviceNotifFragment", "Current data: null")
            }
        }
    }


    private fun startListeningForDeviceStatuses() {
        // Reference the specific device by its id (9001)
        devicesRef = FirebaseDatabase.getInstance().getReference("devices/9001/status")

        // Add a listener to monitor the 'status' field of the device directly
        devicesRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                // Get the status value directly
                val status = snapshot.getValue(String::class.java) ?: "unknown"

                // Fetch other necessary fields (barangay, id) by querying the parent of 'status'
                val parentRef = snapshot.ref.parent
                parentRef?.let { parentSnapshotRef ->
                    parentSnapshotRef.addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(parentSnapshot: DataSnapshot) {
                            val id =
                                parentSnapshot.child("id").getValue(String::class.java) ?: "unknown"
                            val barangay =
                                parentSnapshot.child("barangay").getValue(String::class.java) ?: ""

                            // Now handle different statuses
                            when (status) {
                                "damaged", "working", "under repair" -> {
                                    Log.e("DeviceNotifFragment", "Device is $status")
                                    sendSmsto(barangay, status)
                                    sendnotif(barangay, id, status)
                                }

                                else -> {
                                    Log.e("DeviceNotifFragment", "Unknown status: $status")
                                }
                            }
                        }

                        override fun onCancelled(error: DatabaseError) {
                            Log.e(
                                "DeviceNotifFragment",
                                "Failed to fetch device details",
                                error.toException()
                            )
                        }
                    })
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("DeviceNotifFragment", "Failed to read device status", error.toException())
            }
        })
    }

    private fun startListeningForDeviceStatuse2() {
        // Reference the specific device by its id (9000)
        devicesRef = FirebaseDatabase.getInstance().getReference("devices/9000/status")

        // Add a listener to monitor the 'status' field of the device directly
        devicesRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                // Get the status value directly
                val status = snapshot.getValue(String::class.java) ?: "unknown"

                // Fetch other necessary fields (barangay, id) by querying the parent of 'status'
                val parentRef = snapshot.ref.parent
                parentRef?.let { parentSnapshotRef ->
                    parentSnapshotRef.addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(parentSnapshot: DataSnapshot) {
                            val id =
                                parentSnapshot.child("id").getValue(String::class.java) ?: "unknown"
                            val barangay =
                                parentSnapshot.child("barangay").getValue(String::class.java) ?: ""

                            // Now handle different statuses
                            when (status) {
                                "damaged", "working", "under repair" -> {
                                    Log.e("DeviceNotifFragment", "Device is $status")
                                    sendSmsto(barangay, status)
                                    sendnotif(barangay, id, status)
                                }

                                else -> {
                                    Log.e("DeviceNotifFragment", "Unknown status: $status")
                                }
                            }
                        }

                        override fun onCancelled(error: DatabaseError) {
                            Log.e(
                                "DeviceNotifFragment",
                                "Failed to fetch device details",
                                error.toException()
                            )
                        }
                    })
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("DeviceNotifFragment", "Failed to read device status", error.toException())
            }
        })
    }


    private fun sendnotif(barangay: String, id: String, status: String) {
        val usersRef = db.collection("users")

        // Fetch all users or limit based on some criteria if needed
        usersRef.get().addOnSuccessListener { querySnapshot ->
            if (querySnapshot.isEmpty) {
                Log.d("DeviceNotifFragment", "No users found in the users collection")
                return@addOnSuccessListener
            }

            // Log the id that was passed for comparison
            Log.d("DeviceNotifFragment", "Passed idArea: $id")

            // Loop through each user document
            for (userDoc in querySnapshot.documents) {
                val userId = userDoc.id
                val userEmail = userDoc.getString("email") ?: "No Email"
                val userBarangay = userDoc.getString("barangay") ?: "No idArea"

                // Compare the idArea manually
                if (userBarangay == barangay) {
                    Log.d(
                        "DeviceNotifFragment",
                        "User $userId matched idArea: $userBarangay with passed id: $id"
                    )

                    val notificationTitle = "Electric post status update in $barangay"
                    val notificationMessage = when (status) {
                        "damaged" -> "Attention: The electric post in $barangay is reported damaged. Our team is addressing the issue."
                        "working" -> "Good news: The electric post in $barangay is now back to full operation. Thank you for your patience."
                        "under repair" -> "Update: The electric post in $barangay is under repair. Service will resume soon."
                        else -> "Notification: The electric post in $barangay has an unknown status. Please contact support for more details."
                    }
                    val timestamp = System.currentTimeMillis() / 1000
                    val notificationData = mapOf(
                        "title" to notificationTitle,
                        "status" to false,
                        "isRead" to false,
                        "message" to notificationMessage,
                        "timestamp" to timestamp.toString()
                    )

                    // Sending notification to the matching user
                    db.collection("users").document(userId)
                        .collection("notifications").document(timestamp.toString())
                        .set(notificationData)
                        .addOnSuccessListener {
                            Log.d(
                                "DeviceNotifFragment",
                                "Notification successfully added for user $userId"
                            )
                        }
                        .addOnFailureListener { e ->
                            Log.e(
                                "DeviceNotifFragment",
                                "Failed to add notification for user $userId",
                                e
                            )
                        }
                } else {
                    // Log if user idArea doesn't match the passed id
                }
            }
        }.addOnFailureListener { exception ->
            Log.e("DeviceNotifFragment", "Failed to query users", exception)
        }
    }


    private fun sendSmsto(barangay: String, status: String) {
        val usersRef = db.collection("users")
        usersRef.whereEqualTo("barangay", barangay).get().addOnSuccessListener { querySnapshot ->
            for (userDoc in querySnapshot.documents) {
                val userId = userDoc.id
                val userPhone = userDoc.getString("phone") // Assuming there's a 'phone' field
                val userEmail = userDoc.getString("email") ?: "No Email"
                val notificationTitle = "Electric post status update in $barangay"
                val notificationMessage = when (status) {
                    "damaged" -> "Alert: The electric post in $barangay is currently damaged. Our team is working to resolve the issue as soon as possible. Please stay safe."
                    "working" -> "Update: The electric post in $barangay is now fully operational. Thank you for your patience."
                    "under repair" -> "Notice: The electric post in $barangay is currently under repair. Service will be restored shortly. We apologize for the inconvenience."
                    else -> "Notification: The electric post in $barangay has an unknown status. Please contact support for more information."
                }
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
        try {
            sms.sendTextMessage(phoneNumber, null, notificationMessage, null, null)
            Log.d(
                "DeviceNotifFragment",
                "SMS sent to: $phoneNumber with message: $notificationMessage"
            )
            Toast.makeText(requireContext(), "SMS sent to $phoneNumber", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e("DeviceNotifFragment", "Failed to send SMS to $phoneNumber: ${e.message}")
        }
    }

    private fun sendSms1(phoneNumbers: List<String>, notificationMessage: String) {
        val sms = SmsManager.getDefault()

        // Iterate through each phone number and send the SMS
        for (phoneNumber in phoneNumbers) {
            try {
                sms.sendTextMessage(phoneNumber, null, notificationMessage, null, null)
                Log.d(
                    "DeviceNotifFragment",
                    "SMS sent to: $phoneNumber with message: $notificationMessage"
                )
            } catch (e: Exception) {
                Log.e("DeviceNotifFragment", "Failed to send SMS to $phoneNumber: ${e.message}")
            }
        }

        // Show a toast message after attempting to send all SMS
        Toast.makeText(requireContext(), "SMS sent to all contacts", Toast.LENGTH_SHORT).show()
    }

    private fun sendSmsOtp(phoneNumbers: String, notificationMessage: String) {
        val sms = SmsManager.getDefault()

        sms.sendTextMessage(phoneNumbers, null, notificationMessage, null, null)
        Log.d(
            "DeviceNotifFragment",
            "SMS sent to: $phoneNumbers with message: $notificationMessage"
        )
    }


    override fun onDestroy() {
        super.onDestroy()
        notificationsListener?.remove()
    }

    private fun hasSmsPermission(): Boolean {
        return EasyPermissions.hasPermissions(
            requireContext(),
            android.Manifest.permission.SEND_SMS
        )
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