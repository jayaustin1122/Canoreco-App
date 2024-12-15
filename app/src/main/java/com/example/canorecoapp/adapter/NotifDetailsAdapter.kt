package com.example.canorecoapp.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.recyclerview.widget.RecyclerView
import com.example.canorecoapp.R
import com.example.canorecoapp.databinding.NewsItemViewBinding
import com.example.canorecoapp.models.Notif
import com.example.canorecoapp.views.user.news.NewsDetailsFragment
import com.example.canorecoapp.views.user.notif.NotificationBottomSheetDialogFragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class NotifDetailsAdapter(private val context: Context,
                          private val navController: NavController,
                          private var notifArrayList: List<Notif>
                ): RecyclerView.Adapter<NotifDetailsAdapter.ViewHolder>(), Filterable {
    private  lateinit var binding: NewsItemViewBinding
    private var filteredList = notifArrayList
    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var title: TextView = binding.tvTitle
        var date: TextView = binding.tvDate
        var image: ImageView = binding.ivThumbnail
        var indicator: ImageView = binding.imgMessageIndicator

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        binding = NewsItemViewBinding.inflate(LayoutInflater.from(context), parent, false)
        return ViewHolder(binding.root)
    }

    override fun getItemCount(): Int {
        return filteredList.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val model = notifArrayList[position]
        val newsTitle = model.title
        val text = model.message
        val timeStamp = model.timestamp
        val status = model.status
        val isFromDevice = model.isFromDevice
        val formattedDate = parseAndFormatDate(timeStamp)

        holder.title.text = newsTitle
        holder.date.text = formattedDate
        holder.image.visibility = View.GONE
        holder.indicator.visibility = if (!status) View.VISIBLE else View.GONE


        holder.itemView.setOnClickListener {
            updateNotifStatus(timeStamp)

            // Log click with full notification data
            Log.d(
                "NotificationAdapter",
                """
            Notification Clicked:
            Title: $newsTitle
            Text: $text
            Timestamp: $timeStamp
            Formatted Date: $formattedDate
            Is Read: $status
            Is From Device: $isFromDevice
            """.trimIndent()
            )

            if (isFromDevice) {
                val materialDialog = NotificationBottomSheetDialogFragment.newInstance(
                    title = newsTitle,
                    text = text,
                    isFromDevice = isFromDevice,
                    date = formattedDate
                )
                materialDialog.show((context as AppCompatActivity).supportFragmentManager, "NotificationDialog")
            } else {
                // Navigate to NewsDetailsFragment for non-device notifications
                val detailsFragment = NewsDetailsFragment()
                val bundle = Bundle().apply {
                    putString("title", timeStamp)
                    putString("from", "News")
                }
                detailsFragment.arguments = bundle
                navController.navigate(R.id.newsDetailsFragment, bundle)
            }
        }
    }




    fun updateNotifStatus(timeStamp: String) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val db: FirebaseFirestore = FirebaseFirestore.getInstance()

        val notificationsRef = db.collection("users")
            .document(userId)
            .collection("notifications")
            .document(timeStamp)

        notificationsRef.update("status", true)
            .addOnCompleteListener { updateTask ->
                if (updateTask.isSuccessful) {
                    Log.d("NotificationService", "Notification status updated successfully")
                } else {
                    Log.e("NotificationService", "Failed to update isRead status", updateTask.exception)
                }
            }
    }


    @SuppressLint("SimpleDateFormat")
    private fun parseAndFormatDate(timestampString: String): String {
        return try {

            val timestamp = timestampString.toDoubleOrNull()?.toLong() ?: return ""
            val date = Date(timestamp * 1000)
            val outputFormat = SimpleDateFormat("MMMM d, yyyy h:mm a", Locale.getDefault())
            outputFormat.format(date)
        } catch (e: NumberFormatException) {
            Log.e("Home", "Number format error: ", e)
            ""
        }
    }
    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(constraint: CharSequence?): FilterResults {
                val charString = constraint?.toString() ?: ""
                val results = FilterResults()

                results.values = if (charString.isEmpty()) {
                    notifArrayList
                } else {
                    val filtered = notifArrayList.filter {
                        it.title.contains(charString, ignoreCase = true) ||
                                it.title.contains(charString, ignoreCase = true)
                    }
                    filtered
                }
                return results
            }

            override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                filteredList = results?.values as List<Notif>
                notifyDataSetChanged()
            }
        }
    }

}