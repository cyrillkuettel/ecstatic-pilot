package li.garteroboter.pren.qrcode.identification

import android.content.Context
import android.util.Log
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.io.File
import java.net.HttpURLConnection
import java.util.*

class RetroFitWrapper(private val apiKey: String, val context: Context?) {
    /*
    error
    D/RetroFitWrapper: Response{protocol=http/1.1, code=415, message=Unsupported Media Type,
     url=https://my-api.plantnet.org/v2/identify/weurope?include-related-i2mages=false&no-reject=false&lang=en&api-key=2b10rYOrxC0HDiZzccuFce&images=%2Fstorage%2Femulated%2F0%2FAndroid%2Fmedia%2Fli.garteroboter.pren%2FPilot%2F2022-04-21-10-23-54-624.jpg&organs=auto}


    correct url form website:
     https://my-api.plantnet.org/v2/identify/all?include-related-images=false&no-reject=false&lang=en&api-key=2b10rYOrxC0HDiZzccuFce
     */
    private val TAG = "RetroFitWrapper"
    private val testUrl = "https://pren.garteroboter.li/static/img/plant1.jpg"

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




    fun requestLocalPlantIdentification(uri: String) : String {

        val processedUri =  processUri(uri)
        Log.d(TAG, "starting request with uri = $processedUri")

        val file = File(processedUri)
        val requestFile = RequestBody.create(MediaType.parse("multipart/form-data"), file)
        val multiPartBody = MultipartBody.Part.createFormData("images", file.name, requestFile)
        val organs = RequestBody.create(MediaType.parse("multipart/form-data"), "auto")


        val synchronousCall = plantService.singlePlantRequestLocal2(
            multiPartBody,
            organs,
            include = false,
            no_Reject=false,
            "en",
            apiKey)

        Log.v(TAG, synchronousCall.toString())

        try {
            val response: Response<JsonObject> = synchronousCall.execute()

            response.errorBody()?.toString()?.let { Log.e(TAG, it) }

            Log.v(TAG, response.toString())

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




    private fun processUri(input: String) : String {
        val prefixToRemove = "file://"
        val result = input.removePrefix(prefixToRemove)
        return result

    }



    private fun extractBestResult(res: List<Results>): Results? {
        return res.maxOrNull()
    }

}