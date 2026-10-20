package com.example.kiemtragiuaky.adapter

import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.kiemtragiuaky.DetailActivity
import com.example.kiemtragiuaky.R
import com.example.kiemtragiuaky.databinding.ItemDishBinding
import com.example.kiemtragiuaky.model.Dish

class DishAdapter(
    private var dishes: List<Dish> = emptyList(),
    private val onItemClick: ((Dish) -> Unit)? = null,
) : RecyclerView.Adapter<DishAdapter.DishViewHolder>() {

    fun updateData(newDishes: List<Dish>) {
        this.dishes = newDishes
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DishViewHolder {
        val binding = ItemDishBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false,
        )
        return DishViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DishViewHolder, position: Int) {
        holder.bind(dishes[position])
    }

    override fun getItemCount(): Int = dishes.size

    inner class DishViewHolder(
        private val binding: ItemDishBinding,
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(dish: Dish) {
            // 1. Hiển thị thông tin món ăn
            binding.tvDishName.text = dish.BIT240074_Name
            binding.tvDishPrice.text = String.format("%.0f VNĐ", dish.BIT240074_Price)

            // Nhãn thời gian chế biến
            if (dish.BIT240074_PreparationTime <= 15) {
                binding.tvPrepTimeLabel.text = "Quick / Nhanh"
            } else {
                binding.tvPrepTimeLabel.text = "Regular / Thông thường"
            }

            // 2. Load ảnh đại diện dùng Glide (ảnh có isThumbnail = true)
            val thumbnailUrl = dish.BIT240074_DishImage.firstOrNull { it.isThumbnail }?.imageUrl
                ?: dish.BIT240074_DishImage.firstOrNull()?.imageUrl

            Glide.with(binding.root.context)
                .load(thumbnailUrl)
                .placeholder(R.drawable.ic_launcher_background)
                .error(R.drawable.ic_launcher_background)
                .centerCrop()
                .into(binding.imgDishThumbnail)

            // 3. Bắt sự kiện click vào item món ăn để mở DetailActivity
            binding.root.setOnClickListener {
                onItemClick?.invoke(dish) ?: run {
                    val context = binding.root.context
                    val intent = Intent(context, DetailActivity::class.java).apply {
                        putExtra("EXTRA_DISH", dish)
                        putExtra("EXTRA_DISH_ID", dish.id)
                    }
                    context.startActivity(intent)
                }
            }
        }
    }
}
