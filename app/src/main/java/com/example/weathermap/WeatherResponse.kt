package com.example.weathermap

import Main
import Weather

data class WeatherResponse (
    val main: Main,
    val weather: List<Weather>,
    val name: String // Şehir adı
)