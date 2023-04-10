package com.example.workmanager03.connection

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

interface ApiInterfaceReg {
    @POST("location/reg.php")
    fun sendReq(@Body requestModel: RequestModelReg) : Call<ResponseModel>
}