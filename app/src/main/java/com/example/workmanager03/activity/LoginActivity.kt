package com.example.workmanager03.activity

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.workmanager03.connection.*
import com.example.workmanager03.databinding.ActivityLoginBinding
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.math.BigInteger
import java.security.MessageDigest

class LoginActivity : AppCompatActivity() {
    private val binding by lazy {
        ActivityLoginBinding.inflate(layoutInflater)
    }
    private lateinit var sharedPreference: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sharedPreference = getSharedPreferences("LocalMemory", Context.MODE_PRIVATE)

        if (checkStatus()) {
            val intent = Intent(applicationContext, MainActivity::class.java)
            startActivity(intent)
            finish()
        } else {
            setContentView(binding.root)
        }

        binding.btnLogin.setOnClickListener {
            if (binding.userPhoneField.text.length == 9 && binding.userKeyField.text.isNotEmpty()) {
                if (CheckConnection.isOnline(this)) {
                    doLogin(
                        binding.userPhoneField.text.toString(),
                        binding.userKeyField.text.toString(),
                        applicationContext
                    )
                    binding.btnLogin.isEnabled = false
                } else {
                    Toast.makeText(applicationContext, "No Internet Connection!", Toast.LENGTH_LONG)
                        .show()
                }
            } else {
                Toast.makeText(
                    applicationContext,
                    "Please fill in all fields correctly!",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun doLogin(phone: String, key: String, context: Context) {
        val response = ServiceBuilder.buildService(ApiInterfaceLogin::class.java)
        val requestModel = RequestModelLogin(
            md5("$phone${key}bCctS9eqoYaZl21a"),
            phone,
            key
        )
        Log.i("MyTag", "$requestModel ")

        response.sendReq(requestModel).enqueue(
            object : Callback<ResponseModel> {
                override fun onResponse(
                    call: Call<ResponseModel>,
                    response: Response<ResponseModel>
                ) {
                    Log.d("MyTag", response.body()?.status.toString())
                    if (response.body()?.status == 200) {
                        Toast.makeText(context, "Authorization successful!", Toast.LENGTH_LONG)
                            .show()
                        saveUserData(phone)
                    } else {
                        Toast.makeText(
                            applicationContext,
                            "Authorization failed!",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }

                override fun onFailure(call: Call<ResponseModel>, t: Throwable) {
                    Log.d("MyTag", t.toString())
                }
            }
        )
    }

    private fun saveUserData(phone: String) {
        val editor = sharedPreference.edit()
        editor.putInt("loginState", 100)
        editor.putString("phone", phone)
        editor.apply()

        val intent = Intent(applicationContext, MainActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun md5(input: String): String {
        val md = MessageDigest.getInstance("MD5")
        return BigInteger(1, md.digest(input.toByteArray())).toString(16).padStart(32, '0')
    }

    private fun checkStatus(): Boolean {
        return sharedPreference.getInt("loginState", -1) == 100
    }
}