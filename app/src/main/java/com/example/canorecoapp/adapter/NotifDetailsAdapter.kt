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
import androidx.navigation.NavController
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.canorecoapp.R
import com.example.canorecoapp.databinding.NewsActivitiesItemViewsBinding
import com.example.canorecoapp.databinding.NewsItemViewBinding
import com.example.canorecoapp.models.Maintenance
import com.example.canorecoapp.models.News
import com.example.canorecoapp.models.Notif
import com.example.canorecoapp.views.user.news.NewsDetailsFragment
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
        val text = model.text
        val timeStamp = model.timestamp
        val formattedDate = parseAndFormatDate(timeStamp)
        holder.title.text = newsTitle
        holder.date.text = formattedDate
        holder.image.visibility = View.GONE

        holder.itemView.setOnClickListener {
            Log.d("adapteradapter", "$formattedDate")
        }
    }

    @SuppressLint("SimpleDateFormat")
    private fun parseAndFormatDate(timestampString: String): String {
        return try {
            val timestampMilliseconds = timestampString.toDoubleOrNull()?.toLong() ?: return ""
            val date = Date(timestampMilliseconds)
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