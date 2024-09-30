package com.example.canorecoapp.utils

data class ApiResponse(
    val data: Data
)

data class Data(
    val id: String,
    val type: String,
    val attributes: Attributes
)

data class Attributes(
    val amount: Int,
    val archived: Boolean,
    val currency: String,
    val description: String,
    val livemode: Boolean,
    val fee: Int,
    val remarks: String,
    val status: String,
    val reference_number: String,
    val created_at: Long,
    val updated_at: Long,
    val payments: List<Payment>
)

data class Payment(
    val data: PaymentData
)

data class PaymentData(
    val id: String,
    val type: String,
    val attributes: PaymentAttributes
)

data class PaymentAttributes(
    val amount: Int,
    val billing: Billing,
    val description: String,
    val external_reference_number: String,
    val fee: Int,
    val net_amount: Int,
    val source: PaymentSource,
    val status: String,
    val created_at: Long
)

data class Billing(
    val address: Address,
    val email: String,
    val name: String,
    val phone: String
)

data class Address(
    val city: String,
    val country: String,
    val line1: String,
    val line2: String,
    val postal_code: String,
    val state: String
)

data class PaymentSource(
    val id: String,
    val type: String
)
