package com.example.canorecoapp

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class NotificationService : Service() {
    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private var notificationsListener: ListenerRegistration? = null
    private lateinit var devicesRef: DatabaseReference
    override fun onCreate() {
        super.onCreate()
        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()
        startListeningForNotifications()
        startListeningForDeviceStatuses()
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
                            createNotification(title, timestamp.toString(),notificationsRef2,isRead)
                            notificationsRef2.update("isRead", true).addOnCompleteListener { updateTask ->
                                if (!updateTask.isSuccessful) {
                                    Log.e("NotificationService", "Failed to update isRead status", updateTask.exception)
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
            val notificationChannel = NotificationChannel(channelId, channelName, importance).apply {
                description = "Channel description"
                enableLights(true)
                lightColor = Color.RED
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 1000)
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(notificationChannel)
        }
        val intent = Intent(this, MainActivity::class.java).apply {
            updateIsRead(notificationsRef2,isRead)
            putExtra("navigate_to_fragment", "YourFragmentTag") // Replace with your fragment tag
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK

        }
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)

        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.logo)
            .setContentTitle(title)
            .setContentText(parseAndFormatDate(timestamp))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(false)
            .setVibrate(longArrayOf(0, 500, 1000))
            .setContentIntent(pendingIntent)

        with(NotificationManagerCompat.from(this)) {
            if (ActivityCompat.checkSelfPermission(
                    this@NotificationService,
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
                val userEmail = userDoc.getString("email") ?: "No Email"
                val notificationTitle = "Electric Post Damaged in $barangay"
                val notificationMessage = "A device in $barangay has been detected as damaged. Please check the system for more details."
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

    override fun onDestroy() {
        super.onDestroy()
        notificationsListener?.remove()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}
