package com.example.canorecoapp.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.navigation.NavController
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.canorecoapp.R
import com.example.canorecoapp.databinding.NewsActivitiesItemViewsBinding
import com.example.canorecoapp.databinding.NewsItemViewBinding
import com.example.canorecoapp.models.News
import com.example.canorecoapp.views.user.news.NewsDetailsFragment
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class NewsDetailsAdapter(private val context: Context,
                         private val navController: NavController,
                         private var newsArrayList: List<News>
                ): RecyclerView.Adapter<NewsDetailsAdapter.ViewHolder>() {
    private  lateinit var binding: NewsItemViewBinding
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
        return newsArrayList.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val model = newsArrayList[position]
        val newsTitle = model.title
        val newsDesc = model.shortDescription
        val image = model.image
        val date = model.date
        val timeStamp = model.timestamp
        val category = model.category
        val formattedDate = parseAndFormatDate(timeStamp)
        holder.title.text = newsTitle
        holder.date.text = formattedDate
        val from = "News"
        Glide.with(this@NewsDetailsAdapter.context)
            .load(image)
            .into(holder.image)

        holder.itemView.setOnClickListener {
            val detailsFragment = NewsDetailsFragment()
            val bundle = Bundle().apply {
                putString("category", category)
                putString("from", from)
            }
            detailsFragment.arguments = bundle
            Log.d("maintenanceList", "Category: $category")
            navController.navigate(R.id.newsDetailsFragment, bundle)
        }
    }
    @SuppressLint("SimpleDateFormat")
    private fun parseAndFormatDate(timestampString: String): String {
        return try {
            val timestampSeconds = timestampString.toLongOrNull() ?: return ""
            val date = Date(timestampSeconds * 1000)
            val outputFormat = SimpleDateFormat("MMMM d, yyyy h:mm a", Locale.getDefault())
            outputFormat.format(date)
        } catch (e: NumberFormatException) {
            Log.e("Home", "Number format error: ", e)
            ""
        }

    }


}