package com.example.kiemtragiuaky.network

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
    // Thay đổi BASE_URL thành URL MockAPI thực tế của bạn
    private const val BASE_URL = "https://6ad73c295e98f36aef2181e.mockapi.io/"

    val apiService: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}
