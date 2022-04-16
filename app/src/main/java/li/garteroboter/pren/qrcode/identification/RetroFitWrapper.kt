package li.garteroboter.pren.qrcode.identification

import android.app.Activity
import android.content.Context
import android.util.Log
import li.garteroboter.pren.qrcode.identification.PlantApiService
import okhttp3.OkHttpClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.net.HttpURLConnection

class RetroFitWrapper(val apiKey: String) {

    private val TAG = "RetroFitWrapper"
    private val testUrl = "https://pren.garteroboter.li/static/img/plant4.jpg"
    private val BASE_URL: String = "https://my-api.plantnet.org/"



    private val retrofit = Retrofit.Builder()
        .client(
            OkHttpClient()
                .newBuilder()
                .build()
        )
        .addConverterFactory(MoshiConverterFactory.create())
        .baseUrl(BASE_URL)
        .build()

    private val plantService = retrofit.create(PlantApiService::class.java)

    fun requestPlantIdentification() {
        Log.d(TAG, "Starting call with key = $apiKey")
        val call = plantService.singlePlantRequest(apiKey, testUrl)
        call.enqueue(object : Callback<JsonObject> {
            override fun onResponse(call: Call<JsonObject>, response: Response<JsonObject>) {
                if (response.code() == HttpURLConnection.HTTP_OK) {
                    Log.d(TAG, response.toString())
                    Log.d(TAG, response.body().toString())
                } else {
                    Log.e(TAG, response.toString())
                }
            }

            override fun onFailure(call: Call<JsonObject>, t: Throwable) {
                Log.d(TAG, "onFailure")
            }
        })
    }


}