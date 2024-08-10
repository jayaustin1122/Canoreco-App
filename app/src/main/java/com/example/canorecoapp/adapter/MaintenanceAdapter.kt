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
import com.example.canorecoapp.databinding.NewsActivitiesItemViewsBinding
import com.example.canorecoapp.databinding.NewsItemViewBinding
import com.example.canorecoapp.models.Maintenance
import com.example.canorecoapp.models.News

class MaintenanceAdapter(private val context: Context,
                         private val navController: NavController,
                         private var newsArrayList: List<Maintenance>
                ): RecyclerView.Adapter<MaintenanceAdapter.ViewHolder>() {
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

        holder.title.text = newsTitle
        holder.shortDescription.text = newsDesc
        holder.date.text = date

        Glide.with(this@MaintenanceAdapter.context)
            .load(image)
            .into(holder.image)

        holder.itemView.setOnClickListener {
//            val detailsFragment = FreeDetailsFragment()
//            val bundle = Bundle()
//            bundle.putString("timeStamp", timeStamp)
//            bundle.putString("uid", uid)
//            bundle.putString("image", imageselected)
//            bundle.putString("text", text.toString())
//            detailsFragment.arguments = bundle
//            Log.d("BundleValues", "TimeStamp: $timeStamp")
//            Log.d("BundleValues", "UID: $uid")
//            Log.d("BundleValues", "Image: $imageselected")
//            navController.navigate(R.id.freeDetailsFragment, bundle)
        }
    }


}