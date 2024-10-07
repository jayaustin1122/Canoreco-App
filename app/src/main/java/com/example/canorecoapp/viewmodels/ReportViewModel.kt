package com.example.canorecoapp.viewmodels

import android.net.Uri
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

class ReportViewModel : ViewModel() {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val storage: FirebaseStorage = FirebaseStorage.getInstance()
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()

    // LiveData for upload status
    private val _uploadStatus = MutableLiveData<UploadStatus>()
    val uploadStatus: LiveData<UploadStatus> get() = _uploadStatus

    // Enum to represent different upload states
    sealed class UploadStatus {
        data object Idle : UploadStatus()
        data object UploadingImage : UploadStatus()
        data class ImageUploadSuccess(val imageUrl: String) : UploadStatus()
        data class ImageUploadFailure(val error: String) : UploadStatus()
        data object UploadingData : UploadStatus()
        data object DataUploadSuccess : UploadStatus()
        data class DataUploadFailure(val error: String) : UploadStatus()
    }

    // Function to upload image to Firebase Storage
    fun uploadImage(selectedImageUri: Uri) {
        val uid = auth.uid
        if (uid == null) {
            _uploadStatus.value = UploadStatus.ImageUploadFailure("User not authenticated.")
            return
        }

        _uploadStatus.value = UploadStatus.UploadingImage

        val reference = storage.reference.child("Complaint Images").child(uid).child(System.currentTimeMillis().toString())

        reference.putFile(selectedImageUri)
            .addOnSuccessListener {
                reference.downloadUrl.addOnSuccessListener { uri ->
                    _uploadStatus.value = UploadStatus.ImageUploadSuccess(uri.toString())
                }.addOnFailureListener { e ->
                    _uploadStatus.value = UploadStatus.ImageUploadFailure(e.message ?: "Failed to get download URL.")
                }
            }
            .addOnFailureListener { e ->
                _uploadStatus.value = UploadStatus.ImageUploadFailure(e.message ?: "Image upload failed.")
            }
    }

    fun uploadComplaint(
        report: String,
        concern: String,
        concernDescription: String,
        municipality: String,
        barangay: String,
        street: String,
        imageUrl: String
    ) {
        val uid = auth.uid
        if (uid == null) {
            _uploadStatus.value = UploadStatus.DataUploadFailure("User not authenticated.")
            return
        }

        _uploadStatus.value = UploadStatus.UploadingData

        val timestamp = System.currentTimeMillis() / 1000
        val address = "$barangay, $municipality $street"

        val complaintData = hashMapOf(
            "uid" to uid,
            "reportTitle" to report,
            "concern" to concern,
            "concernDescription" to concernDescription,
            "image" to imageUrl,
            "timestamp" to timestamp,
            "status" to "Sent",
            "address" to address
        )

        firestore.collection("users/$uid/my_complaints")
            .document(timestamp.toString())
            .set(complaintData)
            .addOnSuccessListener {
                // After successful upload, notify super admins
                notifySuperAdmins(report, concern, municipality, timestamp.toString())
            }
            .addOnFailureListener { e ->
                _uploadStatus.value = UploadStatus.DataUploadFailure(e.message ?: "Failed to upload complaint.")
            }
    }

    // Function to notify super admins about the new complaint
    private fun notifySuperAdmins(
        report: String,
        concern: String,
        municipality: String,
        timestamp: String
    ) {
        // Define the area mapping
        val areaMap = mapOf(
            1 to listOf(
                "Basud",
                "Mercedes",
                "Daet",
                "San Lorenzo Ruiz",
                "San Vicente",
                "Talisay",
                "Vinzons"
            ),
            2 to listOf("Labo", "Santa Elena", "Capalonga"),
            3 to listOf("Paracale", "Jose Panganiban")
        )

        val areaKey = areaMap.entries.find { it.value.contains(municipality) }?.key.toString()
        Log.d("UserViewModel", "Municipality: $municipality, Area Key: $areaKey")

        firestore.collection("users")
            .whereEqualTo("userType", "admin")
            .get()
            .addOnSuccessListener { adminSnapshot ->
                if (adminSnapshot.isEmpty) {
                    _uploadStatus.value = UploadStatus.DataUploadFailure("No admin users found.")
                    return@addOnSuccessListener
                }

                var adminFound = false
                adminSnapshot.documents.forEach { adminDoc ->
                    val userArea = adminDoc.getString("area")
                    if (userArea == areaKey) {
                        adminFound = true
                        val notificationData = hashMapOf(
                            "isRead" to false,
                            "status" to false,
                            "text" to concern,
                            "timestamp" to timestamp,
                            "title" to report
                        )
                        firestore.collection("users/${adminDoc.id}/notifications")
                            .document(timestamp)
                            .set(notificationData)
                            .addOnSuccessListener {
                                _uploadStatus.value = UploadStatus.DataUploadSuccess
                            }
                            .addOnFailureListener { e ->
                                _uploadStatus.value = UploadStatus.DataUploadFailure("Failed to notify admin: ${e.message}")
                            }
                    }
                }

                if (!adminFound) {
                   // _uploadStatus.value = UploadStatus.DataUploadFailure("No matching admin users found for the specified area.")
                }
            }
            .addOnFailureListener { e ->
                _uploadStatus.value = UploadStatus.DataUploadFailure("Error fetching admin users: ${e.message}")
            }
    }
}