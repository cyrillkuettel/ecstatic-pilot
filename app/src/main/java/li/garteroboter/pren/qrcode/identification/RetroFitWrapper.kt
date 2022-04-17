package li.garteroboter.pren.qrcode.identification

import android.content.Context
import android.util.Log
import okhttp3.OkHttpClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.net.HttpURLConnection
import java.util.*

class RetroFitWrapper(private val apiKey: String, val context: Context?) {

    private val TAG = "RetroFitWrapper"
    private val testUrl = "https://pren.garteroboter.li/static/img/plant8.jpg"
    private val test2 = "https://media.wired.com/photos/5d8aab8bef84070009028d31/master/w_2560%2Cc_limit/Plant-Music-1162975190.jpg"
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

    fun testRequestPlantIdentification() {
        val call = plantService.singlePlantRequest(testUrl, "auto", include=false, no_Reject=false, "en", apiKey)
        call.enqueue(object : Callback<JsonObject> {
            override fun onResponse(call: Call<JsonObject>, response: Response<JsonObject>) {
                Log.d(TAG, response.toString())
                if (response.code() == HttpURLConnection.HTTP_OK) {
                    val plantNetApiResult: JsonObject? = response.body()
                    val results: List<Results>  = plantNetApiResult?.results ?: Collections.emptyList()



                    Log.d(TAG, results.toString())
                }
            }

            override fun onFailure(call: Call<JsonObject>, t: Throwable) {
                Log.d(TAG, "onFailure")
            }
        })
    }

    fun printKeyDebug() {
        Log.d(TAG, "Starting call with key = $apiKey")
    }
}