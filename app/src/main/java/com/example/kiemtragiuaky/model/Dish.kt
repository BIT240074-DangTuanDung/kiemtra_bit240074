package com.example.kiemtragiuaky.model

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class Dish(
    @SerializedName("id")
    val id: String,

    @SerializedName("BIT240074_Name")
    val BIT240074_Name: String,

    @SerializedName("BIT240074_Price")
    val BIT240074_Price: Double,

    @SerializedName("BIT240074_PreparationTime")
    val BIT240074_PreparationTime: Int,

    @SerializedName("BIT240074_IsAvailable")
    val BIT240074_IsAvailable: Boolean,

    @SerializedName("BIT240074_Description")
    val BIT240074_Description: String,

    @SerializedName("BIT240074_DishCategoryId")
    val BIT240074_DishCategoryId: String,

    @SerializedName("BIT240074_DishImage")
    val BIT240074_DishImage: List<DishImage> = emptyList(),
) : Serializable
