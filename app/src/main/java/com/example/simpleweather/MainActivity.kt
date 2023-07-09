package com.example.simpleweather

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Resources
import android.location.Location
import android.location.LocationManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat.getColor
import androidx.databinding.DataBindingUtil
import com.example.simpleweather.POJO.ModelClass
import com.example.simpleweather.Utilities.ApiUtilities
import com.example.simpleweather.databinding.ActivityMainBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
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
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var activityMainBinding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        activityMainBinding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        supportActionBar?.hide()

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        activityMainBinding.rlMainLayout.visibility = View.VISIBLE

        //Get the location of device
//        getCurrentLocation()

        activityMainBinding.etGetCityName.setOnEditorActionListener { v, actionId, keyEvent ->
            //User search for city name
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                getCityWeather(activityMainBinding.etGetCityName.text.toString())
                val view = this.currentFocus
                //If city name is typed
                if (view != null) {
                    //Hide the keyboard
                    val imm: InputMethodManager =
                        getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.hideSoftInputFromWindow(view.windowToken, 0)
                    activityMainBinding.etGetCityName.clearFocus()
                }
                true
            } else
                false

        }


    }

    private fun getCityWeather(cityName: String) {
        activityMainBinding.pbLoading.visibility = View.VISIBLE

        ApiUtilities.getApiInterface()?.getCityWeatherData(cityName, API_KEY)
            ?.enqueue(object : Callback<ModelClass> {
                @RequiresApi(Build.VERSION_CODES.O)
                override fun onResponse(call: Call<ModelClass>, response: Response<ModelClass>) {
                    setDataOnViews(response.body())
                }

                override fun onFailure(call: Call<ModelClass>, t: Throwable) {
                    Toast.makeText(
                        applicationContext,
                        "Please input a valid city",
                        Toast.LENGTH_SHORT
                    )
                        .show()
                }

            })

    }

    private fun getCurrentLocation() {
        //Check if we have permission
        if (checkPermissions()) {
            //If the location setting is enabled
            if (isLocationEnabled()) {
                //Get longitude and latitude
                if (ActivityCompat.checkSelfPermission(
                        this,
                        android.Manifest.permission.ACCESS_FINE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                        this,
                        android.Manifest.permission.ACCESS_COARSE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    //resend request
                    requestPermission()
                    return
                }

                fusedLocationProviderClient.lastLocation.addOnCompleteListener(this) { task ->
                    val location: Location? = task.result
                    if (location == null) {
                        Toast.makeText(this, "Failed to receive location", Toast.LENGTH_SHORT)
                            .show()

                    } else {
                        //Get current weather
                        fetchCurrentLocationWeather(
                            location.latitude.toString(),
                            location.longitude.toString()
                        )

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
        ApiUtilities.getApiInterface()?.getCurrentWeatherData(latitude, longitude, API_KEY)
            ?.enqueue(object :
                Callback<ModelClass> {
                @RequiresApi(Build.VERSION_CODES.O)
                override fun onResponse(call: Call<ModelClass>, response: Response<ModelClass>) {
                    if (response.isSuccessful) {
                        setDataOnViews(response.body())
                    }
                }

                override fun onFailure(call: Call<ModelClass>, t: Throwable) {
                    Toast.makeText(applicationContext, "Could not fetch data", Toast.LENGTH_SHORT)
                        .show()
                }
            })

    }

    @SuppressLint("SetTextI18n")
    @RequiresApi(Build.VERSION_CODES.O)
    private fun setDataOnViews(body: ModelClass?) {
        val sdf = SimpleDateFormat("'dd/mm/yy hh:mm")
        val currentDate = sdf.format(Date())
        //Get the current date
        activityMainBinding.tvDate.text = currentDate

        //Get temp data
        activityMainBinding.tvDayMaxTemp.text = "Day " + kelvinToCelsius(body!!.main.temp_max) + "°"
        activityMainBinding.tvDayMinTemp.text =
            "Night " + kelvinToCelsius(body.main.temp_min) + "°"
        activityMainBinding.tvTemp.text = "" + kelvinToCelsius(body.main.temp) + "°"
        activityMainBinding.tvFeelsLike.text =
            "Feels like " + kelvinToCelsius(body.main.feels_like) + "°"
        activityMainBinding.tvWeatherType.text = body.weather[0].main

        //Sunrise and sunset time
        activityMainBinding.tvSunrise.text = timestampToTime(body.sys.sunrise)
        activityMainBinding.tvSunset.text = timestampToTime(body.sys.sunset)

        //Pressure and humidity
        activityMainBinding.tvPressure.text = body.main.pressure.toString()
        activityMainBinding.tvHumidity.text = "" + body.main.humidity.toString() + "%"

        //Wind speed
        activityMainBinding.tvWindSpeed.text = "" + body.wind.speed.toString() + " m/s"

        //Temp in fahrenheit
        activityMainBinding.tvFahrenheit.text = "" + celsiusToFahrenheit(body.main.temp) + "°"

        //City's name
        activityMainBinding.etGetCityName.setText(body.name)

        //Depends on which weather id, update the UI
        updateUI(body.weather[0].id)


    }

    private fun updateUI(id: Int) {
        //ThunderStorm
        if (id in 200..232) {
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
            //Change color of status bar
            window.statusBarColor = resources.getColor(R.color.black)
            //Set color for tool bar
            activityMainBinding.rlToolbar.setBackgroundColor(resources.getColor(R.color.black))
            activityMainBinding.rlSubLayout.background = ContextCompat.getDrawable(
                this@MainActivity,
                R.drawable.bg_thunder_storm
            )

            activityMainBinding.llMainBgBelow.background = ContextCompat.getDrawable(
                this@MainActivity,
                R.drawable.bg_thunder_storm
            )
            activityMainBinding.llMainBgAbove.background = ContextCompat.getDrawable(
                this@MainActivity,
                R.drawable.bg_thunder_storm
            )
            //Background
            activityMainBinding.ivWeatherBg.setImageResource(R.drawable.bg_thunder_storm)
            //Weather icon
            activityMainBinding.ivWeatherIcon.setImageResource(R.drawable.ic_sunrise)


        }
        //Drizzle
        else if (id in 300..322) {
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
            //Change color of status bar
            //window.statusBarColor = resources.getColor(R.color.black)
            //Set color for tool bar
            activityMainBinding.rlToolbar.setBackgroundColor(resources.getColor(R.color.black))
            activityMainBinding.rlSubLayout.background = ContextCompat.getDrawable(
                this@MainActivity,
                R.drawable.bg_drizzle
            )

            activityMainBinding.llMainBgBelow.background = ContextCompat.getDrawable(
                this@MainActivity,
                R.drawable.bg_drizzle
            )
            activityMainBinding.llMainBgAbove.background = ContextCompat.getDrawable(
                this@MainActivity,
                R.drawable.bg_drizzle
            )

            //Background
            activityMainBinding.ivWeatherBg.setImageResource(R.drawable.bg_drizzle)
            //Weather icon
            activityMainBinding.ivWeatherIcon.setImageResource(R.drawable.ic_sunrise)


        }
        //Rain
        else if (id in 500..532) {
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
            //Change color of status bar
            //window.statusBarColor = resources.getColor(R.color.black)
            //Set color for tool bar
            activityMainBinding.rlToolbar.setBackgroundColor(resources.getColor(R.color.black))
            activityMainBinding.rlSubLayout.background = ContextCompat.getDrawable(
                this@MainActivity,
                R.drawable.bg_rainy
            )

            activityMainBinding.llMainBgBelow.background = ContextCompat.getDrawable(
                this@MainActivity,
                R.drawable.bg_rainy
            )
            activityMainBinding.llMainBgAbove.background = ContextCompat.getDrawable(
                this@MainActivity,
                R.drawable.bg_rainy
            )
            //Background
            activityMainBinding.ivWeatherBg.setImageResource(R.drawable.bg_rainy)
            //Weather icon
            activityMainBinding.ivWeatherIcon.setImageResource(R.drawable.bg_rainy)


        } else if (id in 600..632) {
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
            //Change color of status bar
            //window.statusBarColor = resources.getColor(R.color.black)
            //Set color for tool bar
            activityMainBinding.rlToolbar.setBackgroundColor(resources.getColor(R.color.black))
            activityMainBinding.rlSubLayout.background = ContextCompat.getDrawable(
                this@MainActivity,
                R.drawable.bg_snowy
            )

            activityMainBinding.llMainBgBelow.background = ContextCompat.getDrawable(
                this@MainActivity,
                R.drawable.bg_snowy
            )
            activityMainBinding.llMainBgAbove.background = ContextCompat.getDrawable(
                this@MainActivity,
                R.drawable.bg_snowy
            )
            //Background
            activityMainBinding.ivWeatherBg.setImageResource(R.drawable.bg_snowy)
            //Weather icon
            activityMainBinding.ivWeatherIcon.setImageResource(R.drawable.bg_snowy)


        } else if (id == 800) {
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
            //Change color of status bar
            //window.statusBarColor = resources.getColor(R.color.black)
            //Set color for tool bar
            activityMainBinding.rlToolbar.setBackgroundColor(resources.getColor(R.color.black))
            activityMainBinding.rlSubLayout.background = ContextCompat.getDrawable(
                this@MainActivity,
                R.drawable.bg_clear
            )

            activityMainBinding.llMainBgBelow.background = ContextCompat.getDrawable(
                this@MainActivity,
                R.drawable.bg_clear
            )
            activityMainBinding.llMainBgAbove.background = ContextCompat.getDrawable(
                this@MainActivity,
                R.drawable.bg_clear
            )
            //Background
            activityMainBinding.ivWeatherBg.setImageResource(R.drawable.bg_clear)
            //Weather icon
            activityMainBinding.ivWeatherIcon.setImageResource(R.drawable.bg_clear)

        } else if (id in 801..805) {
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
            //Change color of status bar
            //window.statusBarColor = resources.getColor(R.color.black)
            //Set color for tool bar
            activityMainBinding.rlToolbar.setBackgroundColor(resources.getColor(R.color.black))
            activityMainBinding.rlSubLayout.background = ContextCompat.getDrawable(
                this@MainActivity,
                R.drawable.bg_cloudy
            )

            activityMainBinding.llMainBgBelow.background = ContextCompat.getDrawable(
                this@MainActivity,
                R.drawable.bg_cloudy
            )
            activityMainBinding.llMainBgAbove.background = ContextCompat.getDrawable(
                this@MainActivity,
                R.drawable.bg_cloudy
            )
            //Background
            activityMainBinding.ivWeatherBg.setImageResource(R.drawable.bg_cloudy)
            //Weather icon
            activityMainBinding.ivWeatherIcon.setImageResource(R.drawable.bg_cloudy)

        } else {
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
            //Change color of status bar
            //window.statusBarColor = resources.getColor(R.color.black)
            //Set color for tool bar
            activityMainBinding.rlToolbar.setBackgroundColor(resources.getColor(R.color.black))
            activityMainBinding.rlSubLayout.background = ContextCompat.getDrawable(
                this@MainActivity,
                R.drawable.bg_normal
            )

            activityMainBinding.llMainBgBelow.background = ContextCompat.getDrawable(
                this@MainActivity,
                R.drawable.bg_normal
            )
            activityMainBinding.llMainBgAbove.background = ContextCompat.getDrawable(
                this@MainActivity,
                R.drawable.bg_normal
            )
            //Background
            activityMainBinding.ivWeatherBg.setImageResource(R.drawable.bg_normal)
            //Weather icon
            activityMainBinding.ivWeatherIcon.setImageResource(R.drawable.bg_normal)

        }
        activityMainBinding.pbLoading.visibility = View.GONE
        activityMainBinding.rlMainLayout.visibility = View.VISIBLE

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
        val locationManager: LocationManager =
            getSystemService(Context.LOCATION_SERVICE) as LocationManager
        //Check if the location is enabled via gps or network provider
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    private fun requestPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                android.Manifest.permission.ACCESS_COARSE_LOCATION,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ),
            PERMISSION_REQUEST_ACCESS_LOCATION
        )
    }


    companion object {
        private const val PERMISSION_REQUEST_ACCESS_LOCATION = 100
        const val API_KEY = "28ee86d26d0aeae420ec461d3553b533"
    }

    private fun checkPermissions(): Boolean {
        if (ActivityCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
            && ActivityCompat.checkSelfPermission(
                this,
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
            } else {
                Toast.makeText(applicationContext, "Permission Denied", Toast.LENGTH_SHORT).show()
            }
        }
    }


}