package com.example.kiemtragiuaky.network

import com.example.kiemtragiuaky.model.Dish
import com.example.kiemtragiuaky.model.DishCategory
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface ApiService {
    @GET("BIT240074_DishCategory")
    suspend fun getCategories(): List<DishCategory>

    @GET("BIT240074_Dish")
    suspend fun getDishes(): List<Dish>

    @POST("BIT240074_Dish")
    suspend fun createDish(@Body dish: Dish): Dish

    @PUT("BIT240074_Dish/{id}")
    suspend fun updateDish(
        @Path("id") id: String,
        @Body dish: Dish,
    ): Dish

    @DELETE("BIT240074_Dish/{id}")
    suspend fun deleteDish(
        @Path("id") id: String,
    ): Dish
}
