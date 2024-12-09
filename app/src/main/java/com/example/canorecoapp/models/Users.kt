package com.example.canorecoapp.models

data class Users(
    val access: Boolean,
    val barangay: String,
    val dateOfBirth: String,
    val email: String,
    val authEmail: String,
    val firstName: String,
    val image: String,
    val lastName: String,
    val municipality: String,
    val password: String,
    val phone: String,
    val timestamp: Long,
    val token: String,
    val uid: String,
    val userType: String,
    val accountNumber: String,
    val position: String,
    val area: String
)
