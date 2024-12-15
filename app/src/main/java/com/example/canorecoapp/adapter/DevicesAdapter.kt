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
import com.example.canorecoapp.models.Devices
import com.example.canorecoapp.models.Maintenance
import com.example.canorecoapp.views.linemen.tasks.TasksDetailsFragment
import com.example.canorecoapp.views.user.news.NewsDetailsFragment

class DevicesAdapter(private val context: Context,
                     private val navController: NavController,
                     private val childFragmentManager: androidx.fragment.app.FragmentManager,
                     private var deviceLists: List<Devices>
                ): RecyclerView.Adapter<DevicesAdapter.ViewHolder>(), Filterable {
    private  lateinit var binding: NewsItemViewBinding
    private var filteredList = deviceLists
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
        val model = deviceLists[position]
        val locationName = model.locationName
        val status = model.status
        val latitude = model.latitude
        val longitude = model.longitude
        val startTime = model.startTime
        val endTime = model.endTime
        val dataKey = model.id
        val barangay = model.barangay
        val assigned = model.assigned
        holder.title.text = locationName
        holder.date.text = status



        holder.itemView.setOnClickListener {
        val detailsDialog = TasksDetailsFragment()
        val bundle = Bundle().apply {
            putString("locationName", locationName)
            putDouble("latitude", latitude ?: 0.0)
            putDouble("longitude", longitude ?: 0.0)
            putString("startTime", startTime)
            putString("endTime", endTime)
            putString("barangay", barangay)
            putString("status", "Status: $status")
            putString("id", dataKey)
            putString("assigned", assigned)
        }
        detailsDialog.arguments = bundle
        detailsDialog.show(childFragmentManager, "TasksDetailsFragment")
        }
    }
    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(constraint: CharSequence?): FilterResults {
                val charString = constraint?.toString() ?: ""
                val results = FilterResults()

                results.values = if (charString.isEmpty()) {
                    deviceLists
                } else {
                    val filtered = deviceLists.filter {
                        it.locationName.contains(charString, ignoreCase = true) ||
                                it.status.contains(charString, ignoreCase = true)
                    }
                    filtered
                }
                return results
            }

            override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                filteredList = results?.values as List<Devices>
                notifyDataSetChanged()
            }
        }
    }



}