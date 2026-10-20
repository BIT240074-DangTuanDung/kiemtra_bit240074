package com.example.kiemtragiuaky.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.kiemtragiuaky.model.Dish
import com.example.kiemtragiuaky.model.DishImage

@Entity(tableName = "dishes")
data class DishEntity(
    @PrimaryKey
    val id: String,
    val BIT240074_Name: String,
    val BIT240074_Price: Double,
    val BIT240074_PreparationTime: Int,
    val BIT240074_IsAvailable: Boolean,
    val BIT240074_Description: String,
    val BIT240074_DishCategoryId: String,
    val imageUrl: String = "",
    val isSynced: Boolean = false,
    val isDeleted: Boolean = false,
) {
    // Chuyển đổi từ Entity sang Model Dish
    fun toDish(): Dish {
        val images = if (imageUrl.isNotBlank()) {
            listOf(DishImage(imageUrl = imageUrl, isThumbnail = true))
        } else {
            emptyList()
        }
        return Dish(
            id = id,
            BIT240074_Name = BIT240074_Name,
            BIT240074_Price = BIT240074_Price,
            BIT240074_PreparationTime = BIT240074_PreparationTime,
            BIT240074_IsAvailable = BIT240074_IsAvailable,
            BIT240074_Description = BIT240074_Description,
            BIT240074_DishCategoryId = BIT240074_DishCategoryId,
            BIT240074_DishImage = images,
        )
    }

    companion object {
        // Chuyển đổi từ Model Dish sang Entity
        fun fromDish(dish: Dish, isSynced: Boolean = false, isDeleted: Boolean = false): DishEntity {
            val thumbUrl = dish.BIT240074_DishImage.firstOrNull { it.isThumbnail }?.imageUrl
                ?: dish.BIT240074_DishImage.firstOrNull()?.imageUrl ?: ""
            return DishEntity(
                id = dish.id,
                BIT240074_Name = dish.BIT240074_Name,
                BIT240074_Price = dish.BIT240074_Price,
                BIT240074_PreparationTime = dish.BIT240074_PreparationTime,
                BIT240074_IsAvailable = dish.BIT240074_IsAvailable,
                BIT240074_Description = dish.BIT240074_Description,
                BIT240074_DishCategoryId = dish.BIT240074_DishCategoryId,
                imageUrl = thumbUrl,
                isSynced = isSynced,
                isDeleted = isDeleted,
            )
        }
    }
}
