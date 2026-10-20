package com.example.kiemtragiuaky.model

import com.google.gson.annotations.SerializedName

data class DishCategory(
    @SerializedName("id")
    val id: String,

    @SerializedName("BIT240074_Name")
    val BIT240074_Name: String,

    @SerializedName("BIT240074_Description")
    val BIT240074_Description: String,
)
