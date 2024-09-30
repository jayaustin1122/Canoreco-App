package com.example.canorecoapp.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.canorecoapp.databinding.ItemPaymentBinding
import com.example.canorecoapp.models.PaymentInfo
import com.example.canorecoapp.utils.PaymentAttributes
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
class PaymentAdapter(private val paymentList: List<PaymentAttributes>) :
    RecyclerView.Adapter<PaymentAdapter.PaymentViewHolder>() {

    inner class PaymentViewHolder(val binding: ItemPaymentBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PaymentViewHolder {
        val binding = ItemPaymentBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PaymentViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PaymentViewHolder, position: Int) {
        val payment = paymentList[position]
        holder.binding.apply {
            tvAmount.text = "${payment.amount / 100}"
            tvEmail.text = "${payment.billing.email}"
            tvPhone.text = "${payment.billing.phone}"
            tvStatus.text = "${payment.status.capitalize()}"
            tvDescription.text = "${payment.description}"
            tvReferenceNumber.text = "${payment.external_reference_number}"
            tvCreatedAt.text = "${formatTimestamp(payment.created_at)}"
        }
    }

    override fun getItemCount(): Int = paymentList.size

    // Helper function to format timestamp
    private fun formatTimestamp(timestamp: Long): String {
        val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
        return sdf.format(Date(timestamp * 1000)) // Convert to milliseconds
    }
}

