package com.example.canorecoapp.adapter

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
import com.example.canorecoapp.databinding.NewsItemViewBinding
import com.example.canorecoapp.models.Maintenance
import com.example.canorecoapp.views.user.news.NewsDetailsFragment

class MaintenanceListAdapter(private val context: Context,
                             private val navController: NavController,
                             private var maintenanceList: List<Maintenance>
                ): RecyclerView.Adapter<MaintenanceListAdapter.ViewHolder>(), Filterable {
    private  lateinit var binding: NewsItemViewBinding
    private var filteredList = maintenanceList
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
        val model = maintenanceList[position]
        val newsTitle = model.title
        val newsDesc = model.shortDescription
        val image = model.image
        val date = model.date
        val timeStamp = model.timestamp
        val category = model.category
        val from = "Maintenance"
        holder.title.text = newsTitle
        holder.date.text = date

        Glide.with(this@MaintenanceListAdapter.context)
            .load(image)
            .into(holder.image)

        holder.itemView.setOnClickListener {
            val detailsFragment = NewsDetailsFragment()
            val bundle = Bundle().apply {
                putString("title", newsTitle)
                putString("from", from)
            }
            detailsFragment.arguments = bundle
            Log.d("maintenanceList", "Category: $category $from")
            navController.navigate(R.id.newsDetailsFragment, bundle)
        }
    }
    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(constraint: CharSequence?): FilterResults {
                val charString = constraint?.toString() ?: ""
                val results = FilterResults()

                results.values = if (charString.isEmpty()) {
                    maintenanceList
                } else {
                    val filtered = maintenanceList.filter {
                        it.title.contains(charString, ignoreCase = true) ||
                                it.shortDescription.contains(charString, ignoreCase = true)
                    }
                    filtered
                }
                return results
            }

            override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                filteredList = results?.values as List<Maintenance>
                notifyDataSetChanged()
            }
        }
    }



}