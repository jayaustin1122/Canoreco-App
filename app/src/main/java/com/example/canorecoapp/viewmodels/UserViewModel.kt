package com.example.canorecoapp.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.canorecoapp.models.Users
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class UserViewModel : ViewModel() {

    private val _userInfo = MutableLiveData<Users?>()
    val userInfo: LiveData<Users?> get() = _userInfo

    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String> get() = _errorMessage

    fun loadUserInfo() {
        val db = FirebaseFirestore.getInstance()
        val currentUser = FirebaseAuth.getInstance().currentUser

        if (currentUser != null) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val querySnapshot = db.collection("users")
                        .whereEqualTo("uid", currentUser.uid)
                        .get().await()

                    if (!querySnapshot.isEmpty) {
                        val document = querySnapshot.documents.firstOrNull()

                        document?.let {
                            val access = it.getBoolean("access") ?: false
                            val barangay = it.getString("barangay") ?: ""
                            val dateOfBirth = it.getString("dateOfBirth") ?: ""
                            val email = it.getString("email") ?: ""
                            val firstName = it.getString("firstName") ?: ""
                            val image = it.getString("image") ?: ""
                            val lastName = it.getString("lastName") ?: ""
                            val municipality = it.getString("municipality") ?: ""
                            val password = it.getString("password") ?: ""
                            val phone = it.getString("phone") ?: ""
                            val timestamp = it.getLong("timestamp") ?: 0L
                            val token = it.getString("token") ?: ""
                            val uid = it.getString("uid") ?: ""
                            val userType = it.getString("userType") ?: ""

                            val user = Users(
                                access = access,
                                barangay = barangay,
                                dateOfBirth = dateOfBirth,
                                email = email,
                                firstName = firstName,
                                image = image,
                                lastName = lastName,
                                municipality = municipality,
                                password = password,
                                phone = phone,
                                timestamp = timestamp,
                                token = token,
                                uid = uid,
                                userType = userType
                            )

                            withContext(Dispatchers.Main) {
                                _userInfo.value = user
                            }
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            _errorMessage.value = "No user data found"
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        _errorMessage.value = e.message
                    }
                }
            }
        } else {
            _errorMessage.value = "User not authenticated"
        }
    }

}