package com.example.workmanager03.connection

data class RequestModel(
    val sign: String,
    val datetime: String,
    val latitude: String,
    val longitude: String,
    val altitude: String,
    val signal: Int,
    val phoneId: String
)
