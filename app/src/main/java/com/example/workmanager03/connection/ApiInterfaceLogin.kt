package com.example.workmanager03.connection

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

interface ApiInterfaceLogin {
    @POST("location/login.php")
    fun sendReq(@Body requestModel: RequestModelLogin) : Call<ResponseModel>
}