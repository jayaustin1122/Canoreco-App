package com.example.canorecoapp.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.canorecoapp.databinding.ItemPaymentBinding
import com.example.canorecoapp.databinding.ItemPaymentsBinding
import com.example.canorecoapp.models.PaymentInfo
import com.example.canorecoapp.utils.PaymentAttributes
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
class PaymentAdapter(private val paymentList: List<PaymentAttributes>) :
    RecyclerView.Adapter<PaymentAdapter.PaymentViewHolder>() {

    inner class PaymentViewHolder(val binding: ItemPaymentsBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PaymentViewHolder {
        val binding = ItemPaymentsBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PaymentViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PaymentViewHolder, position: Int) {
        val payment = paymentList[position]
        holder.binding.apply {
            tvCurrentBillValue.text = "${payment.amount / 100}"
            tvModeOfPaymentValue.text = payment.source.type.capitalize()
            tvAccountNumberValue.text = payment.description.takeIf { it.isNotBlank() }
                ?: "Wala Pa, di pa Na seset"
            tvStatus.text = payment.status.capitalize()
            tvPaymentDate.text = formatTimestamp(payment.created_at)
            tvBillMonth.text = getBillMonth(payment.created_at)
        }
    }

    private fun formatTimestamp(timestamp: Long): String {
        val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
        return sdf.format(Date(timestamp * 1000))
    }

    private fun getBillMonth(timestamp: Long): String {
        val sdf = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
        return "Bill Month: ${sdf.format(Date(timestamp * 1000))}"
    }


    override fun getItemCount(): Int = paymentList.size

}

