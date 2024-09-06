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
import com.example.canorecoapp.databinding.NewsImagesBinding
import com.example.canorecoapp.databinding.NewsItemViewBinding
import com.example.canorecoapp.models.Images
import com.example.canorecoapp.models.Maintenance
import com.example.canorecoapp.models.News
import com.example.canorecoapp.views.user.news.NewsDetailsFragment
import org.bouncycastle.asn1.x500.style.RFC4519Style.title

class NewsImagesAdapter(private val context: Context,
                        private val navController: NavController,
                        private var newsArrayList: List<String>
                ): RecyclerView.Adapter<NewsImagesAdapter.ViewHolder>() {
    private  lateinit var binding: NewsImagesBinding
    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        var image: ImageView = binding.imageBackground

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        binding = NewsImagesBinding.inflate(LayoutInflater.from(context), parent, false)
        return ViewHolder(binding.root)
    }

    override fun getItemCount(): Int {
        return newsArrayList.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        val imageUrl = newsArrayList[position]
        Log.d("NewsImagesAdapter", "Loading image from URL: $imageUrl")
        Glide.with(holder.itemView.context)
            .load(imageUrl)
            .error(R.drawable.logo) // Optional error image
            .into(holder.image)



//        holder.itemView.setOnClickListener {
//            holder.itemView.setOnClickListener {
//                val detailsFragment = NewsDetailsFragment()
//                val bundle = Bundle()
//                bundle.putString("title",newsTitle)
//                detailsFragment.arguments = bundle
//                Log.d("BundleValues", "TimeStamp: $newsTitle")
//                navController.navigate(R.id.newsDetailsFragment, bundle)
//            }
//        }
    }


}