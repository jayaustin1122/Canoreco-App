package com.example.canorecoapp.adapter

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

class NewsAdapter(private val context: Context,
                  private val navController: NavController,
                  private var newsArrayList: List<News>
                ): RecyclerView.Adapter<NewsAdapter.ViewHolder>() {
    private  lateinit var binding: NewsActivitiesItemViewsBinding
    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var title: TextView = binding.tvActivityTitle
        var moreBtn: ImageView = binding.moreBtn
        var date: TextView = binding.tvActivityDate
        var image: ImageView = binding.imageBackground
        var shortDescription: TextView = binding.tvActivityDescription
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        binding = NewsActivitiesItemViewsBinding.inflate(LayoutInflater.from(context), parent, false)
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

        holder.title.text = newsTitle
        holder.shortDescription.text = newsDesc
        holder.date.text = date

        Glide.with(this@NewsAdapter.context)
            .load(image)
            .into(holder.image)

        holder.itemView.setOnClickListener {
            val detailsFragment = NewsDetailsFragment()
            val bundle = Bundle()
            bundle.putString("timestamp", timeStamp)
            detailsFragment.arguments = bundle
            Log.d("BundleValues", "TimeStamp: $timeStamp")
            navController.navigate(R.id.newsDetailsFragment, bundle)
        }
    }


}