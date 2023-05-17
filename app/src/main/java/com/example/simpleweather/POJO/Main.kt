package com.example.simpleweather.POJO

import com.google.gson.annotations.SerializedName

data class Main (
    @SerializedName("temp") val temp: Float,
    @SerializedName("feels_like") val feels_like: Float,
    @SerializedName("temp_min") val temp_min: Float,
    @SerializedName("temp_max") val temp_max: Float,
    @SerializedName("pressure") val pressure: Float,
    @SerializedName("humidity") val humidity: Float
)
