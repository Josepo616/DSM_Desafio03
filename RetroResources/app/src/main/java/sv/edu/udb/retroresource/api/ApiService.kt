package sv.edu.udb.retroresource.api
import retrofit2.Call
import retrofit2.http.*

interface ResourcesApi {
    @GET("recursos")
    fun obtenerRecursos(): Call<List<Resources>>

    @POST("recursos")
    fun agregarRecurso(@Body recurso: Resources): Call<Resources>

    @PUT("recursos/{id}")
    fun actualizarRecurso(@Path("id") id: Int, @Body recurso: Resources): Call<Resources>

    @DELETE("recursos/{id}")
    fun eliminarRecurso(@Path("id") id: String): Call<Void>
}
