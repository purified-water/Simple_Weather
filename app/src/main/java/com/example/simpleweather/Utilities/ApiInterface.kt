package com.example.simpleweather.Utilities

import com.example.simpleweather.POJO.ModelClass
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface ApiInterface {

    @GET("weather")
    fun getCurrentWeatherData(
        @Query("lat") latitude: String,
        @Query("lon") longitude: String,
        @Query("APPID") api_key: String
    ): Call<ModelClass> //using call retrofit2

    @GET("weather")
    fun getCityWeatherData(
        @Query("q") city_name: String,
        @Query("APPID") api_key: String
    ): Call<ModelClass> //using call retrofit2
}