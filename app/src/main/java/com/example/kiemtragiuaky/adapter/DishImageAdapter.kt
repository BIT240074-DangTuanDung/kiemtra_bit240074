package com.example.kiemtragiuaky.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.kiemtragiuaky.R
import com.example.kiemtragiuaky.databinding.ItemDishImageDetailBinding
import com.example.kiemtragiuaky.model.DishImage

class DishImageAdapter(
    private val images: List<DishImage>,
) : RecyclerView.Adapter<DishImageAdapter.ImageViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val binding = ItemDishImageDetailBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false,
        )
        return ImageViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        holder.bind(images[position])
    }

    override fun getItemCount(): Int = images.size

    inner class ImageViewHolder(
        private val binding: ItemDishImageDetailBinding,
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(dishImage: DishImage) {
            Glide.with(binding.root.context)
                .load(dishImage.imageUrl)
                .placeholder(R.drawable.ic_launcher_background)
                .error(R.drawable.ic_launcher_background)
                .centerCrop()
                .into(binding.imgDetailItem)
        }
    }
}
