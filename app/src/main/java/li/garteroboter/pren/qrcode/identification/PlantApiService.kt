package li.garteroboter.pren.qrcode.identification



import retrofit2.Call
import retrofit2.http.GET



/*
note:
    GET for using URLS:
    POST for local files
*/


interface PlantApiService {

    @GET("v2/identify/weurope")
    fun singlePlantRequest(@retrofit2.http.Query("images") imageUrl: String?,
                           @retrofit2.http.Query("organs") organs: String?,
                           @retrofit2.http.Query("include-related-images") include: Boolean?,
                           @retrofit2.http.Query("no-reject") no_Reject: Boolean?,
                           @retrofit2.http.Query("lang") lang: String?,
                           @retrofit2.http.Query("api-key") key: String?): Call<JsonObject>



}

data class JsonObject (
    val query: Query,
    val language: String,
    val preferedReferential: String,
    val bestMatch: String,
    val results: List<Results>,
    val version: String,
    val remainingIdentificationRequests: Long
)

data class Query (
    val project: String,
    val images: List<String>,
    val organs: List<String>,
    val includeRelatedImages: Boolean
)

data class Results (
    val score: Double,
    val species: Species,
    val gbif: Gbif
)

data class Gbif (
    val id: String
)

data class Species (
    val scientificNameWithoutAuthor: String,
    val scientificNameAuthorship: String,
    val genus: Family,
    val family: Family,
    val commonNames: List<String>,
    val scientificName: String
)

data class Family (
    val scientificNameWithoutAuthor: String,
    val scientificNameAuthorship: String,
    val scientificName: String
)
