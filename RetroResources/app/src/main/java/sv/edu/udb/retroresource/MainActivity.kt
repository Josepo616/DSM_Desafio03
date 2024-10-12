package sv.edu.udb.retroresource

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.SearchView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import okhttp3.Credentials
import okhttp3.OkHttpClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import sv.edu.udb.retroresource.api.Resources
import sv.edu.udb.retroresource.api.ResourcesAdapter
import sv.edu.udb.retroresource.api.ResourcesApi

class MainActivity : AppCompatActivity() {

    private lateinit var connectivityReceiver: ConnectivityReceiver
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ResourcesAdapter
    private lateinit var searchView: SearchView
    private lateinit var fab: FloatingActionButton
    private lateinit var btnRecargar: FloatingActionButton  // Nuevo botón para recargar
    private lateinit var textViewError: TextView // Para mostrar el mensaje de error

    private val auth_username = "admin"
    private val auth_password = "admin123"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        recyclerView = findViewById(R.id.recyclerView)
        fab = findViewById(R.id.fab)
        btnRecargar = findViewById(R.id.btnRecargar)  // Inicializar el botón de recarga
        searchView = findViewById(R.id.searchView)
        textViewError = findViewById(R.id.textViewError) // Inicializar el TextView de error

        recyclerView.layoutManager = LinearLayoutManager(this)

        // Inicializar el BroadcastReceiver
        connectivityReceiver = ConnectivityReceiver { isConnected ->
            if (isConnected) {
                cargarRecursos() // Recargar recursos si hay conexión
            } else {
                mostrarErrorDeConexion()
            }
        }

        // Registrar el BroadcastReceiver
        val intentFilter = IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION)
        registerReceiver(connectivityReceiver, intentFilter)

        // Configurar el botón flotante (FAB) para añadir nuevos recursos
        fab.setOnClickListener {
            if (isNetworkAvailable()) {
                val intent = Intent(this, AddResourceActivity::class.java)
                startActivity(intent)
            } else {
                Toast.makeText(this, "Por favor conectese a internet", Toast.LENGTH_SHORT).show()
            }
        }

        btnRecargar.setOnClickListener {
            // Limpia el RecyclerView
            adapter.clearDatos() // Asegúrate de tener un método en tu adaptador para limpiar los datos
            recyclerView.visibility = View.GONE // Oculta el RecyclerView
            textViewError.visibility = View.GONE // Oculta el mensaje de error anterior

            // Verifica si hay conexión a internet
            if (isNetworkAvailable()) {
                // Si hay conexión, intenta cargar los datos
                cargarRecursos()
            } else {
                // Si no hay conexión, muestra el mensaje de error
                mostrarErrorDeConexion()
            }
        }

        // Cargar los recursos
        cargarRecursos()
    }

    override fun onDestroy() {
        super.onDestroy()
        // Desregistrar el BroadcastReceiver
        unregisterReceiver(connectivityReceiver)
    }

    private fun cargarRecursos() {
        // Verificar conexión a Internet
        if (!isNetworkAvailable()) {
            mostrarErrorDeConexion()
            return
        }

        // Cliente OkHttpClient con autenticación
        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .addHeader("Authorization", Credentials.basic(auth_username, auth_password))
                    .build()
                chain.proceed(request)
            }
            .build()

        // Configuración de Retrofit
        val retrofit = Retrofit.Builder()
            .baseUrl("https://670886058e86a8d9e42f2b50.mockapi.io/")
            .addConverterFactory(GsonConverterFactory.create())
            .client(client)
            .build()

        val api = retrofit.create(ResourcesApi::class.java)
        val call = api.obtenerRecursos()

        call.enqueue(object : Callback<List<Resources>> {
            override fun onResponse(
                call: Call<List<Resources>>,
                response: Response<List<Resources>>
            ) {
                if (response.isSuccessful) {
                    val recursos = response.body()
                    if (recursos != null) {
                        // Crear el adaptador y pasar la API
                        adapter = ResourcesAdapter(this@MainActivity, recursos.toMutableList(), api) { resource ->
                            mostrarDialogoConfirmacion(resource.titulo, resource.enlace)
                        }
                        recyclerView.adapter = adapter

                        // Conectar SearchView con el adaptador
                        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                            override fun onQueryTextSubmit(query: String?): Boolean {
                                return false
                            }

                            override fun onQueryTextChange(newText: String?): Boolean {
                                adapter.filtrar(newText ?: "")
                                return true
                            }
                        })

                        // Ocultar el mensaje de error si la carga es exitosa
                        recyclerView.visibility = View.VISIBLE
                        textViewError.visibility = View.GONE
                    } else {
                        mostrarError("No se encontraron recursos")
                    }
                } else {
                    mostrarError("Error en la respuesta del servidor")
                }
            }

            override fun onFailure(call: Call<List<Resources>>, t: Throwable) {
                mostrarError("Error en la carga de recursos: ${t.message}")
            }
        })
    }

    private fun mostrarDialogoConfirmacion(nombre: String, enlace: String) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Confirmación")
        builder.setMessage("¿Deseas abrir este enlace de $nombre?")
        builder.setPositiveButton("Sí") { dialog, _ ->
            abrirEnlace(enlace)
            dialog.dismiss()
        }
        builder.setNegativeButton("No") { dialog, _ ->
            dialog.dismiss()
        }
        builder.show()
    }

    private fun abrirEnlace(enlace: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(enlace))
        startActivity(intent)
    }

    // Mostrar el mensaje de error
    private fun mostrarErrorDeConexion() {
        recyclerView.visibility = View.GONE
        textViewError.text = "Sin conexión a Internet"
        textViewError.visibility = View.VISIBLE
    }

    private fun mostrarError(mensaje: String) {
        recyclerView.visibility = View.GONE
        textViewError.text = mensaje
        textViewError.visibility = View.VISIBLE
    }

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkCapabilities = connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
        return networkCapabilities != null &&
                networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    // BroadcastReceiver para detectar cambios de conectividad
    class ConnectivityReceiver(private val onNetworkChange: (Boolean) -> Unit) : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val networkCapabilities = connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
            val isConnected = networkCapabilities != null && networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            onNetworkChange(isConnected)
        }
    }
}
