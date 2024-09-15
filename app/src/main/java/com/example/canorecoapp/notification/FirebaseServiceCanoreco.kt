package com.example.bidnshare.notification


import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.NotificationManager.IMPORTANCE_HIGH
import android.app.PendingIntent
import android.app.PendingIntent.FLAG_ONE_SHOT
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.example.canorecoapp.MainActivity
import com.example.canorecoapp.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlin.random.Random
private const val CHANNEL_ID = "my_channel"

class FirebaseServiceCanoreco : FirebaseMessagingService() {

    companion object {
        var sharedPref: SharedPreferences? = null

        var token: String?
            get() {
                return sharedPref?.getString("token", "")
            }
            set(value) {
                sharedPref?.edit()?.putString("token", value)?.apply()
            }
    }

    override fun onNewToken(newToken: String) {
        super.onNewToken(newToken)
        token = newToken
    }


    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        val title = message.data["notificationTitle"] ?: "Canoreco App"
        val text = message.data["notificationText"] ?: "Read Now the Latest Maintenance and Advisory"

        val intent = Intent(this, MainActivity::class.java)
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notificationID = Random.nextInt()
        saveNotificationToFirestore(title, text)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel(notificationManager)
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pendingIntent = PendingIntent.getActivity(this, 0, intent,
            FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE)

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(R.drawable.logo)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(notificationID, notification)
    }
    private fun saveNotificationToFirestore(title: String, text: String) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid != null) {
            val db = FirebaseFirestore.getInstance()
            val notificationData = hashMapOf(
                "title" to title,
                "text" to text,
                "timestamp" to System.currentTimeMillis(),
                "status" to "unread"
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

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel(notificationManager: NotificationManager) {
        val channelName = "channelName"
        val channel = NotificationChannel(CHANNEL_ID, channelName, IMPORTANCE_HIGH).apply {
            description = "My channel Description"
            enableLights(true)
            lightColor = Color.MAGENTA
        }
        notificationManager.createNotificationChannel(channel)
    }


}