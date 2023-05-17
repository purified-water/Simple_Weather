package com.example.simpleweather

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.databinding.DataBindingUtil
import com.example.simpleweather.POJO.ModelClass
import com.example.simpleweather.Utilities.ApiUtilities
import com.example.simpleweather.databinding.ActivityMainBinding
import com.google.android.gms.location.FusedLocationProviderClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.math.RoundingMode
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.ZoneId
import java.util.*
import kotlin.math.roundToInt

class MainActivity : AppCompatActivity() {
    private lateinit var fusedLocationProviderClient : FusedLocationProviderClient
    private lateinit var activityMainBinding : ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {


        super.onCreate(savedInstanceState)
        activityMainBinding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        supportActionBar?.hide()
        activityMainBinding.rlMainLayout.visibility = View.GONE

        getCurrentLocation()

    }

    private fun getCurrentLocation() {
        //Check if we have permission
        if (checkPermissions()) {
            //If the location setting is enabled
            if (isLocationEnabled()) {
                //Get longitude and latitude
                if (ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    //Check
                    requestPermission()
                    return
                }
                fusedLocationProviderClient.lastLocation.addOnCompleteListener(this) { task ->
                    val location:Location?=task.result
                    if (location == null) {
                        Toast.makeText(this, "Failed to receive location", Toast.LENGTH_SHORT).show()

                    }
                    else {
                        //Get current weather
                        fetchCurrentLocationWeather(location.latitude.toString(), location.longitude.toString())

                    }

                }


            } else {
                //setting open here
                Toast.makeText(this, "Please turn on location", Toast.LENGTH_SHORT).show()
                val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                startActivity(intent)


            }
        } else {
            //Show request for permission
            requestPermission()

        }
    }

    //Fetch weather function
    private fun fetchCurrentLocationWeather(latitude: String, longitude: String) {
        activityMainBinding.pbLoading.visibility = View.VISIBLE
        ApiUtilities.getApiInterface()?.getCurrentWeatherData(latitude, longitude, API_KEY)?.enqueue(object :
            Callback<ModelClass>{
            override fun onResponse(call: Call<ModelClass>, response: Response<ModelClass>) {
                if (response.isSuccessful) {
                    setDataOnViews(response.body())
                }
            }

            override fun onFailure(call: Call<ModelClass>, t: Throwable) {
                Toast.makeText(applicationContext,"Could not fetch data", Toast.LENGTH_SHORT).show()
            }
        })

    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun setDataOnViews(body: ModelClass?) {
        val sdf = SimpleDateFormat("'dd/mm/yy hh:mm")
        val currentDate = sdf.format(Date())
        //Get the current date
        activityMainBinding.tvDate.text = currentDate

        //Get temp data
        activityMainBinding.tvDayMaxTemp.text = "Day " + kelvinToCelsius(body!!.main.temp_max) + "°"
        activityMainBinding.tvDayMinTemp.text = "Night " + kelvinToCelsius(body!!.main.temp_min) + "°"
        activityMainBinding.tvTemp.text = ""+kelvinToCelsius(body!!.main.temp) + "°"
        activityMainBinding.tvFeelsLike.text = "Feels like " + kelvinToCelsius(body!!.main.feels_like) + "°"
        activityMainBinding.tvWeatherType.text = body.weather[0].main

        //Sunrise and sunset time
        activityMainBinding.tvSunrise.text = timestampToTime(body.sys.sunrise)
        activityMainBinding.tvSunset.text = timestampToTime(body.sys.sunset)

        //Pressure and humidity
        activityMainBinding.tvPressure.text = body.main.pressure.toString()
        activityMainBinding.tvHumidity.text = ""+body.main.humidity.toString() + "%"

        //Wind speed
        activityMainBinding.tvWindSpeed.text = ""+body.wind.speed.toString() + " m/s"

        //Temp in fahrenheit
        activityMainBinding.tvFahrenheit.text = ""+celsiusToFahrenheit(body.main.temp) + "°"

        //City's name
        activityMainBinding.etGetCityName.setText(body.name)

        //Depends on which weather id, update the UI
        updateUI(body.weather[0].id)



    }

    private fun updateUI(id: Int) {
        if (id in 200..232) {
            TODO("Update the ui")
        }
    }

    //From kelvin to celsius degree
    private fun kelvinToCelsius(temp: Float): Float {
        return (temp - 273.15).toBigDecimal().setScale(1, RoundingMode.UP).toFloat()

    }
    //From celsius to fahrenheit
    private fun celsiusToFahrenheit(temp: Float): Float {
        return ((temp * 1.8) + 32).toBigDecimal().setScale(1, RoundingMode.UP).toFloat()
    }

    //Convert timestamp to time format
    @RequiresApi(Build.VERSION_CODES.O)
    private fun timestampToTime(timestamp: Long): String {
        val localTime = timestamp.let {
            Instant.ofEpochSecond(it)
                .atZone(ZoneId.systemDefault())
                .toLocalTime()
        }
        return localTime.toString()
    }




    //check if location is enabled
    private fun isLocationEnabled(): Boolean {
        val locationManager:LocationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        //Check if the location is enabled via gps or network provider
        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
            locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER))
        {
            return true
        }
        return false
    }

    private fun requestPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(android.Manifest.permission.ACCESS_COARSE_LOCATION,
                android.Manifest.permission.ACCESS_FINE_LOCATION),
            PERMISSION_REQUEST_ACCESS_LOCATION
        )
    }


    companion object {
        private const val PERMISSION_REQUEST_ACCESS_LOCATION = 100
        const val API_KEY = "28ee86d26d0aeae420ec461d3553b533"
    }

    private fun checkPermissions(): Boolean {
        if (ActivityCompat.checkSelfPermission(this,
                android.Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
            && ActivityCompat.checkSelfPermission(this,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            return true
        }
        return false
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == PERMISSION_REQUEST_ACCESS_LOCATION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(applicationContext, "Granted", Toast.LENGTH_SHORT).show()
                getCurrentLocation()
            }
            else {
                Toast.makeText(applicationContext, "Permission Denied", Toast.LENGTH_SHORT).show()
            }
        }
    }


}