package li.garteroboter.pren.qrcode.identification

import android.content.Context
import android.util.Log
import androidx.lifecycle.MutableLiveData
import okhttp3.OkHttpClient
import org.apache.commons.lang3.NotImplementedException
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

    private val liveDataResult = MutableLiveData<Results>()
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


    fun requestRemotePlantIdentificationSynchronously() : String {
        val synchronousCall = plantService.singlePlantRequest(testUrl, "auto", include=false, no_Reject=false, "en", apiKey)
        try {
            val response: Response<JsonObject> = synchronousCall.execute()
            Log.d(TAG, response.toString())
            if (response.code() == HttpURLConnection.HTTP_OK) {
                val plantNetApiResult: JsonObject? = response.body()
                val res: List<Results>  = plantNetApiResult?.results ?: Collections.emptyList()
                Log.d(TAG, res.toString())

              return extractBestResult(res)?.species?.scientificName ?: "failed";



            }
        } catch (ex: Exception) {
            Log.e(TAG, "Api Call requestRemotePlantIdentificationSynchronously failed ");
            ex.printStackTrace()
        }

        return "failed";
    }

    // Not optimal for my use case
    fun requestRemotePlantIdentificationAsynchronously() {
        val call = plantService.singlePlantRequest(testUrl, "auto", include=false, no_Reject=false, "en", apiKey)
        call.enqueue(object : Callback<JsonObject> {
            override fun onResponse(call: Call<JsonObject>, response: Response<JsonObject>) {
                Log.d(TAG, response.toString())
                if (response.code() == HttpURLConnection.HTTP_OK) {
                    val plantNetApiResult: JsonObject? = response.body()
                    val res: List<Results>  = plantNetApiResult?.results ?: Collections.emptyList()
                    Log.d(TAG, res.toString())

                    val finalResult: Results? = extractBestResult(res)
                    finalResult?.let { liveDataResult.value = it } ?: run {
                        Log.e(TAG, "extractBestResult is null. probably no results")
                    }


                }
            }

            override fun onFailure(call: Call<JsonObject>, t: Throwable) {
                Log.d(TAG, "onFailure")
            }
        })
    }



    fun extractBestResult(res: List<Results>): Results? {
        return res.maxOrNull()

    }

    fun requestLocalPlantIdentification() {
        throw NotImplementedException("not implemented")

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

}