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

    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> get() = _loading

    fun loadUserInfo() {
        val db = FirebaseFirestore.getInstance()
        val currentUser = FirebaseAuth.getInstance().currentUser

        if (currentUser != null) {
            _loading.postValue(true) // Start loading
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val querySnapshot = db.collection("users")
                        .whereEqualTo("uid", currentUser.uid)
                        .get().await()

                    if (!querySnapshot.isEmpty) {
                        val document = querySnapshot.documents.firstOrNull()

                        document?.let {
                            val user = Users(
                                access = it.getBoolean("access") ?: false,
                                barangay = it.getString("barangay") ?: "",
                                dateOfBirth = it.getString("dateOfBirth") ?: "",
                                email = it.getString("email") ?: "",
                                authEmail = it.getString("authEmail") ?: "",
                                firstName = it.getString("firstName") ?: "",
                                image = it.getString("image") ?: "",
                                lastName = it.getString("lastName") ?: "",
                                municipality = it.getString("municipality") ?: "",
                                password = it.getString("password") ?: "",
                                phone = it.getString("phone") ?: "",
                                timestamp = it.getLong("timestamp") ?: 0L,
                                token = it.getString("token") ?: "",
                                uid = it.getString("uid") ?: "",
                                userType = it.getString("userType") ?: ""
                            )

                            withContext(Dispatchers.Main) {
                                _userInfo.value = user
                                _loading.value = false // Stop loading
                            }
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            _errorMessage.value = "No user data found"
                            _loading.value = false // Stop loading
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        _errorMessage.value = e.message
                        _loading.value = false // Stop loading on error
                    }
                }
            }
        } else {
            _errorMessage.value = "User not authenticated"
            _loading.value = false // Stop loading if no user
        }
    }

}