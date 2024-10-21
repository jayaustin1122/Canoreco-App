package com.example.canorecoapp.viewmodels
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.database.*

class DeviceViewModel : ViewModel() {
    private val database: DatabaseReference = FirebaseDatabase.getInstance().getReference("devices")

    val device9000Status: MutableLiveData<String> = MutableLiveData()
    val device9001Status: MutableLiveData<String> = MutableLiveData()

    init {
        // Set up listeners for real-time updates
        listenToDeviceStatus("9000", device9000Status)
        listenToDeviceStatus("9001", device9001Status)
    }

    private fun listenToDeviceStatus(deviceId: String, statusLiveData: MutableLiveData<String>) {
        database.child(deviceId).child("status").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val status = snapshot.getValue(String::class.java) ?: "unknown"
                statusLiveData.value = status
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle possible errors
            }
        })
    }

    fun toggleDeviceStatus(deviceId: String, currentStatus: String) {
        val newStatus = if (currentStatus == "damaged") "working" else "damaged"
        database.child(deviceId).child("status").setValue(newStatus)
    }
}
