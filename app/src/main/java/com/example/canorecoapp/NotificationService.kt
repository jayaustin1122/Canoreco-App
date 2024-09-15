package com.example.canorecoapp

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.IBinder
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class NotificationService : Service() {
    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private var notificationsListener: ListenerRegistration? = null

    override fun onCreate() {
        super.onCreate()
        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()
        startListeningForNotifications()
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

            snapshot?.let {
                for (document in it.documentChanges) {
                    if (document.type == com.google.firebase.firestore.DocumentChange.Type.ADDED) {
                        val title = document.document.getString("title") ?: "No Title"
                        val timestamp = document.document.getDouble("timestamp")
                        createNotification(title, timestamp.toString())
                    }
                }
            }
        }
    }

    private fun createNotification(title: String, timestamp: String) {
        val channelId = "test_channel_id"
        val channelName = "Test Channel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_DEFAULT
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

        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.logo)
            .setContentTitle(title)
            .setContentText(timestamp)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setVibrate(longArrayOf(0, 500, 1000))

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

    override fun onDestroy() {
        super.onDestroy()
        notificationsListener?.remove()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}
