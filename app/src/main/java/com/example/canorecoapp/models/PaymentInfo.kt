package com.example.canorecoapp.models

data class PaymentInfo(
    val id: String,
    val type: String,
    val amount: Int,
    val currency: String,
    val description: String,
    val status: String,
    val checkoutUrl: String,
    val referenceNumber: String,
    val createdAt: Long,
    val updatedAt: Long
)
