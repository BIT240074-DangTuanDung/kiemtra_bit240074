package com.example.kiemtragiuaky.model

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class DishImage(
    @SerializedName("imageUrl")
    val imageUrl: String,

    @SerializedName("isThumbnail")
    val isThumbnail: Boolean = false,
) : Serializable
