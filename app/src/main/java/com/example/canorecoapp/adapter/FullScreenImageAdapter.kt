package com.example.canorecoapp.adapter


import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.canorecoapp.databinding.ItemFullScreenImageBinding

class FullScreenImageAdapter(private val imageList: List<String>) :
    RecyclerView.Adapter<FullScreenImageAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemFullScreenImageBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemFullScreenImageBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val imageUrl = imageList[position]

        // Load image using Glide
        Glide.with(holder.itemView.context)
            .load(imageUrl)
            .into(holder.binding.fullScreenImageView)
    }

    override fun getItemCount(): Int {
        return imageList.size
    }
}
