package com.example.canorecoapp.views.user.complaints

data class Complaint(
    val reportTitle: String,
    val timestamp: String,
    val status: String,
    val concern: String,
    val concernDescription: String,
    val address: String,
    val image: String
)
