package com.example.canorecoapp.adapter

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.navigation.NavController
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.canorecoapp.R
import com.example.canorecoapp.databinding.ItemViewsCentersBinding
import com.example.canorecoapp.models.Centers
import com.example.canorecoapp.views.user.bayadcenterandbusinesscenter.ViewMapMarkerClickFragment

class CentersAdapter(
    private val context: Context,
    private val navController: NavController,
    private var centerArrayList: List<Centers>,
    private var from: String?
): RecyclerView.Adapter<CentersAdapter.ViewHolder>() {
    private  lateinit var binding: ItemViewsCentersBinding

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var title: TextView = binding.tvTitle
        var date: TextView = binding.tvDate
        var image: ImageView = binding.ivThumbnail

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        binding = ItemViewsCentersBinding.inflate(LayoutInflater.from(context), parent, false)
        return ViewHolder(binding.root)
    }

    override fun getItemCount(): Int {
        return centerArrayList.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val model = centerArrayList[position]
        val locationName = model.locationName
        val barangay = model.barangay
        val municipality = model.municipality
        val lat = model.latitude
        val lng = model.longitude

        val imageUrl = R.drawable.icon_business_center
        val imageUrl2 = R.drawable.icon_payment_center
        if (from == "List Of Bayad Centers"){
            Glide.with(this@CentersAdapter.context)
                .load(imageUrl2)
                .error(R.drawable.logo) // In case of an error loading the image
                .into(holder.image)
            holder.itemView.setOnClickListener {
                val detailsFragment = ViewMapMarkerClickFragment()
                val bundle = Bundle().apply {
                    putString("lat", lat)
                    putString("lng", lng)
                    putString("from", "List Of Bayad Centers")

                }
                detailsFragment.arguments = bundle
                navController.navigate(R.id.viewMapMarkerClickFragment, bundle)
            }
        }
        else{
            Glide.with(this@CentersAdapter.context)
                .load(imageUrl)
                .error(R.drawable.logo) // In case of an error loading the image
                .into(holder.image)
            holder.itemView.setOnClickListener {
                val detailsFragment = ViewMapMarkerClickFragment()
                val bundle = Bundle().apply {
                    putString("lat", lat)
                    putString("lng", lng)
                    putString("from", "List Of Business Centers")
                }
                detailsFragment.arguments = bundle
                navController.navigate(R.id.viewMapMarkerClickFragment, bundle)
            }
        }
        holder.title.text = locationName

        holder.date.text = "$barangay, $municipality"



    }


}