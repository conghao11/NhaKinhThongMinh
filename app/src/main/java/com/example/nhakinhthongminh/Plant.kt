package com.example.nhakinhthongminh

data class Plant(
    val name: String,
    val tempRange: String,
    val moistureRange: String,
    val iconRes: Int,
    val category: String = "Khác",
    val targetMoisture: Int
)