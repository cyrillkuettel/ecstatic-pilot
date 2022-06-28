package li.garteroboter.pren.qrcode.identification

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

class RetroFitWrapper(private val apiKey: String) {

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


    fun requestLocalPlantIdentificationSynchronously(uri: String) : String {
        Log.d(TAG, "requestLocalPlantIdentification");
        val processedUri =  removeFilePrefixFromURI(uri)
        Log.d(TAG, "starting request with uri = $processedUri")

        val file = File(processedUri)
        val requestFile = RequestBody.create(MediaType.parse("multipart/form-data"), file)
        val multiPartBody = MultipartBody.Part.createFormData("images", file.name, requestFile)
        val organs = RequestBody.create(MediaType.parse("multipart/form-data"), "leaf")


        val synchronousCall = plantService.singlePlantRequestLocal2(
            multiPartBody,
            organs,
            include = false,
            no_Reject=false,
            "de",
            apiKey)

        Log.v(TAG, synchronousCall.toString())

        try {
            val response: Response<JsonObject> = synchronousCall.execute()

            response.errorBody()?.toString()?.let { Log.e(TAG, it) }

            Log.v(TAG, response.toString())

            if (response.code() == HttpURLConnection.HTTP_OK) {
                val plantNetApiResult: JsonObject? = response.body()
                val res: List<Results> = plantNetApiResult?.results ?: Collections.emptyList()
                Log.d(TAG, res.toString())
                val concatenatedNames = extractBestResult(res)?.species?.commonNames?.joinToString()
                return concatenatedNames ?: "failed to extract Name";
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


    private fun removeFilePrefixFromURI(input: String) : String {
        val prefixToRemove = "file://"
        val result = input.removePrefix(prefixToRemove)
        return result

    }


    private fun extractBestResult(res: List<Results>): Results? {
        /** Custom Comparator in PlantApiService */
        return res.maxOrNull()
    }

}