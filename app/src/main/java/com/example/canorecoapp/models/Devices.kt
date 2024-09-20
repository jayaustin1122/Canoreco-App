package com.example.canorecoapp.models

data class Devices(
    val assigned: String,
    val barangay: String,
    val date: String,
    val endTime: String,
    val id: String,
    val latitude: Double,
    val locationName: String,
    val longitude: Double,
    val startTime: String,
    val status: String,
    val timestamp: Long
)
