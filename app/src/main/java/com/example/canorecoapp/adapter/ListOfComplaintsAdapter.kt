package com.example.canorecoapp.adapter

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.canorecoapp.databinding.NewsItemViewBinding
import android.widget.Filter
import android.widget.Filterable
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import com.example.canorecoapp.R
import com.example.canorecoapp.databinding.ItemComplaintsBinding
import com.example.canorecoapp.models.News
import com.example.canorecoapp.views.user.complaints.ViewCompaintFragment
import com.example.canorecoapp.views.user.news.NewsDetailsFragment
import com.example.canorecoapp.views.user.news.ViewMapsWithAreasFragment
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException

class   ListOfComplaintsAdapter(
    private val context: Context,
    private val navController: NavController,
    private var selectedLocations: List<News>
) : RecyclerView.Adapter<ListOfComplaintsAdapter.ViewHolder>(), Filterable {

    private lateinit var binding: ItemComplaintsBinding
    private var filteredList = selectedLocations

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var selectedLocationsText: TextView = binding.tvTitle
        var status: TextView = binding.status

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        binding = ItemComplaintsBinding.inflate(LayoutInflater.from(context), parent, false)
        return ViewHolder(binding.root)
    }

    override fun getItemCount(): Int {
        return filteredList.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val model = selectedLocations[position]
        val title = model.title
        val status = model.category

        holder.selectedLocationsText.text = title
        holder.status.text = status

        holder.itemView.setOnClickListener {
            val detailsFragment = ViewCompaintFragment()
            val bundle = Bundle().apply {
                putString("timestamp", model.timestamp)
            }
            detailsFragment.arguments = bundle
            navController.navigate(R.id.viewCompaintFragment, bundle)
        }

        when (status) {
            "Sent" -> holder.status.setTextColor(context.getColor(R.color.g_green))
            "Review" -> holder.status.setTextColor(context.getColor(R.color.g_orange_yellow))
            "Processing" -> holder.status.setTextColor(context.getColor(R.color.g_blue2))
            else -> holder.status.setTextColor(context.getColor(android.R.color.black))
        }
    }


    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(constraint: CharSequence?): FilterResults {
                val charString = constraint?.toString() ?: ""
                val results = FilterResults()

                results.values = if (charString.isEmpty()) {
                    selectedLocations
                } else {
                    val filtered = selectedLocations.filter {
                        it.title.contains(charString, ignoreCase = true)
                    }
                    filtered
                }
                return results
            }

            override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                filteredList = results?.values as List<News>
                notifyDataSetChanged()
            }
        }
    }

    fun updateList(newList: List<News>) {
        selectedLocations = newList
        filteredList = newList
        notifyDataSetChanged()
    }
}
