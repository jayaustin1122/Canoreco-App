package com.example.canorecoapp.utils

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.example.canorecoapp.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class NotificationWorker(context: Context, params: WorkerParameters) : Worker(context, params) {

    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private var notificationsListener: ListenerRegistration? = null

    override fun doWork(): Result {
        startListeningForNotifications()
        return Result.success()
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
            val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(notificationChannel)
        }

        val notificationBuilder = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(R.drawable.logo)
            .setContentTitle(title)
            .setContentText(timestamp)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setVibrate(longArrayOf(0, 500, 1000))

        with(NotificationManagerCompat.from(applicationContext)) {
            if (ActivityCompat.checkSelfPermission(
                    applicationContext,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
            notify(1, notificationBuilder.build())
        }
    }

    override fun onStopped() {
        super.onStopped()
        notificationsListener?.remove()
    }
}