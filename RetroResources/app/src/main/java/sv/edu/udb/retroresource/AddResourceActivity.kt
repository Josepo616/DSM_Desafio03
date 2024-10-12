package sv.edu.udb.retroresource

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import okhttp3.Credentials
import okhttp3.OkHttpClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import sv.edu.udb.retroresource.api.Resources
import sv.edu.udb.retroresource.api.ResourcesApi

class AddResourceActivity : AppCompatActivity() {

    private lateinit var etNombre: EditText
    private lateinit var etEnlace: EditText
    private lateinit var etImagenEnlace: EditText // Nuevo campo para URL de la imagen
    private lateinit var etTipo: EditText // Nuevo campo para el tipo de recurso
    private lateinit var etDescripcion: EditText
    private lateinit var btnAddResource: Button

    val auth_username = "admin"
    val auth_password = "admin123"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_resource)

        // Inicializar vistas
        etNombre = findViewById(R.id.etNombre)
        etEnlace = findViewById(R.id.etEnlace)
        etImagenEnlace = findViewById(R.id.etImagenEnlace) // Inicializar el nuevo campo de imagen
        etTipo = findViewById(R.id.etTipo) // Inicializar el nuevo campo de tipo
        etDescripcion = findViewById(R.id.etDescripcion)
        btnAddResource = findViewById(R.id.btnAddResource)

        // Configurar Retrofit
        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .addHeader("Authorization", Credentials.basic(auth_username, auth_password))
                    .build()
                chain.proceed(request)
            }
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl("https://670886058e86a8d9e42f2b50.mockapi.io/")
            .addConverterFactory(GsonConverterFactory.create())
            .client(client)
            .build()

        val api = retrofit.create(ResourcesApi::class.java)

        // Configurar el botón para añadir recurso
        btnAddResource.setOnClickListener {
            val nombre = etNombre.text.toString().trim()
            val enlace = etEnlace.text.toString().trim()
            val imagenEnlace = etImagenEnlace.text.toString().trim() // Obtener la URL de la imagen
            val tipo = etTipo.text.toString().trim() // Obtener el tipo
            val descripcion = etDescripcion.text.toString().trim()

            fun isValidUrl(url: String): Boolean {
                val urlPattern = "^(https?://)([a-zA-Z0-9\\-]+\\.)+[a-zA-Z]{2,}(/\\S*)?$"
                return url.matches(Regex(urlPattern))
            }

            if (nombre.isNotEmpty() && enlace.isNotEmpty() && descripcion.isNotEmpty() && imagenEnlace.isNotEmpty() && tipo.isNotEmpty()) {
                if (isValidUrl(enlace) && isValidUrl(imagenEnlace)) { // Validar ambas URLs
                    // Cambiar texto y deshabilitar el botón
                    btnAddResource.text = "Añadiendo recurso..."
                    btnAddResource.isEnabled = false

                    // Obtener todos los recursos para encontrar el último ID
                    api.obtenerRecursos().enqueue(object : Callback<List<Resources>> {
                        override fun onResponse(
                            call: Call<List<Resources>>,
                            response: Response<List<Resources>>
                        ) {
                            if (response.isSuccessful) {
                                val recursos = response.body()
                                val nuevoRecurso: Resources

                                if (recursos != null && recursos.isNotEmpty()) {
                                    val ultimoId = recursos.maxOf { it.id ?: 0 }
                                    val nuevoId = ultimoId + 1
                                    nuevoRecurso = Resources(id = nuevoId, titulo = nombre, enlace = enlace, imagen = imagenEnlace, tipo = tipo, descripcion = descripcion)
                                } else {
                                    nuevoRecurso = Resources(id = 1, titulo = nombre, enlace = enlace, imagen = imagenEnlace, tipo = tipo, descripcion = descripcion)
                                }

                                // Llamar a la API para añadir el nuevo recurso
                                api.agregarRecurso(nuevoRecurso)
                                    .enqueue(object : Callback<Resources> {
                                        override fun onResponse(call: Call<Resources>, response: Response<Resources>) {
                                            if (response.isSuccessful) {
                                                Toast.makeText(this@AddResourceActivity, "Recurso añadido exitosamente", Toast.LENGTH_SHORT).show()
                                                finish() // Cerrar la actividad
                                            } else {
                                                Toast.makeText(this@AddResourceActivity, "Error al añadir recurso", Toast.LENGTH_SHORT).show()
                                                resetButton()
                                            }
                                        }

                                        override fun onFailure(call: Call<Resources>, t: Throwable) {
                                            Toast.makeText(this@AddResourceActivity, "Error en la conexión", Toast.LENGTH_SHORT).show()
                                            resetButton()
                                        }
                                    })
                            } else {
                                Toast.makeText(this@AddResourceActivity, "Error al obtener recursos", Toast.LENGTH_SHORT).show()
                                resetButton()
                            }
                        }

                        override fun onFailure(call: Call<List<Resources>>, t: Throwable) {
                            Toast.makeText(this@AddResourceActivity, "Error al obtener recursos", Toast.LENGTH_SHORT).show()
                            resetButton()
                        }
                    })
                } else {
                    Toast.makeText(this@AddResourceActivity, "Por favor, ingresa URLs válidas", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this@AddResourceActivity, "Por favor, completa todos los campos", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Función para resetear el estado del botón
    fun resetButton() {
        btnAddResource.text = "Añadir Recurso"
        btnAddResource.isEnabled = true
    }
}
