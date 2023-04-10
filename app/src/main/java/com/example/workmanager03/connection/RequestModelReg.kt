package com.example.workmanager03.connection

data class RequestModelReg(
    val sign: String,
    val qr: String,
    val fullName: String,
    val gender: String,
    val region: String,
    val district: String,
    val jamoat: String,
    val village: String,
    val phone: String
)
