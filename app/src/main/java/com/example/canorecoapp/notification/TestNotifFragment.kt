package com.example.canorecoapp.notification

import android.content.ContentValues
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.example.bidnshare.notification.NotificationData
import com.example.bidnshare.notification.PushNotification
import com.example.bidnshare.notification.RetrofitInstance
import com.example.canorecoapp.R
import com.example.canorecoapp.databinding.FragmentTestNotifBinding
import com.example.canorecoapp.views.signups.TOPIC
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await


class TestNotifFragment : Fragment() {
    private lateinit var binding : FragmentTestNotifBinding
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentTestNotifBinding.inflate(layoutInflater)
        // Inflate the layout for this fragment
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnTest.setOnClickListener {
            PushNotification(
                NotificationData("New Data", "Sample Push NOtif"),
                "fJypwSHaRN6dIFcjERCxOc:APA91bHKO43GGykAebVCQaB1Tm2rm1lFN05er784B1ZiNeba_TBw6P-pPDrXkI8yfsl04faNtlsA1V_DjIyTrFb-ZlWCYuDfYqtz4d-KQMKP-NpLFZtO2iTilIRzYM99NvWlnrRsNfs-"
            ).also {
                sendNotification(it)
                val fcmToken = FirebaseMessaging.getInstance().token
                Toast.makeText(this@TestNotifFragment.requireContext(),"$fcmToken",Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun sendNotification(notification: PushNotification) = CoroutineScope(Dispatchers.IO).launch {
        try {
            val response = RetrofitInstance.api.postNotification(notification)
            if (response.isSuccessful) {
                Log.d("NOtif", "Notification sent successfully")
            } else {
                Log.e("NOtif", "Failed to send notification. Error: ${response.errorBody().toString()}")
            }
        } catch (e: Exception) {
            Log.e(ContentValues.TAG, "Error sending notification: ${e.toString()}")
        }
    }
}