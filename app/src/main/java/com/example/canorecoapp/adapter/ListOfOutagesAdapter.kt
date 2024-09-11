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
import com.example.canorecoapp.views.user.news.ViewMapsWithAreasFragment
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException

class   ListOfOutagesAdapter(
    private val context: Context,
    private val navController: NavController,
    private var selectedLocations: List<String>
) : RecyclerView.Adapter<ListOfOutagesAdapter.ViewHolder>(), Filterable {

    private lateinit var binding: NewsItemViewBinding
    private var filteredList = selectedLocations

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var selectedLocationsText: TextView = binding.tvTitle
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
        val location = filteredList[position]

        holder.date.visibility = View.GONE
        holder.image.visibility = View.GONE
        retrieveLocationName(location, holder)
    }
    private fun loadJsonFromRaw(resourceId: Int): String? {
        return try {
            val inputStream =context.resources.openRawResource(resourceId)
            inputStream.bufferedReader().use { it.readText() }
        } catch (e: IOException) {
            Log.e("JSON", "Error reading JSON file from raw resources: ${e.message}")
            null
        }
    }
    private fun retrieveLocationName(location: String, holder: ViewHolder) {
        try {
            val jsonData = loadJsonFromRaw(R.raw.filtered_barangayss)
            val jsonObject = JSONObject(jsonData)
            val features = jsonObject.getJSONArray("features")

            for (i in 0 until features.length()) {
                val feature = features.getJSONObject(i)
                val properties = feature.getJSONObject("properties")
                val barangayId = properties.getString("ID_3")
                val barangayName = properties.getString("NAME_3")

                if (barangayId == location) {
                    holder.selectedLocationsText.text = barangayName
                    holder.itemView.setOnClickListener {
                        val detailsFragment = ViewMapsWithAreasFragment()
                        val bundle = Bundle()
                        bundle.putString("Areas", location)
                        detailsFragment.arguments = bundle
                        navController.navigate(R.id.viewMapsWithAreasFragment, bundle)
                    }
                    break
                }

            }
        } catch (e: JSONException) {
            Log.e("JSON", "Error parsing JSON data: ${e.message}")
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
                        it.contains(charString, ignoreCase = true)
                    }
                    filtered
                }
                return results
            }

            override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                filteredList = results?.values as List<String>
                notifyDataSetChanged()
            }
        }
    }
    fun updateList(newList: List<String>) {
        selectedLocations = newList
        filteredList = newList
        notifyDataSetChanged()
    }
}
