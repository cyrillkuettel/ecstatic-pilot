package li.garteroboter.pren.qrcode.identification

import android.content.Context
import android.util.Log
import androidx.lifecycle.MutableLiveData
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
    private val testUrl = "https://pren.garteroboter.li/static/img/plant1.jpg"

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
        val synchronousCall = plantService.singlePlantRequestRemote(testUrl, "auto", include=false, no_Reject=false, "en", apiKey)
        try {
            val response: Response<JsonObject> = synchronousCall.execute()
            Log.d(TAG, response.toString())
            if (response.code() == HttpURLConnection.HTTP_OK) {
                val plantNetApiResult: JsonObject? = response.body()
                val res: List<Results>  = plantNetApiResult?.results ?: Collections.emptyList()
                Log.d(TAG, res.toString())

              return extractBestResult(res)?.species?.scientificName ?: "failed"
            }
        } catch (ex: Exception) {
            Log.e(TAG, "Api Call requestRemotePlantIdentificationSynchronously failed ");
            ex.printStackTrace()
        }

        return "failed"
    }


    fun requestLocalPlantIdentification(uri: String) : String {
        val processedUri =  processUri(uri)
        Log.d(TAG, "starting request with uri = $processedUri")
        val synchronousCall = plantService.singlePlantRequestLocal(include = false, no_Reject=false,"en", apiKey, processedUri, "auto")
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
        return "failed"
    }

    fun processUri(input: String) : String {
        val prefixToRemove = "file://"
        val result = input.removePrefix(prefixToRemove)
        return result

    }



    fun extractBestResult(res: List<Results>): Results? {
        return res.maxOrNull()

    }

}