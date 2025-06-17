package com.example.weathermap

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.example.weathermap.databinding.ActivityMapsBinding
import com.example.weathermap.databinding.CustomInfoWindowBinding
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import retrofit2.*
import retrofit2.converter.gson.GsonConverterFactory
import java.util.*

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var binding: ActivityMapsBinding
    private lateinit var mMap: GoogleMap
    private val apiKey = "#"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        mMap.setInfoWindowAdapter(object : GoogleMap.InfoWindowAdapter {
            override fun getInfoContents(marker: Marker): View {
                val infoBinding = CustomInfoWindowBinding.inflate(layoutInflater)
                infoBinding.tvCityName.text = marker.title
                infoBinding.tvWeather.text = marker.snippet

                val desc = marker.snippet?.lowercase(Locale.getDefault()) ?: ""
                val icon = when {
                    "rain" in desc -> R.drawable.rainy_icon
                    "cloud" in desc -> R.drawable.cloudy_icon
                    "clear" in desc -> R.drawable.sunny_icon
                    "snow" in desc -> R.drawable.snowy_icon
                    else -> R.drawable.default_weather_icon
                }
                infoBinding.ivWeatherIcon.setImageResource(icon)
                return infoBinding.root
            }

            override fun getInfoWindow(marker: Marker): View? = null
        })

        mMap.setOnCameraIdleListener {
            val zoom = mMap.cameraPosition.zoom
            val bounds = mMap.projection.visibleRegion.latLngBounds

            val density = when {
                zoom >= 9 -> "high"
                zoom >= 6 -> "medium"
                else      -> "low"
            }

            loadWeatherForVisibleCities(bounds, density)
        }
    }

    private fun loadWeatherForVisibleCities(bounds: LatLngBounds, density: String) {
        val allCities = listOf(
            "Istanbul" to LatLng(41.0082, 28.9784),
            "Ankara" to LatLng(39.9208, 32.8541),
            "Izmir" to LatLng(38.4192, 27.1287),
            "Bursa" to LatLng(40.1950, 29.0600),
            "Antalya" to LatLng(36.8969, 30.7133),
            "Adana" to LatLng(37.0000, 35.3213),
            "Gaziantep" to LatLng(37.0662, 37.3833),
            "Konya" to LatLng(37.8715, 32.4846),
            "Kayseri" to LatLng(38.7312, 35.4787),
            "Mersin" to LatLng(36.8121, 34.6415),
            "Samsun" to LatLng(41.2867, 36.33),
            "Trabzon" to LatLng(41.0015, 39.7178),
            "Diyarbakir" to LatLng(37.9144, 40.2306),
            "Malatya" to LatLng(38.3552, 38.3095),
            "Erzurum" to LatLng(39.9043, 41.2679),
            "Van" to LatLng(38.4942, 43.3836),
            "Manisa" to LatLng(38.6191, 27.4289),
            "Sivas" to LatLng(39.7477, 37.0179),
            "Balıkesir" to LatLng(39.6484, 27.8826),
            "Eskişehir" to LatLng(39.7767, 30.5206),
            "Çanakkale" to LatLng(40.1553, 26.4142),
            "Aydın" to LatLng(37.8480, 27.8456),
            "Kahramanmaraş" to LatLng(37.5736, 36.9371),
            // İlçeler
            "Kadıköy" to LatLng(40.9897, 29.0366),
            "Beşiktaş" to LatLng(41.0438, 29.0094),
            "Üsküdar" to LatLng(41.0220, 29.0282),
            "Bakırköy" to LatLng(40.9794, 28.8724),
            "Şişli" to LatLng(41.0606, 28.9870),
            "Çankaya" to LatLng(39.9179, 32.8627),
            "Keçiören" to LatLng(39.9889, 32.8573),
            "Karşıyaka" to LatLng(38.4575, 27.1089),
            "Buca" to LatLng(38.3871, 27.1557),
            "Yıldırım" to LatLng(40.1828, 29.1238),
            "Osmangazi" to LatLng(40.1958, 29.0627)
        )

        val majorDistricts = listOf("Kadıköy", "Beşiktaş", "Çankaya", "Üsküdar", "Buca", "Karşıyaka", "Şişli", "Keçiören", "Yıldırım", "Osmangazi")

        val filtered = when (density) {
            "high" -> allCities
            "medium" -> allCities.filterNot { it.first in majorDistricts }
            "low" -> listOf("Istanbul", "Ankara", "Izmir", "Bursa", "Antalya")
                .mapNotNull { name -> allCities.find { it.first == name } }
            else -> emptyList()
        }

        mMap.clear()

        val retrofit = Retrofit.Builder()
            .baseUrl("https://api.openweathermap.org/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        val service = retrofit.create(WeatherService::class.java)

        for ((cityName, location) in filtered) {
            if (bounds.contains(location)) {
                service.getWeather(cityName, apiKey).enqueue(object : Callback<WeatherResponse> {
                    override fun onResponse(call: Call<WeatherResponse>, response: Response<WeatherResponse>) {
                        if (response.isSuccessful) {
                            val weather = response.body()
                            val tempCelsius = (weather?.main?.temp ?: 0f) - 273.15f
                            val description = weather?.weather?.get(0)?.description?.replaceFirstChar(Char::titlecase) ?: "-"
                            val snippet = "Sıcaklık: ${tempCelsius.toInt()}°C\nDurum: $description"

                            val marker = mMap.addMarker(
                                MarkerOptions()
                                    .position(location)
                                    .title(cityName)
                                    .snippet(snippet)
                                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
                            )
                            marker?.showInfoWindow()
                        }
                    }

                    override fun onFailure(call: Call<WeatherResponse>, t: Throwable) {
                        Log.e("MAP_WEATHER", "API hatası: ${t.localizedMessage}")
                    }
                })
            }
        }
    }
}