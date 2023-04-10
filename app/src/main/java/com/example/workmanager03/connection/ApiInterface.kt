package com.example.workmanager03.connection

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

interface ApiInterface {
    @POST("location/insert.php")
    fun sendReq(@Body requestModel: RequestModel) : Call<ResponseModel>
}